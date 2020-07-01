/*
 * Copyright (c) 2018 deltaDNA Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.deltadna.android.sdk;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.AnyThread;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import android.util.Log;

import com.deltadna.android.sdk.helpers.Settings;
import com.deltadna.android.sdk.listeners.RequestListener;
import com.deltadna.android.sdk.net.NetworkManager;
import com.deltadna.android.sdk.net.Response;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static com.deltadna.android.sdk.DatabaseHelper.ImageMessages.Column.ID;
import static com.deltadna.android.sdk.DatabaseHelper.ImageMessages.Column.LOCATION;
import static com.deltadna.android.sdk.DatabaseHelper.ImageMessages.Column.NAME;

class ImageMessageStore {
    
    private static final String TAG = BuildConfig.LOG_TAG + ' ' + "IMStore";
    private static final String SUBDIRECTORY = "image_messages";
    
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    private final Context context;
    private final DatabaseHelper database;
    private final NetworkManager network;
    private final Settings settings;
    
    ImageMessageStore(
            Context context,
            DatabaseHelper database,
            NetworkManager network,
            Settings settings) {
        
        this.context = context;
        this.database = database;
        this.network = network;
        this.settings = settings;
        
        for (final Location location : Location.values()) {
            if (location.available()) {
                final File dir = location.cache(context, SUBDIRECTORY);
                if (!dir.exists() && !dir.mkdirs()) {
                    Log.w(TAG, "Failed to create directory for " + location);
                }
            }
        }
    }
    
    @AnyThread
    final boolean contains(String url) {
        try (Cursor cursor = database.getImageMessage(url)) {
            return cursor.getCount() > 0;
        }
    }
    
    @AnyThread
    @Nullable
    final File getOnlyIfCached(String url) {
        File file = null;
        
        try (Cursor cursor = database.getImageMessage(url)) {
            if (cursor.moveToFirst()) {
                final Location location = Location.valueOf(cursor.getString(
                        cursor.getColumnIndex(LOCATION.toString())));
                if (!location.available()) {
                    Log.v(TAG, location + " not available for " + url);
                    return null;
                }
                
                file = new File(
                        location.cache(context, SUBDIRECTORY),
                        cursor.getString(cursor.getColumnIndex(NAME.toString())));
                if (!file.exists()) {
                    Log.v(TAG, String.format(
                            Locale.ENGLISH,
                            "%s for %s was evicted from storage",
                            file,
                            url));
                    
                    database.removeImageMessage(cursor.getLong(
                            cursor.getColumnIndex(ID.toString())));
                    file = null;
                } else {
                    Log.v(TAG, String.format(
                            Locale.ENGLISH,
                            "Found %s for %s",
                            file,
                            url));
                }
            } else {
                Log.v(TAG, String.format(
                        Locale.ENGLISH,
                        "Failed to find %s in storage",
                        url));
            }
        }
        
        return file;
    }
    
    @WorkerThread
    File get(String url) throws FetchingException {
        File file = getOnlyIfCached(url);
        
        if (file == null) {
            file = fetch(
                    url,
                    settings.isUseInternalStorageForImageMessages() || !Location.EXTERNAL.available()
                            ? Location.INTERNAL
                            : Location.EXTERNAL,
                    Uri.parse(url).getLastPathSegment());
        }
        
        return file;
    }
    
    @AnyThread
    void getAsync(final String url, final Callback<File> callback) {
        executor.execute(() -> {
            try {
                final File file = get(url);
                handler.post(() -> callback.onCompleted(file));
            } catch (FetchingException e) {
                handler.post(() -> callback.onFailed(e));
            }
        });
    }
    
    @AnyThread
    void prefetch(final Callback<Void> callback, String... urls) {
        if (urls == null || urls.length == 0) {
            handler.post(() -> callback.onCompleted(null));
            return;
        }
        
        Log.v(TAG, "Prefetching " + Arrays.toString(urls));
        
        final List<Callable<File>> tasks = new ArrayList<>(urls.length);
        for (final String url : urls) { tasks.add(() -> get(url)); }
        
        executor.execute(() -> {
            try {
                for (final Future<File> result : executor.invokeAll(tasks)) {
                    result.get();
                }
                
                handler.post(() -> callback.onCompleted(null));
            } catch (InterruptedException e) {
                Log.w(TAG, "Interrupted while prefetching", e);
                handler.post(() -> callback.onFailed(e));
            } catch (ExecutionException e) { // fail if any single one fails
                Log.w(TAG, "Failed to prefetch", e.getCause());
                handler.post(() -> callback.onFailed(e.getCause()));
            }
        });
    }
    
    @AnyThread
    ImageMessageStore cleanUp() {
        executor.execute(new CleanUp());
        return this;
    }
    
    @AnyThread
    ImageMessageStore clear() {
        executor.execute(new Clear());
        return this;
    }
    
    @WorkerThread
    private File fetch(
            final String url,
            final Location location,
            final String name) throws FetchingException {
        
        final CountDownLatch latch = new CountDownLatch(1);
        
        final File file = new File(location.cache(context, SUBDIRECTORY), name);
        final AtomicReference<FetchingException> error = new AtomicReference<>();
        final RequestListener<File> listener = new RequestListener<File>() {
            @Override
            public void onCompleted(Response<File> response) {
                if (response.isSuccessful()) {
                    Log.v(TAG, String.format(
                            "Successfully fetched %s to %s",
                            url,
                            file));
                    database.insertImageMessage(
                            url,
                            location,
                            name,
                            file.length(),
                            new Date());
                } else {
                    Log.w(TAG, String.format(
                            Locale.ENGLISH,
                            "Failed fetching %s due to %d: %s",
                            url,
                            response.code,
                            response.error));
                    error.set(new FetchingException(url, file, response));
                }
                
                latch.countDown();
            }
            
            @Override
            public void onError(Throwable t) {
                Log.w(  TAG,
                        String.format(
                                Locale.ENGLISH,
                                "Error while fetching %s to %s",
                                url,
                                file),
                        t);
                error.set(new FetchingException(url, file, t));
                latch.countDown();
            }
        };
        
        Log.v(TAG, String.format(Locale.ENGLISH, "Fetching %s to %s", url, file));
        network.fetch(url, file, listener);
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.w(  TAG,
                    String.format(
                            Locale.ENGLISH,
                            "Interrupted while fetching %s to %s",
                            url,
                            file),
                    e);
            error.set(new FetchingException(url, file, e));
        }
        
        if (error.get() != null) {
            throw error.get();
        } else if (file.exists()) {
            return file;
        } else {
            throw new FetchingException(url, file);
        }
    }
    
    interface Callback<V> {
        
        void onCompleted(V value);
        void onFailed(Throwable reason);
    }
    
    static final class FetchingException extends Exception {
        
        FetchingException(String url, File file, Response response) {
            super(String.format(
                    Locale.ENGLISH,
                    "Failed fetching %s to %s due to %d: %s",
                    url,
                    file,
                    response.code,
                    response.error));
        }
        
        FetchingException(String url, File file) {
            super(String.format(
                    Locale.ENGLISH,
                    "Failed fetching %s to %s",
                    url,
                    file));
        }
        
        FetchingException(String url, File file, Throwable cause) {
            super(  String.format(
                            Locale.ENGLISH,
                            "Failed fetching %s to %s",
                            url,
                            file),
                    cause);
        }
    }
    
    private final class CleanUp implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "Running cleanup task");
            
            int count = 0;
            try (Cursor cursor = database.getImageMessages()) {
                while (cursor.moveToNext()) {
                    final Location location = Location.valueOf(cursor.getString(
                            cursor.getColumnIndex(LOCATION.toString())));
                    final String name = cursor.getString(
                            cursor.getColumnIndex(NAME.toString()));
                    if (location.available()) {
                        final File file = new File(
                                location.cache(context, SUBDIRECTORY),
                                name);
                        if (!file.exists()) {
                            Log.v(TAG, "Removing database entry for missing " + file);
                            
                            database.removeImageMessage(cursor.getLong(
                                    cursor.getColumnIndex(ID.toString())));
                            count++;
                        }
                    }
                }
                
                Log.d(TAG, "Finished cleanup task with " + count + " removed");
            }
        }
    }
    
    private final class Clear implements Runnable {
        
        @Override
        public void run() {
            Log.d(TAG, "Running clearing task");
            
            for (final Location location : Location.values()) {
                if (location.available()) {
                    for (   final File file
                            : location.cache(context, SUBDIRECTORY).listFiles()) {
                        if (!file.delete()) Log.w(TAG, "Failed to clear " + file);
                    }
                }
            }
            
            database.removeImageMessageRows();
        }
    }
}
