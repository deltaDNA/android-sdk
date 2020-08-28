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

import android.os.AsyncTask;
import androidx.annotation.Nullable;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.deltadna.android.sdk.helpers.ClientInfo;
import com.deltadna.android.sdk.listeners.EngageListener;
import com.deltadna.android.sdk.listeners.RequestListener;
import com.deltadna.android.sdk.net.CancelableRequest;
import com.deltadna.android.sdk.net.NetworkManager;
import com.deltadna.android.sdk.net.Response;
import com.deltadna.android.sdk.util.CloseableIterator;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

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
            new ScheduledThreadPoolExecutor(1, r -> new Thread(
                    r,
                    EventHandler.class.getSimpleName()));

    private final Handler mainThreadTaskHandler = new Handler(Looper.getMainLooper());
    
    private final EventStore events;
    private final EngageStore engagements;
    private final NetworkManager network;
    
    @Nullable
    private ScheduledFuture<?> uploadTask;
    @Nullable
    private Future<?> upload;
    
    EventHandler(
            EventStore events,
            EngageStore engagements,
            NetworkManager network) {
        
        this.events = events;
        this.engagements = engagements;
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
        events.add(event.toString());
    }
    
    /**
     * Handles an engage {@code event}.
     */

    public class HandleEngagementTask<E extends Engagement> extends AsyncTask<Void , Void, Void> {

        final E engagement;
        final EngageListener<E> listener;
        String userId;
        String sessionId;
        final int engageApiVersion;
        String sdkVersion;
        String platform;

        public HandleEngagementTask(
                                    final E engagement,
                                    final EngageListener<E> listener,
                                    String userId,
                                    String sessionId,
                                    final int engageApiVersion,
                                    String sdkVersion,
                                    String platform) {
            this.engagement = engagement;
            this.listener = listener;
            this.userId = userId;
            this.sessionId = sessionId;
            this.engageApiVersion = engageApiVersion;
            this.sdkVersion = sdkVersion;
            this.platform = platform;
        }


        @Override
        protected Void doInBackground(Void... voids) {
            final JSONObject event;
            try {
                event = new JSONObject()
                        .put("userID", userId)
                        .put("decisionPoint", engagement.name)
                        .put("flavour", engagement.flavour)
                        .put("sessionID", sessionId)
                        .put("version", engageApiVersion)
                        .put("sdkVersion", sdkVersion)
                        .put("platform", platform)
                        .put("manufacturer", ClientInfo.manufacturer())
                        .put("operatingSystemVersion", ClientInfo.operatingSystemVersion())
                        .put("timezoneOffset", ClientInfo.timezoneOffset())
                        .put("locale", ClientInfo.locale());

                if (!engagement.params.isEmpty()) {
                    event.put("parameters", engagement.params.json);
                }
            } catch (JSONException e) {
                // should never happen due to params enforcement
                throw new IllegalArgumentException(e);
            }

            network.engage(event, new RequestListener<JSONObject>() {
                @Override
                public void onCompleted(Response<JSONObject> result) {
                    engagement.setResponse(result);
                    if (engagement.isSuccessful()) {
                        engagements.put(engagement);
                    } else if (engagement.isCacheCandidate() ){
                        Log.w(TAG, String.format(
                                Locale.US,
                                "Not caching %s due to failure, checking cache",
                                engagement));

                        final JSONObject cached = engagements.get(engagement);
                        if (cached != null) {
                            try {
                                engagement.setResponse(new Response<>(
                                        engagement.getStatusCode(),
                                        true,
                                        null,
                                        cached.put("isCachedResponse", true),
                                        engagement.getError()));

                                Log.d(  TAG,
                                        "Using cached response " + engagement.getJson());
                            } catch (JSONException ignored) {}
                        }
                    } else {
                        Log.w(TAG, String.format(
                                Locale.US,
                                "Not caching %s due to failure, and not checking cache due to client error response",
                                engagement));

                    }
                    listener.onCompleted(engagement);
                }

                @Override
                public void onError(Throwable t) {
                    // This needs to be run off the main thread, as it involves blocking database
                    // operations that can cause ANRs.
                    executor.execute(() -> {
                        final JSONObject cached = engagements.get(engagement);
                        if (cached != null) {
                            try {
                                engagement.setResponse(new Response<>(
                                        200,
                                        true,
                                        null,
                                        cached.put("isCachedResponse", true),
                                        null));

                                Log.d(TAG, "Using cached response " + engagement.getJson());

                                mainThreadTaskHandler.post(() -> listener.onCompleted(engagement));
                            } catch (JSONException e) {
                                mainThreadTaskHandler.post(() -> listener.onError(e));
                            }
                        } else {
                            mainThreadTaskHandler.post(() -> listener.onError(t));
                        }
                    });
                }
            }, "config".equalsIgnoreCase(engagement.name) && "internal".equalsIgnoreCase(engagement.flavour));
            return null;
        }
    }
    <E extends Engagement> void handleEngagement(
            final E engagement,
            final EngageListener<E> listener,
            String userId,
            String sessionId,
            final int engageApiVersion,
            String sdkVersion,
            String platform) {
            new HandleEngagementTask<E>(engagement, listener, userId, sessionId, engageApiVersion, sdkVersion, platform).execute();
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
            Log.v(TAG, "Starting event upload");
            final CloseableIterator<EventStoreItem> items = events.items();
            final AtomicReference<CloseableIterator.Mode> clearEvents =
                    new AtomicReference<>(CloseableIterator.Mode.ALL);

            try {
                if (!items.hasNext()) {
                    Log.d(TAG, "No stored events to upload");

                    clearEvents.set(CloseableIterator.Mode.NONE);
                }

                final StringBuilder builder = new StringBuilder("{\"eventList\":[");
                int count = 0;
                while (items.hasNext()) {
                    final EventStoreItem event = items.next();

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
                        clearEvents.set(CloseableIterator.Mode.UP_TO_CURRENT);
                        break;
                    }
                }
                if (builder.charAt(builder.length() - 1) == ',') {
                    builder.deleteCharAt(builder.length() - 1);
                }
                builder.append("]}");

                JSONObject payload = null;
                try {
                    payload = new JSONObject(builder.toString());
                } catch (JSONException e) {
                    Log.w(TAG, e);

                    clearEvents.set(CloseableIterator.Mode.NONE);
                }

                Log.d(TAG, "Uploading " + count + " events");
                final CountDownLatch latch = new CountDownLatch(1);
                final CancelableRequest request = network.collect(
                        payload,
                        new RequestListener<Void>() {
                            @Override
                            public void onCompleted(Response<Void> result) {
                                if (result.isSuccessful()) {
                                    Log.d(TAG, "Successfully uploaded events");
                                } else {
                                    Log.w(TAG, "Failed to upload events due to " + result);
                                    if (result.code == 400) {
                                        Log.w(TAG, "Wiping event store due to unrecoverable data");
                                        clearEvents.set(CloseableIterator.Mode.ALL);
                                    }
                                }

                                latch.countDown();
                            }

                            @Override
                            public void onError(Throwable t) {
                                Log.w(TAG,
                                        "Failed to upload events, will retry later",
                                        t);

                                clearEvents.set(CloseableIterator.Mode.NONE);
                                latch.countDown();
                            }
                        });

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Log.w(TAG, "Cancelling event upload", e);

                    clearEvents.set(CloseableIterator.Mode.NONE);
                    request.cancel();
                }
            } finally {
                Log.v(TAG, "Finished event upload");
                items.close(clearEvents.get());
            }
        }
    }

}
