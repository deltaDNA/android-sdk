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

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.deltadna.android.sdk.helpers.Settings;
import com.deltadna.android.sdk.listeners.EngageListener;
import com.deltadna.android.sdk.listeners.EventListener;
import com.deltadna.android.sdk.listeners.RequestListener;
import com.deltadna.android.sdk.listeners.internal.IEventListener;
import com.deltadna.android.sdk.net.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * {@inheritDoc}
 */
final class DDNANonTracking extends DDNA {
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    private final LocalBroadcastManager broadcasts;
    
    DDNANonTracking(
            Application application,
            String environmentKey,
            String collectUrl,
            String engageUrl,
            Settings settings,
            @Nullable String hashSecret,
            @Nullable String platform,
            Set<EventListener> eventListeners,
            Set<IEventListener> iEventListeners) {
        
        super(  application,
                environmentKey,
                collectUrl,
                engageUrl,
                settings,
                hashSecret,
                platform,
                eventListeners,
                iEventListeners);
        
        broadcasts = LocalBroadcastManager.getInstance(application);
    }
    
    @Override
    public synchronized DDNA startSdk() {
        return startSdk(null);
    }
    
    @Override
    public synchronized DDNA startSdk(@Nullable String userId) {
        if (preferences.isForgetMe() && !preferences.isForgotten()) {
            handler.removeCallbacksAndMessages(null);
            handler.post(new ForgetMe());
        }
        
        return this;
    }
    
    @Override
    public DDNA stopSdk() {
        return this;
    }
    
    @Override
    public boolean isStarted() {
        return true;
    }
    
    @Override
    public EventAction recordEvent(String name) {
        return EventAction.EMPTY;
    }
    
    @Override
    public EventAction recordEvent(Event event) {
        return EventAction.EMPTY;
    }
    
    @Override
    public EventAction recordNotificationOpened(boolean launch, Bundle payload) {
        return EventAction.EMPTY;
    }
    
    @Override
    public EventAction recordNotificationDismissed(Bundle payload) {
        return EventAction.EMPTY;
    }
    
    @Override
    public DDNA requestEngagement(String decisionPoint, EngageListener<Engagement> listener) {
        return requestEngagement(new Engagement(decisionPoint), listener);
    }
    
    @Override
    public <E extends Engagement> DDNA requestEngagement(E engagement, EngageListener<E> listener) {
        listener.onCompleted((E) engagement.setResponse(new Response<>(
                200, false, new byte[] {}, new JSONObject(), null)));
        return this;
    }
    
    @Override
    public DDNA requestSessionConfiguration() {
        performOn(iEventListeners, it -> it.onSessionConfigured(false, new JSONObject()));
        performOn(eventListeners, it -> it.onSessionConfigured(false));
        performOn(eventListeners, EventListener::onImageCachePopulated);
        return this;
    }
    
    @Override
    public DDNA upload() {
        return this;
    }
    
    @Override
    public DDNA downloadImageAssets() {
        performOn(eventListeners, EventListener::onImageCachePopulated);
        return this;
    }
    
    @Nullable
    @Override
    public String getRegistrationId() {
        return null;
    }
    
    @Override
    public DDNA setRegistrationId(@Nullable String registrationId) {
        return this;
    }
    
    @Override
    public DDNA clearRegistrationId() {
        return this;
    }
    
    @Override
    public DDNA clearPersistentData() {
        return this;
    }
    
    @Override
    public synchronized DDNA forgetMe() {
        if (!preferences.isForgotten()) {
            preferences.setForgetMe(true);
            
            broadcasts.sendBroadcast(new Intent(Actions.FORGET_ME));
            handler.post(new ForgetMe());
        }
        
        return this;
    }
    
    @Override
    ImageMessageStore getImageMessageStore() {
        // ok as we should never get far enough to use the store
        return null;
    }
    
    @Override
    Map<String, Integer> getIso4217() {
        return Collections.emptyMap();
    }
    
    private final class ForgetMe implements Runnable {
        
        @Override
        public void run() {
            JSONObject payload;
            try {
                payload = new JSONObject()
                        .put("eventName", "ddnaForgetMe")
                        .put("eventTimestamp", getCurrentTimestamp())
                        .put("eventUUID", UUID.randomUUID().toString())
                        .put("sessionID", getSessionId())
                        .put("userID", preferences.getUserId())
                        .put("eventParams", new JSONObject()
                                .put("platform", platform)
                                .put("sdkVersion", SDK_VERSION));
            } catch (JSONException e) {
                Log.w(BuildConfig.LOG_TAG, "Failed creating ddnaForgetMe event", e);
                return;
            }
            
            network.collect(payload, new RequestListener<Void>() {
                @Override
                public void onCompleted(Response<Void> response) {
                    if (response.isSuccessful()) preferences.setForgotten(true);
                    // else we'll try again on the next game start
                }
                
                @Override
                public void onError(Throwable t) {
                    // we'll try again on the next game start
                }
            });
        }
    }
}
