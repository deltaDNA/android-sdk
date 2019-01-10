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

import com.deltadna.android.sdk.exceptions.NotStartedException;
import com.deltadna.android.sdk.exceptions.SessionConfigurationException;
import com.deltadna.android.sdk.helpers.ClientInfo;
import com.deltadna.android.sdk.helpers.Objects;
import com.deltadna.android.sdk.helpers.Preconditions;
import com.deltadna.android.sdk.helpers.Settings;
import com.deltadna.android.sdk.listeners.EngageListener;
import com.deltadna.android.sdk.listeners.EventListener;
import com.deltadna.android.sdk.listeners.internal.IEventListener;
import com.deltadna.android.sdk.net.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

/**
 * {@inheritDoc}
 */
final class DDNAImpl extends DDNA {
    
    private static final int ENGAGE_API_VERSION = 4;
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + DDNAImpl.class.getSimpleName();
    
    @Nullable
    private final String clientVersion;
    
    private final EventStore eventStore;
    private final EngageStore engageStore;
    private final ActionStore actionStore;
    private final ImageMessageStore imageMessageStore;
    
    private final SessionRefreshHandler sessionHandler;
    private final EventHandler eventHandler;
    
    private final Map<String, Integer> iso4217;
    
    private boolean started;
    
    private Set<String> whitelistDps = Collections.emptySet();
    private Set<String> whitelistEvents = Collections.emptySet();
    private Set<String> cacheImages = Collections.emptySet();
    private Map<String, SortedSet<EventTrigger>> eventTriggers = Collections.emptyMap();
    
    @Override
    public DDNA startSdk() {
        return startSdk(null);
    }
    
    @Override
    public DDNA startSdk(@Nullable String userId) {
        Log.d(TAG, "Starting SDK");
        
        if (started) {
            Log.w(TAG, "SDK already started");
        } else {
            started = true;
            
            if (setUserId(userId)) {
                Log.d(TAG, "Clearing engage and action store on user change");
                engageStore.clear();
                actionStore.clear();
            }
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
            
            Log.d(TAG, "SDK started");
            performOn(iEventListeners, IEventListener::onStarted);
        }
        
        return this;
    }
    
    @Override
    public DDNA stopSdk() {
        Log.d(TAG, "Stopping SDK");
        
        if (!started) {
            Log.w(TAG, "SDK has not been started");
        } else {
            recordEvent("gameEnded");
            
            sessionHandler.unregister();
            eventHandler.stop(true);
            
            imageMessageStore.cleanUp();
            
            started = false;
            
            Log.d(TAG, "SDK stopped");
            performOn(iEventListeners, IEventListener::onStopped);
        }
        
        return this;
    }
    
    @Override
    public boolean isStarted() {
        return started;
    }
    
    @Override
    public EventAction recordEvent(String name) {
        return recordEvent(new Event(name));
    }
    
    @Override
    public EventAction recordEvent(Event event) {
        Preconditions.checkArg(event != null, "event cannot be null");
        if (!whitelistEvents.isEmpty() && !whitelistEvents.contains(event.name)) {
            Log.d(TAG, "Event " + event.name + " is not whitelisted, ignoring");
            return EventAction.EMPTY;
        }
        
        Log.v(TAG, "Recording event " + event.name);
        if (!started) {
            Log.w(TAG, "SDK has not been started");
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
        
        return new EventAction(
                event,
                eventTriggers.containsKey(event.name)
                        ? eventTriggers.get(event.name)
                        : Collections.unmodifiableSortedSet(new TreeSet<>()),
                actionStore,
                settings);
    }
    
    @Override
    public EventAction recordNotificationOpened(boolean launch, Bundle payload) {
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
    public EventAction recordNotificationDismissed(Bundle payload) {
        return recordNotificationOpened(false, payload);
    }
    
    @Override
    public DDNA requestEngagement(String decisionPoint, EngageListener<Engagement> listener) {
        return requestEngagement(new Engagement(decisionPoint), listener);
    }
    
    @Override
    public <E extends Engagement> DDNA requestEngagement(E engagement, EngageListener<E> listener) {
        Preconditions.checkArg(engagement != null, "engagement cannot be null");
        Preconditions.checkArg(listener != null, "listener cannot be null");
        
        if (!started) {
            Log.w(TAG, "SDK has not been started, aborting engagement " + engagement);
            listener.onError(new NotStartedException());
            return this;
        } else if (!whitelistDps.isEmpty()
                && !whitelistDps.contains(engagement.getDecisionPointAndFlavour())) {
            Log.d(TAG, String.format(
                    Locale.ENGLISH,
                    "Decision point %s is not whitelisted",
                    engagement.getDecisionPointAndFlavour()));
            listener.onCompleted((E) engagement.setResponse(new Response<>(
                    200, false, new byte[] {}, new JSONObject(), null)));
            return this;
        }
        
        Log.v(TAG, "Requesting engagement " + engagement);
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
    public DDNA requestSessionConfiguration() {
        final Date firstSession = preferences.getFirstSession();
        final Date lastSession = preferences.getLastSession();
        
        return requestEngagement(
                new Engagement("config", "internal")
                        .putParam(
                                "timeSinceFirstSession",
                                firstSession == null
                                        ? 0
                                        : new Date().getTime() - firstSession.getTime())
                        .putParam(
                                "timeSinceLastSession",
                                lastSession == null
                                        ? 0
                                        : new Date().getTime() - lastSession.getTime()),
                new SessionConfigCallback());
    }
    
    @Override
    public DDNA upload() {
        eventHandler.dispatch();
        return this;
    }
    
    @Override
    public DDNA downloadImageAssets() {
        imageMessageStore.prefetch(
                new ImageMessageStore.Callback<Void>() {
                    @Override
                    public void onCompleted(Void value) {
                        performOn(
                                eventListeners,
                                EventListener::onImageCachePopulated);
                    }
                    
                    @Override
                    public void onFailed(Throwable reason) {
                        performOn(
                                eventListeners,
                                l -> l.onImageCachingFailed(reason));
                    }
                },
                cacheImages.toArray(new String[0]));
        return this;
    }

    @Nullable
    @Override
    public String getCrossGameUserId() {
        return preferences.getCrossGameUserId();
    }

    @Override
    public DDNA setCrossGameUserId(String crossGameUserId) {
        if (TextUtils.isEmpty(crossGameUserId)) {
            Log.w(TAG, "crossGameUserId cannot be null or empty");
        } else if (!Objects.equals(preferences.getCrossGameUserId(), crossGameUserId)) {
            preferences.setCrossGameUserId(crossGameUserId);
            recordEvent(new Event("ddnaRegisterCrossGameUserID")
                    .putParam("ddnaCrossGameUserID", crossGameUserId));
        }

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
            recordEvent(new Event("notificationServices").putParam(
                    "androidRegistrationID",
                    (registrationId == null) ? "" : registrationId));
        }
        
        return this;
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
        engageStore.clear();
        actionStore.clear();
        imageMessageStore.clear();
        
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
            
            Log.d(TAG, "Recording 'newPlayer' event");
            
            recordEvent(new Event("newPlayer").putParam(
                    "userCountry", ClientInfo.countryCode()));
            
            preferences.setFirstRun(0);
        }
        
        if (settings.onInitSendGameStartedEvent()) {
            Log.d(TAG, "Recording 'gameStarted' event");
            
            final Event event = new Event("gameStarted")
                    .putParam("userLocale", ClientInfo.locale());
            if (!TextUtils.isEmpty(clientVersion)) {
                event.putParam("clientVersion", clientVersion);
            }

            if (!TextUtils.isEmpty(getCrossGameUserId())) {
                event.putParam("ddnaCrossGameUserID", getCrossGameUserId());
            }
            if (getRegistrationId() != null) {
                event.putParam("androidRegistrationID", getRegistrationId());
            }
            
            recordEvent(event);
        }
        
        if (settings.onInitSendClientDeviceEvent()) {
            Log.d(TAG, "Recording 'clientDevice' event");
            
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
        
        this.clientVersion = clientVersion;
        
        final Location location;
        if (settings.isUseInternalStorageForEngage()) {
            location = Location.INTERNAL;
        } else if (Location.EXTERNAL.available()) {
            location = Location.EXTERNAL;
        } else {
            Log.w(TAG, String.format(
                    Locale.US,
                    "%s not available, falling back to %s",
                    Location.EXTERNAL,
                    Location.INTERNAL));
            location = Location.INTERNAL;
        }
        
        final DatabaseHelper database = new DatabaseHelper(application);
        eventStore = new EventStore(
                application,
                database,
                settings,
                preferences);
        engageStore = new EngageStore(
                database,
                location.storage(application, "engage" + File.separator),
                settings);
        actionStore = new ActionStore(database);
        imageMessageStore = new ImageMessageStore(
                application,
                database,
                network,
                settings);
        
        sessionHandler = new SessionRefreshHandler(
                application,
                settings,
                () -> {
                    Log.d(TAG, "Session expired, updating id");
                    newSession(true);
                });
        eventHandler = new EventHandler(eventStore, engageStore, network);
        
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
            Log.w(TAG, "Failed to find ISO 4217 resource", e);
        } catch (XmlPullParserException | IOException e) {
            Log.w(TAG, "Failed parsing ISO 4217 resource", e);
        } finally {
            iso4217 = Collections.unmodifiableMap(temp);
        }
        
        setUserId(userId);
    }
    
    private final class SessionConfigCallback implements EngageListener<Engagement> {
        
        @Override
        public void onCompleted(Engagement engagement) {
            Log.v(TAG, "Received session configuration");
            
            if (engagement.isSuccessful()) {
                Log.v(TAG, "Retrieved session configuration");
                
                final JSONArray dpWhitelist = Objects.extractArray(
                        engagement.getJson(),
                        "parameters",
                        "dpWhitelist");
                if (dpWhitelist != null) {
                    final Set<String> toBeWhitelisted =
                            new HashSet<>(dpWhitelist.length());
                    for (int i = 0; i < dpWhitelist.length(); i++) {
                        try {
                            toBeWhitelisted.add(dpWhitelist.getString(i));
                        } catch (JSONException e) {
                            Log.w(  TAG,
                                    "Failed deserialising decision point whitelist",
                                    e);
                        }
                    }
                    
                    whitelistDps = Collections.unmodifiableSet(toBeWhitelisted);
                }
                
                final JSONArray eventsWhitelist = Objects.extractArray(
                        engagement.getJson(),
                        "parameters",
                        "eventsWhitelist");
                if (eventsWhitelist != null) {
                    final Set<String> toBeWhitelisted =
                            new HashSet<>(eventsWhitelist.length());
                    for (int i = 0; i < eventsWhitelist.length(); i++) {
                        try {
                            toBeWhitelisted.add(eventsWhitelist.getString(i));
                        } catch (JSONException e) {
                            Log.w(  TAG,
                                    "Failed deserialising event whitelist",
                                    e);
                        }
                    }
                    
                    whitelistEvents = Collections.unmodifiableSet(toBeWhitelisted);
                }
                
                final JSONArray triggers = Objects.extractArray(
                        engagement.getJson(),
                        "parameters",
                        "triggers");
                if (triggers != null) {
                    final List<EventTrigger> toBeSaved = new ArrayList<>(triggers.length());
                    for (int i = 0; i < triggers.length(); i++) {
                        try {
                            toBeSaved.add(new EventTrigger(
                                    DDNAImpl.this,
                                    i,
                                    triggers.getJSONObject(i)));
                        } catch (JSONException e) {
                            Log.w(TAG, "Failed deserialising event trigger", e);
                        }
                    }
                    
                    // put the triggers into buckets based on event names
                    eventTriggers = new HashMap<>();
                    for (final EventTrigger trigger : toBeSaved) {
                        if (eventTriggers.containsKey(trigger.getEventName())) {
                            eventTriggers.get(trigger.getEventName()).add(trigger);
                        } else {
                            final SortedSet<EventTrigger> set = new TreeSet<>();
                            set.add(trigger);
                            
                            eventTriggers.put(trigger.getEventName(), set);
                        }

                        // save persistent actions
                        final JSONObject parameters = Objects.extract(
                                trigger.getResponse(), "parameters");
                        if (    parameters != null
                                && parameters.has("ddnaIsPersistent")
                                && parameters.optBoolean("ddnaIsPersistent", false)) {
                            actionStore.put(trigger, parameters);
                        }
                    }
                    // make the collections read-only
                    for (final String key : eventTriggers.keySet()) {
                        eventTriggers.put(
                                key,
                                Collections.unmodifiableSortedSet(eventTriggers.get(key)));
                    }
                    DDNAImpl.this.eventTriggers =
                            Collections.unmodifiableMap(eventTriggers);
                }
                
                final JSONArray imageCache = Objects.extractArray(
                        engagement.getJson(),
                        "parameters",
                        "imageCache");
                if (imageCache != null) {
                    final Set<String> toBeCached =
                            new HashSet<>(imageCache.length());
                    for (int i = 0; i < imageCache.length(); i++) {
                        try {
                            toBeCached.add(imageCache.getString(i));
                        } catch (JSONException e) {
                            Log.w(TAG, "Failed deserialising session configuration", e);
                        }
                    }
                    
                    cacheImages = Collections.unmodifiableSet(toBeCached);
                    downloadImageAssets();
                }
                
                Log.v(TAG, "Session configured");
                performOn(iEventListeners, it -> it.onSessionConfigured(
                        engagement.isCached(),
                        engagement.getJson()));
                performOn(eventListeners, it -> it.onSessionConfigured(
                        engagement.isCached()));
            } else {
                Log.w(TAG, String.format(
                        Locale.ENGLISH,
                        "Failed to retrieve session configuration due to %d/%s",
                        engagement.getStatusCode(),
                        engagement.getError()));
                performOn(eventListeners, it -> it.onSessionConfigurationFailed(
                        new SessionConfigurationException(String.format(
                                Locale.ENGLISH,
                                "Engage returned %d/%s",
                                engagement.getStatusCode(),
                                engagement.getError()))));
            }
        }
        
        @Override
        public void onError(Throwable t) {
            Log.w(TAG, "Failed to retrieve session configuration", t);
            performOn(eventListeners, it -> it.onSessionConfigurationFailed(t));
        }
    }
}
