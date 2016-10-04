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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.deltadna.android.sdk.exceptions.NotInitialisedException;
import com.deltadna.android.sdk.helpers.ClientInfo;
import com.deltadna.android.sdk.helpers.EngageArchive;
import com.deltadna.android.sdk.helpers.Preconditions;
import com.deltadna.android.sdk.helpers.Settings;
import com.deltadna.android.sdk.listeners.EngageListener;
import com.deltadna.android.sdk.listeners.ImageMessageListener;
import com.deltadna.android.sdk.listeners.SessionListener;
import com.deltadna.android.sdk.net.NetworkManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.WeakHashMap;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Singleton class for accessing the deltaDNA SDK.
 * <p>
 * The singleton instance must first be initialised by calling
 * {@link DDNA#initialise(Configuration)} from a {@link Application}'s
 * {@link Application#onCreate()} method. Following this call an instance can
 * be accessed at any time through {@link DDNA#instance()}.
 * <p>
 * Prior to sending events, or performing engagements, the SDK must be started
 * through {@link #startSdk()} or {@link #startSdk(String)}. {@link #stopSdk()}
 * should be called when the game is stopped.
 * <p>
 * To customise behaviour after initialisation you can call
 * {@link #getSettings()} to get access to the {@link Settings}.
 */
public final class DDNA {
    
    private static final String SDK_VERSION =
            "Android SDK v" + BuildConfig.VERSION_NAME;
    private static final int ENGAGE_API_VERSION = 4;
    
    private static final String ENGAGE_STORAGE_PATH = "%s/ddsdk/engage/";
    
    private static final SimpleDateFormat TIMESTAMP_FORMAT;
    static {
        final SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        TIMESTAMP_FORMAT = format;
    }
    
    private static DDNA instance;
    
    private final Settings settings;
    @Nullable
    private final String clientVersion;
    
    private final Preferences preferences;
    private final EventStore store;
    private final EngageArchive archive;
    private final NetworkManager network;
    
    private final SessionRefreshHandler sessionHandler;
    private final EventHandler eventHandler;
    
    private Map<String, Integer> iso4217;
    
    private final String engageStoragePath;
    
    private boolean started;
	private String sessionId = UUID.randomUUID().toString();
    
    private final Set<SessionListener> sessionListeners = Collections.newSetFromMap(
            new WeakHashMap<SessionListener, Boolean>(1));
    private final InputStream inputSourceIso4217;

    public static synchronized DDNA initialise(Configuration configuration) {
        Preconditions.checkArg(
                configuration != null,
                "configuration cannot be null");
        
        if (instance == null) {
            instance = new DDNA(
                    configuration.application,
                    configuration.environmentKey,
                    configuration.collectUrl,
                    configuration.engageUrl,
                    configuration.settings,
                    configuration.hashSecret,
                    configuration.clientVersion,
                    configuration.userId);
        } else {
            Log.w(BuildConfig.LOG_TAG, "SDK has already been initialised");
        }
        
        return instance;
    }
    
    public static synchronized DDNA instance() {
        if (instance == null) {
            throw new NotInitialisedException();
        }
        
        return instance;
    }
    
    /**
     * Starts the SDK.
     * <p>
     * This method needs to be called before sending events or making
     * engagements.
     *
     * @return this {@link DDNA} instance
     */
    public DDNA startSdk() {
        return startSdk(null);
    }
    
    /**
     * Starts the SDK.
     * <p>
     * This method needs to be called before sending events or making
     * engagements.
     *
     * @param userId the user id, may be {@code null} in which case the
     *               SDK will generate an id.
     *
     * @return this {@link DDNA} instance
     */
    public DDNA startSdk(@Nullable String userId) {
        Log.d(BuildConfig.LOG_TAG, "Starting SDK");
        
        if (started) {
            Log.w(BuildConfig.LOG_TAG, "SDK already started");
        } else {
            setUserId(userId);
            newSession(true);
            
            started = true;
            
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
        }
        
        return this;
    }
    
    /**
     * Stops the SDK.
     * <p>
     * Calling this method sends a 'gameEnded' event to Collect, disables
     * background uploads and automatic session refreshing.
     *
     * @return this {@link DDNA} instance
     */
    public DDNA stopSdk() {
        Log.d(BuildConfig.LOG_TAG, "Stopping SDK");
        
        if (!started) {
            Log.w(BuildConfig.LOG_TAG, "SDK has not been started");
        } else {
            recordEvent("gameEnded");
            
            sessionHandler.unregister();
            eventHandler.stop(true);
            if (archive != null) {
                archive.save();
            }
            
            Log.d(BuildConfig.LOG_TAG, "SDK stopped");
            started = false;
        }
        
        return this;
    }
    
    /**
     * Queries the status of the SDK.
     *
     * @return {@code true} if the SDK has been started, else {@code false}
     */
    public boolean isStarted() {
        return started;
    }
    
    /**
     * Changes the session id.
     *
     * @return this {@link DDNA} instance
     */
    public DDNA newSession() {
        return newSession(false);
    }
    
    DDNA newSession(boolean suppressWarning) {
        if (!suppressWarning && settings.getSessionTimeout() > 0) {
            Log.w(  BuildConfig.LOG_TAG,
                    "Automatic session refreshing is enabled");
        }
        
        sessionId = UUID.randomUUID().toString();
        
        for (final SessionListener listener : sessionListeners) {
            listener.onSessionUpdated();
        }
        
        return this;
    }
    
    /**
     * Records an event with Collect.
     *
     * @param name the name of the event
     *
     * @return this {@link DDNA} instance
     *
     * @throws IllegalArgumentException if the {@code name} is null or empty
     */
    public DDNA recordEvent(String name) {
        return recordEvent(new Event(name));
    }
    
    /**
     * Records an event with Collect.
     *
     * @param event the event
     *
     * @return this {@link DDNA} instance
     *
     * @throws IllegalArgumentException if the {@code event} is null
     */
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
            params.put("platform", ClientInfo.platform());
            params.put("sdkVersion", SDK_VERSION);
            
            jsonEvent.put("eventParams", params);
        } catch (JSONException e) {
            // should never happen due to params enforcement
            throw new IllegalArgumentException(e);
        }
        
        eventHandler.handleEvent(jsonEvent);
        
        return this;
    }
    
    /**
     * Records an event with Collect.
     *
     * @param name      the name of the event
     * @param params    the parameters of the event, may be {@code null}
     *
     * @return this {@link DDNA} instance
     *
     * @throws IllegalArgumentException if the {@code name} is null or empty
     *
     * @deprecated as of version 4, replaced by {@link #recordEvent(Event)}
     */
    @Deprecated
    public DDNA recordEvent(String name, @Nullable Params params) {
        return recordEvent((params != null)
                ? new Event(name, params)
                : new Event(name));
    }
    
    /**
     * Records an event with Collect.
     *
     * @param name      name of the event
     * @param params    parameters of the event, may be {@code null}
     *
     * @return this {@link DDNA} instance
     *
     * @throws IllegalArgumentException if the {@code name} is null or empty
     *
     * @deprecated as of version 4, replaced by {@link #recordEvent(Event)}
     */
    @Deprecated
    public DDNA recordEvent(String name, @Nullable JSONObject params) {
        return recordEvent((params != null)
                ? new Event(name, new Params(params))
                : new Event(name));
    }
    
    /**
     * Record when a push notification has been opened.
     *
     * @return this {@link DDNA} instance
     *
     * @deprecated  as of version 4.1.2, replaced by
     *              {@link #recordNotificationOpened(boolean)}
     */
    @Deprecated
    public DDNA recordNotificationOpened() {
        return recordNotificationOpened(true);
    }
    
    /**
     * Record when a push notification has been opened.
     *
     * @param launch whether the notification launched the app
     *
     * @return this {@link DDNA} instance
     *
     * @deprecated  as of version 4.1.6, replaced by
     *              {@link #recordNotificationOpened(boolean, Bundle)}
     */
    @Deprecated
    public DDNA recordNotificationOpened(boolean launch) {
        return recordEvent(new Event("notificationOpened")
                .putParam("notificationLaunch", launch));
    }
    
    /**
     * Record when a push notification has been opened.
     *
     * @param launch    whether the notification launched the app
     * @param payload   the payload of the push notification
     *
     * @return this {@link DDNA} instance
     */
    public DDNA recordNotificationOpened(boolean launch, Bundle payload) {
        final Event event = new Event("notificationOpened");
        
        if (payload.containsKey("_ddId"))
            event.putParam("notificationId", payload.getLong("_ddId"));
        if (payload.containsKey("_ddName"))
            event.putParam("notificationName", payload.getString("_ddName"));
        
        boolean insertCommunicationAttrs = false;
        if (payload.containsKey("_ddCampaign")) {
            event.putParam("campaignId", payload.getLong("_ddCampaign"));
            insertCommunicationAttrs = true;
        }
        if (payload.containsKey("_ddCohort")) {
            event.putParam("cohortId", payload.getLong("_ddCohort"));
            insertCommunicationAttrs = true;
        }
        if (insertCommunicationAttrs) {
            event.putParam("communicationSender", "GOOGLE_NOTIFICATION");
            event.putParam("communicationState", "OPEN");
        }
        
        event.putParam("notificationLaunch", launch);
        
        return recordEvent(event);
    }
    
    /**
     * Record when a push notification has been dismissed.
     *
     * @return this {@link DDNA} instance
     *
     * @deprecated  as of version 4.1.6, replaced by
     *              {@link #recordNotificationDismissed(Bundle)}
     */
    @Deprecated
    public DDNA recordNotificationDismissed() {
        return recordEvent(new Event("notificationOpened")
                .putParam("notificationLaunch", false));
    }
    
    /**
     * Record when a push notification has been dismissed.
     *
     * @param payload the payload of the push notification
     *
     * @return this {@link DDNA} instance
     */
    public DDNA recordNotificationDismissed(Bundle payload) {
        return recordNotificationOpened(false, payload);
    }
    
    /**
     * Makes an Engage request.
     * <p>
	 * The result will be passed into the provided {@code listener} through
	 * one of the callback methods on the main UI thread, even if this method
	 * was called from a background thread.
     *
     * @param decisionPoint the decision point for the engagement
     * @param listener      listener for the result
     *
     * @return this {@link DDNA} instance
     *
     * @throws IllegalArgumentException if the {@code decisionPoint} is null
     *                                  or empty
     */
    public DDNA requestEngagement(
            String decisionPoint,
            EngageListener<Engagement> listener) {
        
        return requestEngagement(new Engagement(decisionPoint), listener);
    }
    
    /**
     * Makes an Engage request.
     * <p>
	 * The result will be passed into the provided {@code listener} through
	 * one of the callback methods on the main UI thread, even if this method
	 * was called from a background thread.
     *
     * @param engagement    the engagement
     * @param listener      listener for the result
     *
     * @return this {@link DDNA} instance
     *
     * @throws IllegalArgumentException if the {@code engagement} is null
     */
    public <E extends Engagement> DDNA requestEngagement(
            E engagement,
            EngageListener<E> listener) {
        
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
                ENGAGE_API_VERSION, SDK_VERSION);
        
        return this;
    }
    
    /**
     * Makes an Engage request.
     * <p>
     * The result will be passed into the provided {@code listener} through
     * one of the callback methods on the main UI thread, even if this method
     * was called from a background thread.
     *
     * @param decisionPoint decision point for the request, as defined
     *                      in the Portal
     * @param params        additional parameters for the engagement, may be
     *                      {@code null}
     * @param listener      listener for the result
     *
     * @return this {@link DDNA} instance
     *
     * @throws IllegalArgumentException if the {@code decisionPoint} is null
     *                                  or empty
     *
     * @deprecated  as of version 4, replaced by
     *              {@link #requestEngagement(Engagement, EngageListener)}
     */
    @Deprecated
    public DDNA requestEngagement(
            String decisionPoint,
            @Nullable JSONObject params,
            EngageListener<Engagement> listener) {
        
        return requestEngagement(decisionPoint, null, params, listener);
    }
    
    /**
     * @param decisionPoint decision point for the request, as defined
     *                      in the Portal
     * @param flavour       flavour for the decision point, may be {@code null}
     * @param params        additional parameters for the engagement, may be
     *                      {@code null}
     * @param listener      listener for the result
     *
     * @return this {@link DDNA} instance
     *
     * @throws IllegalArgumentException if the {@code decisionPoint} is null
     *                                  or empty
     *
     * @deprecated  as of version 4, replaced by
     *              {@link #requestEngagement(Engagement, EngageListener)}
     */
    @Deprecated
    public DDNA requestEngagement(
            String decisionPoint,
            @Nullable String flavour,
            @Nullable JSONObject params,
            EngageListener<Engagement> listener) {
        
        return requestEngagement(
                (params != null)
                        ? new Engagement(decisionPoint, flavour, new Params(params))
                        : new Engagement(decisionPoint, flavour),
                listener);
    }
    
    /**
     * Makes a simple Image Message request.
     * <p>
     * The result will be passed into the provided {@code listener} through
     * one of the callback methods on the main UI thread, even if this method
     * was called from a background thread.
     *
     * @param decisionPoint the decision point for the engagement
     * @param listener      listener for the result
     *
     * @return this {@link DDNA} instance
     *
     * @throws IllegalArgumentException if the {@code decisionPoint} is null
     *                                  or empty
     *
     * @deprecated  as of version 4.1, replaced by
     *              {@link #requestEngagement(Engagement, EngageListener)}
     */
    @Deprecated
    public DDNA requestImageMessage(
            String decisionPoint,
            ImageMessageListener listener) {
        
        return requestImageMessage(new Engagement(decisionPoint), listener);
    }
    
    /**
     * Makes an Image Message request.
     * <p>
     * The result will be passed into the provided {@code listener} through
     * one of the callback methods on the main UI thread, even if this method
     * was called from a background thread.
     *
     * @param engagement    the engagement
     * @param listener      listener for the result
     *
     * @return this {@link DDNA} instance
     *
     * @deprecated  as of version 4.1, replaced by
     *              {@link #requestEngagement(Engagement, EngageListener)}
     */
    @Deprecated
    public DDNA requestImageMessage(
            Engagement engagement,
            ImageMessageListener listener) {
        
        return requestEngagement(engagement, listener);
    }
    
    /**
     * Sends pending events to our platform.
     * <p>
     * This is usually called automatically, but in the case that automatic
     * event uploads are disabled the game will be responsible for calling
     * this method at suitable points in the game flow.
     *
     * @return this {@link DDNA} instance
     */
    public DDNA upload() {
        eventHandler.dispatch();
        return this;
    }
    
    /**
     * Gets the user id.
     *
     * @return  the user id, may be {@code null} if the user id hasn't
     *          been set or generated by the SDK at this point
     */
    @Nullable
    public String getUserId() {
        return preferences.getUserId();
    }
    
    /**
     * Sets the user id.
     * <p>
     * Will be applied next time the SDK will be started.
     *
     * @param userId the user id, may be {@code null} in which case
     *               the SDK will generate a user id internally.
     *
     * @return this {@link DDNA} instance
     */
    public DDNA setUserId(@Nullable String userId) {
        final String currentUserId = getUserId();
        final String newUserId;
        boolean changed = false;
        
        if (TextUtils.isEmpty(currentUserId)) {
            if (TextUtils.isEmpty(userId)) {
                newUserId = UUID.randomUUID().toString();
                Log.d(BuildConfig.LOG_TAG, "Generated user id " + newUserId);
            } else {
                newUserId = userId;
            }
        } else {
            if (    !TextUtils.isEmpty(userId)
                    && !currentUserId.equals(userId)) {
                
                Log.d(BuildConfig.LOG_TAG, String.format(
                        Locale.US,
                        "User id has changed from %s to %s",
                        currentUserId,
                        userId));
                
                changed = true;
                newUserId = userId;
            } else {
                Log.d(BuildConfig.LOG_TAG, "User id has not changed");
                return this;
            }
        }
        
        preferences.setUserId(newUserId);
        if (changed) {
            preferences.clearFirstRun();
        }
        
        return this;
    }
    
    /**
     * Gets the registration id for push notifications.
     *
     * @return the registration id, may be {@code null} if not set
     */
    @Nullable
    public String getRegistrationId() {
        return preferences.getRegistrationId();
    }
    
    /**
     * Sets the registration id for push notifications.
     *
     * @param registrationId the registration id, may be {@code null}
     *                       in order to unregister from notifications
     *
     * @return this {@link DDNA} instance
     */
    public DDNA setRegistrationId(@Nullable String registrationId) {
        preferences.setRegistrationId(registrationId);
        return recordEvent(new Event("notificationServices").putParam(
                "androidRegistrationID",
                (registrationId == null) ? "" : registrationId));
    }
    
    /**
     * Clears the registration id associated with this device for disabling
     * push notifications.
     *
     * @return this {@link DDNA} instance
     */
    public DDNA clearRegistrationId() {
        return setRegistrationId(null);
    }
    
    /**
     * Clears persistent data, such as the user id, Collect events,
     * and Engage cache.
     *
     * @return this {@link DDNA} instance
     */
    public DDNA clearPersistentData() {
        preferences.clear();
        store.clear();
        archive.clear();
        
        return this;
    }
    
    public Settings getSettings() {
        return settings;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    String getEngageStoragePath() {
        return engageStoragePath;
    }
    
    // FIXME should not be exposed
    public NetworkManager getNetworkManager() {
        return network;
    }
    
    public DDNA register(SessionListener listener) {
        sessionListeners.add(listener);
        return this;
    }
    
    public DDNA unregister(SessionListener listener) {
        sessionListeners.remove(listener);
        return this;
    }
    
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
    
    DDNA(   Application application,
            String environmentKey,
            String collectUrl,
            String engageUrl,
            Settings settings,
            @Nullable String hashSecret,
            @Nullable String clientVersion,
            @Nullable String userId) {
        
        this.settings = settings;
        this.clientVersion = clientVersion;
        
        // FIXME event archive
        final File dir = application.getExternalFilesDir(null);
        final String path = (dir != null)
                ? dir.getAbsolutePath()
                : "/";
        
        preferences = new Preferences(application);
        store = new EventStore(application, settings, preferences);
        archive = new EngageArchive(engageStoragePath =
                String.format(Locale.US, ENGAGE_STORAGE_PATH, path));
        
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
        eventHandler = new EventHandler(
                store,
                archive,
                network = new NetworkManager(
                        environmentKey,
                        collectUrl,
                        engageUrl,
                        settings,
                        hashSecret));

        /* Async read of currency xml files */
        inputSourceIso4217 = application.getResources().openRawResource(R.raw.iso_4217);
        new AsyncTask<Object, Boolean, Object>() {
            @Override
            protected Object doInBackground(Object... params) {
                Log.w(BuildConfig.LOG_TAG, "Reading ISO 4217 resource file asynchronously");

                try {
                    readIso4217();
                    Log.w(BuildConfig.LOG_TAG, "Successfull read of ISO 4217 resource file");
                } catch (Exception e) {
                    //e.printStackTrace();
                    return false;
                }
                return true;
            }
        }.execute();

        setUserId(userId);
    }
    
    private static String validateUrl(String url) {
        if (    !url.toLowerCase(Locale.US).startsWith("http://")
                && !url.toLowerCase(Locale.US).startsWith("https://")) {
            
            return "http://" + url;
        }
        
        return url;
    }
    
    private static String getCurrentTimestamp() {
        return TIMESTAMP_FORMAT.format(new Date());
    }

    public void readIso4217() {
        if (iso4217 != null ) return;

        if (inputSourceIso4217 ==null ) {
            Log.w(BuildConfig.LOG_TAG, "Cannot read ISO 4217 resource file");
        }

        final XPath xpath = XPathFactory.newInstance().newXPath();
        Map<String, Integer> temp = new HashMap<>(0);
        try {
            final NodeList nodes = (NodeList) xpath.evaluate(
                    "/ISO_4217/CcyTbl/CcyNtry",
                    new InputSource(inputSourceIso4217),
                    XPathConstants.NODESET);
            temp = new HashMap<>(nodes.getLength());

            for (int i = 0; i < nodes.getLength(); i++) {
                final Element el = (Element) nodes.item(i);
                if (el.getElementsByTagName("Ccy") == null) continue;

                final String key = xpath.evaluate("Ccy", el);
                int value;
                try {
                    value = Integer.parseInt(xpath.evaluate("CcyMnrUnts", el));
                } catch (NumberFormatException e) {
                    value = 0;
                }

                temp.put(key, value);
            }
        } catch (XPathExpressionException e) {
            Log.w(BuildConfig.LOG_TAG, "Failed parsing ISO 4217 resource", e);
        } finally {
            iso4217 = Collections.unmodifiableMap(temp);
        }

    }

    /**
     * Class for providing a configuration when initialising the
     * SDK through {@link DDNA#initialise(Configuration)} inside of an
     * {@link Application} class.
     */
    public static final class Configuration {
        
        private final Application application;
        private final String environmentKey;
        private final String collectUrl;
        private final String engageUrl;
        
        @Nullable
        private String hashSecret;
        @Nullable
        private String clientVersion;
        @Nullable
        private String userId;
        
        private final Settings settings;
        
        public Configuration(
                Application application,
                String environmentKey,
                String collectUrl,
                String engageUrl) {
            
            Preconditions.checkArg(
                    application != null,
                    "application cannot be null");
            Preconditions.checkArg(
                    !TextUtils.isEmpty(environmentKey),
                    "environmentKey cannot be null or empty");
            Preconditions.checkArg(
                    !TextUtils.isEmpty(collectUrl),
                    "collectUrl cannot be null or empty");
            Preconditions.checkArg(
                    !TextUtils.isEmpty(engageUrl),
                    "engageUrl cannot be null or empty");
            
            this.application = application;
            this.environmentKey = environmentKey;
            this.collectUrl = validateUrl(collectUrl);
            this.engageUrl = validateUrl(engageUrl);
            
            this.settings = new Settings();
        }
        
        /**
         * Sets the hash secret.
         *
         * @param hashSecret the hash secret
         *
         * @return this {@link Configuration} instance
         */
        public Configuration hashSecret(@Nullable String hashSecret) {
            this.hashSecret = hashSecret;
            return this;
        }
        
        /**
         * Sets the client version.
         * <p>
         * Could be the {@code VERSION_NAME} from your application's
         * {@code BuildConfig}.
         *
         * @param clientVersion the client version
         *
         * @return this {@link Configuration} instance
         */
        public Configuration clientVersion(@Nullable String clientVersion) {
            this.clientVersion = clientVersion;
            return this;
        }
        
        /**
         * Sets the user id.
         * <p>
         * You may use this method to set the user id if you create
         * one in your {@link Application} class, else you may ignore
         * it and set the id later, or let the SDK create its own id.
         *
         * @param userId the user id
         *
         * @return this {@link Configuration} instance
         */
        public Configuration userId(@Nullable String userId) {
            this.userId = userId;
            return this;
        }
        
        /**
         * Allows changing of {@link Settings} values.
         *
         * @param modifier the settings modifier
         *
         * @return this {@link Configuration} instance
         */
        public Configuration withSettings(SettingsModifier modifier) {
            modifier.modify(settings);
            return this;
        }
    }
    
    public interface SettingsModifier {
        
        void modify(Settings settings);
    }
}
