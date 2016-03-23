/*
 * Copyright (c) 2016 deltaDNA Ltd. All rights reserved.
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.deltadna.android.sdk.exceptions.BadRequestException;
import com.deltadna.android.sdk.helpers.EngageArchive;
import com.deltadna.android.sdk.listeners.RequestListener;
import com.deltadna.android.sdk.net.CancelableRequest;
import com.deltadna.android.sdk.net.NetworkManager;
import com.deltadna.android.sdk.net.Response;
import com.deltadna.android.sdk.util.CloseableIterator;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Class which handles collect and engage events, ensuring that collect
 * events are saved to the store, and uploaded as appropriate, and that
 * engage requests go through the archive for caching purposes.
 */
final class EventHandler {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + EventHandler.class.getSimpleName();
    
    private final ScheduledExecutorService executor =
            new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
                @Override
                public Thread newThread(@NonNull Runnable r) {
                    return new Thread(
                            r,
                            EventHandler.class.getSimpleName());
                }
            });
    
    private final EventStore store;
    private final EngageArchive archive;
    private final NetworkManager network;
    
    @Nullable
    private ScheduledFuture<?> uploadTask;
    @Nullable
    private Future<?> upload;
    
    EventHandler(
            EventStore store,
            EngageArchive archive,
            NetworkManager network) {
        
        this.store = store;
        this.archive = archive;
        this.network = network;
    }

    /**
     * Starts automatic background event uploads.
     * 
     * @param startDelay    start delay in seconds
     * @param repeatRate    repeat rate in seconds
     */
    synchronized void start(int startDelay, int repeatRate) {
        cancelUploadTask();
        
        Log.d(TAG, "Starting scheduled event uploads");
        uploadTask = executor.scheduleWithFixedDelay(
                new Upload(),
                startDelay,
                repeatRate,
                TimeUnit.SECONDS);
    }

    /**
     * Stops automatic background event uploads.
     * 
     * @param dispatch  if {@code true} events will be dispatched
     *                  after stopping
     */
    synchronized void stop(boolean dispatch) {
        Log.d(TAG, "Stopping scheduled event uploads");
        
        cancelUploadTask();
        
        if (dispatch) {
            dispatch();
        }
    }

    /**
     * Dispatches any events immediately.
     */
    synchronized void dispatch() {
        if (uploadTask != null) {
            Log.w(TAG, "Event uploads are currently scheduled");
        }
        
        if (upload == null || upload.isDone()) {
            Log.d(TAG, "Submitting immediate events upload");
            upload = executor.submit(new Upload());
        }
    }
    
    /**
     * Handles a collect {@code event} by placing into the queue,
     * to be sent at a later time.
     */
    void handleEvent(JSONObject event) {
        store.add(event.toString());
    }
    
    /**
     * Handles an engage {@code event}.
     */
    void handleEngagement(
            final String decisionPoint,
            @Nullable final String flavour,
            final JSONObject event,
            final RequestListener<JSONObject> listener) {
        
        network.engage(event, new RequestListener<Response<JSONObject>>() {
            @Override
            public void onSuccess(Response<JSONObject> result) {
                archive.put(decisionPoint, flavour, result.body.toString());
                listener.onSuccess(result.body);
            }
            
            @Override
            public void onFailure(Throwable t) {
                if (archive.contains(decisionPoint, flavour)) {
                    try {
                        final JSONObject json = new JSONObject(
                                archive.get(decisionPoint, flavour))
                                .put("isCachedResponse", true);
                        
                        Log.d(TAG, "Using cached engage " + json);
                        listener.onSuccess(json);
                    } catch (JSONException e1) {
                        /*
                         * This can only happen if the archive has become
                         * corrupted as we don't store a live response in
                         * the cache if we failed to convert it in the first
                         * place.
                         */
                        // TODO should we clear the archive?
                        Log.e(  TAG,
                                "Failed converting cached engage to JSON",
                                e1);
                        listener.onFailure(e1);
                    }
                } else {
                    listener.onFailure(t);
                }
            }
        });
    }
    
    private void cancelUploadTask() {
        if (uploadTask != null) {
            if (uploadTask.cancel(false)) {
                Log.d(TAG, "Cancelled scheduled upload task");
            } else {
                Log.w(TAG, "Failed to cancel scheduled upload task");
            }
            
            uploadTask = null;
        }
    }
    
    private final class Upload implements Runnable {
        
        @Override
        public void run() {
            final CloseableIterator<EventStoreItem> events = store.items();
            if (!events.hasNext()) {
                Log.d(TAG, "No stored events to upload");
                return;
            }
            
            final StringBuilder builder = new StringBuilder("{\"eventList\":[");
            int count = 0;
            while (events.hasNext()) {
                final EventStoreItem event = events.next();
                
                if (event.available()) {
                    final String content = event.get();
                    if (content != null) {
                        builder.append(content);
                        builder.append(',');
                        
                        count++;
                    } else {
                        Log.w(TAG, "Failed retrieving event, skipping");
                    }
                } else {
                    Log.w(TAG, "Stored event not available, pausing");
                    break;
                }
            }
            if (builder.charAt(builder.length()- 1) == ',') {
                builder.deleteCharAt(builder.length() - 1);
            }
            builder.append("]}");
            
            final JSONObject payload;
            try {
                payload = new JSONObject(builder.toString());
            } catch (JSONException e) {
                Log.w(TAG, e);
                return;
            }
            
            Log.d(TAG, "Uploading " + count + " events");
            final CountDownLatch latch = new CountDownLatch(1);
            final CancelableRequest request = network.collect(
                    payload,
                    new RequestListener<Response<Void>>() {
                        @Override
                        public void onSuccess(Response<Void> result) {
                            Log.d(TAG, "Successfully uploaded events");
                            
                            events.close(true);
                            latch.countDown();
                        }
                        
                        @Override
                        public void onFailure(Throwable t) {
                            Log.e(TAG, "Failed to upload events", t);
                            
                            if (t instanceof BadRequestException) {
                                Log.w(TAG, "Wiping event store due to unrecoverable data");
                                events.close(true);
                            } else {
                                Log.d(TAG, "Will retry uploading events later");
                                events.close(false);
                            }
                            
                            latch.countDown();
                        }
                    });
            
            try {
                latch.await();
            } catch (InterruptedException e) {
                Log.w(TAG, "Cancelling event upload", e);
                request.cancel();
            }
        }
    }
}
