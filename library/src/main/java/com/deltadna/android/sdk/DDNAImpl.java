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

import android.app.Application;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.deltadna.android.sdk.helpers.ClientInfo;
import com.deltadna.android.sdk.helpers.EngageArchive;
import com.deltadna.android.sdk.helpers.Objects;
import com.deltadna.android.sdk.helpers.Preconditions;
import com.deltadna.android.sdk.helpers.Settings;
import com.deltadna.android.sdk.listeners.EngageListener;
import com.deltadna.android.sdk.listeners.EventListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * {@inheritDoc}
 */
final class DDNAImpl extends DDNA {
    
    private static final int ENGAGE_API_VERSION = 4;
    
    private static final String ENGAGE_DIRECTORY = "engage" + File.separator;
    @Deprecated
    private static final String ENGAGE_PATH_LEGACY = "%s/ddsdk/engage/";
    
    @Nullable
    private final String clientVersion;
    
    private final EventStore eventStore;
    private final ImageMessageStore imageMessageStore;
    private final EngageArchive archive;
    
    private final SessionRefreshHandler sessionHandler;
    private final EventHandler eventHandler;
    
    private final Map<String, Integer> iso4217;
    
    private final File engageStoragePath;
    
    private boolean started;
    
    @Override
    public DDNA startSdk() {
        return startSdk(null);
    }
    
    @Override
    public DDNA startSdk(@Nullable String userId) {
        Log.d(BuildConfig.LOG_TAG, "Starting SDK");
        
        if (started) {
            Log.w(BuildConfig.LOG_TAG, "SDK already started");
        } else {
            started = true;
            
            setUserId(userId);
            newSession(true);
            
            if (settings.getSessionTimeout() > 0) {
                sessionHandler.register();
            }
            
            if (settings.backgroundEventUpload()) {
                eventHandler.start(
                        settings.backgroundEventUploadStartDelaySeconds(),
                        settings.backgroundEventUploadRepeatRateSeconds());
            }
            
            triggerDefaultEvents();
            
            Log.d(BuildConfig.LOG_TAG, "SDK started");
            for (final EventListener listener : eventListeners) {
                listener.onStarted();
            }
        }
        
        return this;
    }
    
    @Override
    public DDNA stopSdk() {
        Log.d(BuildConfig.LOG_TAG, "Stopping SDK");
        
        if (!started) {
            Log.w(BuildConfig.LOG_TAG, "SDK has not been started");
        } else {
            recordEvent("gameEnded");
            
            sessionHandler.unregister();
            eventHandler.stop(true);
            
            imageMessageStore.cleanUp();
            archive.save();
            
            started = false;
            
            Log.d(BuildConfig.LOG_TAG, "SDK stopped");
            for (final EventListener listener : eventListeners) {
                listener.onStopped();
            }
        }
        
        return this;
    }
    
    @Override
    public boolean isStarted() {
        return started;
    }
    
    @Override
    public DDNA recordEvent(String name) {
        return recordEvent(new Event(name));
    }
    
    @Override
    public DDNA recordEvent(Event event) {
        Preconditions.checkArg(event != null, "event cannot be null");
        
        if (!started) {
            Log.w(BuildConfig.LOG_TAG, "SDK has not been started");
        }
        
        final JSONObject jsonEvent = new JSONObject();
        try {
            jsonEvent.put("eventName", event.name);
            jsonEvent.put("eventTimestamp", getCurrentTimestamp());
            jsonEvent.put("eventUUID", UUID.randomUUID().toString());
            jsonEvent.put("sessionID", sessionId);
            jsonEvent.put("userID", getUserId());
            
            JSONObject params =
                    new JSONObject(event.params.toJson().toString());
            params.put("platform", platform);
            params.put("sdkVersion", SDK_VERSION);
            
            jsonEvent.put("eventParams", params);
        } catch (JSONException e) {
            // should never happen due to params enforcement
            throw new IllegalArgumentException(e);
        }
        
        eventHandler.handleEvent(jsonEvent);
        
        return this;
    }
    
    @Override
    public DDNA recordNotificationOpened(boolean launch, Bundle payload) {
        final Event event = new Event("notificationOpened");
        
        if (payload.containsKey("_ddId"))
            event.putParam("notificationId", payload.getLong("_ddId"));
        if (payload.containsKey("_ddName"))
            event.putParam("notificationName", payload.getString("_ddName"));
        
        boolean insertCommunicationAttrs = false;
        if (payload.containsKey("_ddCampaign")) {
            event.putParam(
                    "campaignId",
                    Long.parseLong(payload.getString("_ddCampaign")));
            insertCommunicationAttrs = true;
        }
        if (payload.containsKey("_ddCohort")) {
            event.putParam(
                    "cohortId",
                    Long.parseLong(payload.getString("_ddCohort")));
            insertCommunicationAttrs = true;
        }
        if (insertCommunicationAttrs) {
            event.putParam("communicationSender", "GOOGLE_NOTIFICATION");
            event.putParam("communicationState", "OPEN");
        }
        
        event.putParam("notificationLaunch", launch);
        
        return recordEvent(event);
    }
    
    @Override
    public DDNA recordNotificationDismissed(Bundle payload) {
        return recordNotificationOpened(false, payload);
    }
    
    @Override
    public DDNA requestEngagement(String decisionPoint, EngageListener<Engagement> listener) {
        return requestEngagement(new Engagement(decisionPoint), listener);
    }
    
    @Override
    public <E extends Engagement> DDNA requestEngagement(E engagement, EngageListener<E> listener) {
        Preconditions.checkArg(engagement != null, "engagement cannot be null");
        
        if (!started) {
            Log.w(  BuildConfig.LOG_TAG,
                    "SDK has not been started, aborting engagement");
            return this;
        }
        
        eventHandler.handleEngagement(
                engagement,
                listener,
                getUserId(),
                sessionId,
                ENGAGE_API_VERSION,
                SDK_VERSION,
                platform);
        
        return this;
    }
    
    @Override
    public DDNA upload() {
        eventHandler.dispatch();
        return this;
    }
    
    @Override
    @Nullable
    public String getRegistrationId() {
        return preferences.getRegistrationId();
    }
    
    @Override
    public DDNA setRegistrationId(@Nullable String registrationId) {
        if (!Objects.equals(registrationId, preferences.getRegistrationId())) {
            preferences.setRegistrationId(registrationId);
            return recordEvent(new Event("notificationServices").putParam(
                    "androidRegistrationID",
                    (registrationId == null) ? "" : registrationId));
        } else {
            return this;
        }
    }
    
    @Override
    public DDNA clearRegistrationId() {
        if (!TextUtils.isEmpty(getRegistrationId())) {
            setRegistrationId(null);
        }
        
        return this;
    }
    
    @Override
    public DDNA clearPersistentData() {
        stopSdk();
        
        preferences.clear();
        eventStore.clear();
        imageMessageStore.clear();
        archive.clear();
        
        return this;
    }
    
    @Override
    public DDNA forgetMe() {
        return stopSdk();
    }
    
    @Override
    ImageMessageStore getImageMessageStore() {
        return imageMessageStore;
    }
    
    @Override
    File getEngageStoragePath() {
        return engageStoragePath;
    }
    
    @Override
    Map<String, Integer> getIso4217() {
        return iso4217;
    }
    
    /**
     * Fires the default events, should only be called from
     * {@link #startSdk(String)}.
     */
    private void triggerDefaultEvents() {
        if (    settings.onFirstRunSendNewPlayerEvent()
                && preferences.getFirstRun() > 0) {
            
            Log.d(BuildConfig.LOG_TAG, "Recording 'newPlayer' event");
            
            recordEvent(new Event("newPlayer").putParam(
                    "userCountry", ClientInfo.countryCode()));
            
            preferences.setFirstRun(0);
        }
        
        if (settings.onInitSendGameStartedEvent()) {
            Log.d(BuildConfig.LOG_TAG, "Recording 'gameStarted' event");
            
            final Event event = new Event("gameStarted")
                    .putParam("userLocale", ClientInfo.locale());
            if (!TextUtils.isEmpty(clientVersion)) {
                event.putParam("clientVersion", clientVersion);
            }
            if (getRegistrationId() != null) {
                event.putParam("androidRegistrationID", getRegistrationId());
            }
            
            recordEvent(event);
        }
        
        if (settings.onInitSendClientDeviceEvent()) {
            Log.d(BuildConfig.LOG_TAG, "Recording 'clientDevice' event");
            
            recordEvent(new Event("clientDevice")
                    .putParam("deviceName", ClientInfo.deviceName())
                    .putParam("deviceType", ClientInfo.deviceType())
                    .putParam("hardwareVersion", ClientInfo.deviceModel())
                    .putParam("operatingSystem", ClientInfo.operatingSystem())
                    .putParam("operatingSystemVersion", ClientInfo.operatingSystemVersion())
                    .putParam("manufacturer", ClientInfo.manufacturer())
                    .putParam("timezoneOffset", ClientInfo.timezoneOffset())
                    .putParam("userLanguage", ClientInfo.languageCode()));
        }
    }
    
    DDNAImpl(
            Application application,
             String environmentKey,
             String collectUrl,
             String engageUrl,
             Settings settings,
             @Nullable String hashSecret,
             @Nullable String clientVersion,
             @Nullable String userId,
             @Nullable String platform) {
        
        super(  application,
                environmentKey,
                collectUrl,
                engageUrl,
                settings,
                hashSecret,
                platform);
        
        this.clientVersion = clientVersion;
        
        final Location location;
        if (settings.isUseInternalStorageForEngage()) {
            location = Location.INTERNAL;
        } else if (Location.EXTERNAL.available()) {
            location = Location.EXTERNAL;
        } else {
            Log.w(BuildConfig.LOG_TAG, String.format(
                    Locale.US,
                    "%s not available, falling back to %s",
                    Location.EXTERNAL,
                    Location.INTERNAL));
            location = Location.INTERNAL;
        }
        engageStoragePath = location.storage(application, ENGAGE_DIRECTORY);
        final File legacyPath;
        if (application.getExternalFilesDir(null) != null) {
            final String path = application.getExternalFilesDir(null).getPath();
            legacyPath = new File(String.format(
                    Locale.US,
                    ENGAGE_PATH_LEGACY,
                    (path != null) ? path : ""));
        } else {
            Log.d(BuildConfig.LOG_TAG, "Legacy engage storage path not found");
            legacyPath = null;
        }
        
        final DatabaseHelper database = new DatabaseHelper(application);
        eventStore = new EventStore(
                application,
                database,
                settings,
                preferences);
        imageMessageStore = new ImageMessageStore(
                application,
                database,
                network,
                settings);
        archive = new EngageArchive(engageStoragePath, legacyPath);
        
        sessionHandler = new SessionRefreshHandler(
                application,
                settings,
                new SessionRefreshHandler.Listener() {
                    @Override
                    public void onExpired() {
                        Log.d(  BuildConfig.LOG_TAG,
                                "Session expired, updating id");
                        newSession(true);
                    }
                });
        eventHandler = new EventHandler(eventStore, archive, network);
        
        final Map<String, Integer> temp = new HashMap<>();
        try {
            final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            final XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new InputStreamReader(
                    application.getResources().openRawResource(R.raw.iso_4217)));
            
            boolean expectingCode = false;
            boolean expectingValue = false;
            String pulledCode = null;
            String pulledValue = null;
            
            int eventType;
            while ((eventType = xpp.next()) != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        switch (xpp.getName()) {
                            case "Ccy":
                                expectingCode = true;
                                break;
                            
                            case "CcyMnrUnts":
                                expectingValue = true;
                                break;
                        }
                        break;
                    
                    case XmlPullParser.TEXT:
                        if (expectingCode) {
                            pulledCode = xpp.getText();
                        } else if (expectingValue) {
                            pulledValue = xpp.getText();
                        }
                        break;
                    
                    case XmlPullParser.END_TAG:
                        switch (xpp.getName()) {
                            case "Ccy":
                                expectingCode = false;
                                break;
                            
                            case "CcyMnrUnts":
                                expectingValue = false;
                                break;
                            
                            case "CcyNtry":
                                if (    !TextUtils.isEmpty(pulledCode)
                                        && !TextUtils.isEmpty(pulledValue)) {
                                    int value;
                                    try {
                                        value = Integer.parseInt(pulledValue);
                                    } catch (NumberFormatException ignored) {
                                        value = 0;
                                    }
                                    
                                    temp.put(pulledCode, value);
                                }
                                
                                expectingCode = false;
                                expectingValue = false;
                                pulledCode = null;
                                pulledValue = null;
                                break;
                        }
                }
            }
        } catch (Resources.NotFoundException e) {
            Log.w(BuildConfig.LOG_TAG, "Failed to find ISO 4217 resource", e);
        } catch (XmlPullParserException | IOException e) {
            Log.w(BuildConfig.LOG_TAG, "Failed parsing ISO 4217 resource", e);
        } finally {
            iso4217 = Collections.unmodifiableMap(temp);
        }
        
        setUserId(userId);
    }
}
