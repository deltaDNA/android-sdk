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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.deltadna.android.sdk.listeners.EngageListener;
import com.deltadna.android.sdk.listeners.EventListener;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * {@inheritDoc}
 */
class DDNADelegate extends DDNA {
    
    private final DDNA tracking;
    private final DDNA nonTracking;
    
    DDNADelegate(
            Configuration configuration,
            Set<EventListener> eventListeners,
            DDNA tracking,
            DDNA nonTracking) {
        
        super(  configuration.application,
                configuration.environmentKey,
                configuration.collectUrl,
                configuration.engageUrl,
                configuration.settings,
                configuration.hashSecret,
                configuration.platform,
                eventListeners);
        
        this.tracking = tracking;
        this.nonTracking = nonTracking;
    }
    
    @Override
    public DDNA startSdk() {
        return getDelegate().startSdk();
    }
    
    @Override
    public DDNA startSdk(@Nullable String userId) {
        if (    (!TextUtils.isEmpty(userId) && !userId.equals(getUserId()))
                || TextUtils.isEmpty(getUserId())) {
            preferences.clearForgetMeAndForgotten();
        }
        
        return getDelegate().startSdk(userId);
    }
    
    @Override
    public DDNA stopSdk() {
        return getDelegate().stopSdk();
    }
    
    @Override
    public boolean isStarted() {
        return getDelegate().isStarted();
    }
    
    @Override
    public EventAction recordEvent(String name) {
        return getDelegate().recordEvent(name);
    }
    
    @Override
    public EventAction recordEvent(Event event) {
        return getDelegate().recordEvent(event);
    }
    
    @Override
    public EventAction recordNotificationOpened(boolean launch, Bundle payload) {
        return getDelegate().recordNotificationOpened(launch, payload);
    }
    
    @Override
    public EventAction recordNotificationDismissed(Bundle payload) {
        return getDelegate().recordNotificationDismissed(payload);
    }
    
    @Override
    public DDNA requestEngagement(String decisionPoint, EngageListener<Engagement> listener) {
        return getDelegate().requestEngagement(decisionPoint, listener);
    }
    
    @Override
    public <E extends Engagement> DDNA requestEngagement(E engagement, EngageListener<E> listener) {
        return getDelegate().requestEngagement(engagement, listener);
    }
    
    @Override
    public DDNA requestSessionConfiguration() {
        return getDelegate().requestSessionConfiguration();
    }
    
    @Override
    public DDNA upload() {
        return getDelegate().upload();
    }
    
    @Override
    public DDNA downloadImageAssets() {
        return getDelegate().downloadImageAssets();
    }
    
    @Nullable
    @Override
    public String getRegistrationId() {
        return getDelegate().getRegistrationId();
    }
    
    @Override
    public DDNA setRegistrationId(@Nullable String registrationId) {
        return getDelegate().setRegistrationId(registrationId);
    }
    
    @Override
    public DDNA clearRegistrationId() {
        return getDelegate().clearRegistrationId();
    }
    
    @Override
    public DDNA clearPersistentData() {
        if (preferences.isForgetMe() || preferences.isForgotten()) {
            nonTracking.clearPersistentData();
            tracking.clearPersistentData();
        } else {
            tracking.clearPersistentData();
        }
        
        return tracking;
    }
    
    @Override
    public DDNA forgetMe() {
        if (!preferences.isForgetMe()) {
            tracking.forgetMe();
            nonTracking.forgetMe();
        }
        
        return nonTracking;
    }
    
    @Override
    ImageMessageStore getImageMessageStore() {
        return getDelegate().getImageMessageStore();
    }
    
    @Override
    File getEngageStoragePath() {
        return getDelegate().getEngageStoragePath();
    }
    
    @Override
    Map<String, Integer> getIso4217() {
        return getDelegate().getIso4217();
    }
    
    private DDNA getDelegate() {
        if (preferences.isForgetMe() || preferences.isForgotten()) {
            return nonTracking;
        } else {
            return tracking;
        }
    }
}
