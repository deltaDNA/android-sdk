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
import android.support.annotation.AnyThread;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.deltadna.android.sdk.helpers.Settings;
import com.deltadna.android.sdk.listeners.RequestListener;
import com.deltadna.android.sdk.net.NetworkManager;
import com.deltadna.android.sdk.net.Response;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.deltadna.android.sdk.DatabaseHelper.ImageMessages.Column.ID;
import static com.deltadna.android.sdk.DatabaseHelper.ImageMessages.Column.LOCATION;
import static com.deltadna.android.sdk.DatabaseHelper.ImageMessages.Column.NAME;

class ImageMessageStore {
    
    private static final String TAG = BuildConfig.LOG_TAG + ' ' + "IMStore";
    private static final String SUBDIRECTORY = "image_messages";
    
    private final Executor executor = Executors.newFixedThreadPool(4);
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
        final Cursor cursor = database.getImageMessage(url);
        try {
            return cursor.getCount() > 0; 
        } finally {
            cursor.close();
        }
    }
    
    @AnyThread
    @Nullable
    final File getOnlyIfCached(String url) {
        File file = null;
        
        final Cursor cursor = database.getImageMessage(url);
        try {
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
                    return null;
                } else {
                    Log.v(TAG, String.format(
                            Locale.ENGLISH,
                            "Found %s for %s",
                            file,
                            url));
                }
            }
        } finally {
            cursor.close();
        }
        
        return file;
    }
    
    @WorkerThread
    @Nullable
    File get(String url) {
        File file = getOnlyIfCached(url);
        
        if (file == null) {
            Log.v(TAG, String.format(
                    Locale.ENGLISH,
                    "Failed to find %s in storage, fetching",
                    url));
            
            file = fetch(
                    url,
                    settings.isUseInternalStorageForImageMessages() || !Location.EXTERNAL.available()
                            ? Location.INTERNAL
                            : Location.EXTERNAL,
                    Uri.parse(url).getLastPathSegment());
            if (file == null || !file.exists()) {
                return null;
            }
        } else {
            Log.v(TAG, String.format(
                    Locale.ENGLISH,
                    "Found %s for %s",
                    file,
                    url));
        }
        
        return file;
    }
    
    @AnyThread
    void getAsync(final String url, final Callback<File> callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final File file = get(url);
                
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onCompleted(file);
                    }
                });
            }
        });
    }
    
    @AnyThread
    void prefetch(final Callback<Void> callback, String... urls) {
        Log.v(TAG, "Prefetching " + Arrays.toString(urls));
        
        final CountDownLatch latch = new CountDownLatch(urls.length);
        
        for (final String url : urls) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    get(url);
                    latch.countDown();
                }
            });
        }
        
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    latch.await();
                    Log.v(TAG, "Prefetching completed successfully");
                } catch (InterruptedException e) {
                    Log.w(TAG, "Interrupted while waiting for prefetching", e);
                } finally {
                    callback.onCompleted(null);
                }
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
    @Nullable
    private File fetch(
            final String url,
            final Location location,
            final String name) {
        
        final CountDownLatch latch = new CountDownLatch(1);
        
        final File file = new File(location.cache(context, SUBDIRECTORY), name);
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
                    Log.w(TAG, "Failed fetching " + url);
                }
                
                latch.countDown();
            }
            
            @Override
            public void onError(Throwable t) {
                Log.w(TAG, "Failed fetching " + url, t);
                latch.countDown();
            }
        };
        
        Log.v(TAG, String.format(Locale.ENGLISH, "Fetching %s to %s", url, file));
        network.fetch(url, file, listener);
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.w(TAG, "Interrupted while fetching " + url, e);
        }
        
        return file.exists() ? file : null;
    }
    
    interface Callback<V> {
        
        void onCompleted(@Nullable V value);
    }
    
    private final class CleanUp implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "Running cleanup task");
            
            int count = 0;
            final Cursor cursor = database.getImageMessages();
            try {
                while (!cursor.isAfterLast()) {
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
                    
                    cursor.moveToNext();
                }
                
                Log.d(TAG, "Finished cleanup task with " + count + " removed");
            } finally {
                cursor.close();
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
