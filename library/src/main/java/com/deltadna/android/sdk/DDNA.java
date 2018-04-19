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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.deltadna.android.sdk.exceptions.NotInitialisedException;
import com.deltadna.android.sdk.helpers.ClientInfo;
import com.deltadna.android.sdk.helpers.Preconditions;
import com.deltadna.android.sdk.helpers.Settings;
import com.deltadna.android.sdk.listeners.EngageListener;
import com.deltadna.android.sdk.listeners.EventListener;
import com.deltadna.android.sdk.net.NetworkManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.WeakHashMap;

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
public abstract class DDNA {
    
    static final String SDK_VERSION =
            "Android SDK v" + BuildConfig.VERSION_NAME;
    
    private static final SimpleDateFormat TIMESTAMP_FORMAT;
    static {
        final SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        TIMESTAMP_FORMAT = format;
    }
    
    private static DDNA instance;
    
    public static synchronized DDNA initialise(Configuration configuration) {
        Preconditions.checkArg(
                configuration != null,
                "configuration cannot be null");
        
        if (instance == null) {
            instance = new DDNADelegate(
                    configuration,
                    new DDNAImpl(
                            configuration.application,
                            configuration.environmentKey,
                            configuration.collectUrl,
                            configuration.engageUrl,
                            configuration.settings,
                            configuration.hashSecret,
                            configuration.clientVersion,
                            configuration.userId,
                            configuration.platform),
                    new DDNANonTracking(
                            configuration.application,
                            configuration.environmentKey,
                            configuration.collectUrl,
                            configuration.engageUrl,
                            configuration.settings,
                            configuration.hashSecret,
                            configuration.platform));
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
    
    protected final Settings settings;
    protected final String platform;
    
    final Preferences preferences;
    final NetworkManager network;
    private final EngageFactory engageFactory;
    
    final Set<EventListener> eventListeners = Collections.newSetFromMap(
            new WeakHashMap<EventListener, Boolean>(1));
    
    protected String sessionId = UUID.randomUUID().toString();
    
    DDNA(   Application application,
            String environmentKey,
            String collectUrl,
            String engageUrl,
            Settings settings,
            @Nullable String hashSecret,
            @Nullable String platform) {
        
        this.settings = settings;
        this.platform = (platform == null) ? ClientInfo.platform() : platform;
        
        preferences = new Preferences(application);
        network = new NetworkManager(
                environmentKey,
                collectUrl,
                engageUrl,
                settings,
                hashSecret);
        engageFactory = new EngageFactory(this);
    }
    
    /**
     * Starts the SDK.
     * <p>
     * This method needs to be called before sending events or making
     * engagements.
     *
     * @return this {@link DDNA} instance
     */
    public abstract DDNA startSdk();
    
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
    public abstract DDNA startSdk(@Nullable String userId);
    
    /**
     * Stops the SDK.
     * <p>
     * Calling this method sends a 'gameEnded' event to Collect, disables
     * background uploads and automatic session refreshing.
     *
     * @return this {@link DDNA} instance
     */
    public abstract DDNA stopSdk();
    
    /**
     * Queries the status of the SDK.
     *
     * @return {@code true} if the SDK has been started, else {@code false}
     */
    public abstract boolean isStarted();
    
    /**
     * Records an event with Collect.
     *
     * @param name the name of the event
     *
     * @return this {@link DDNA} instance
     *
     * @throws IllegalArgumentException if the {@code name} is null or empty
     */
    public abstract DDNA recordEvent(String name);
    
    /**
     * Records an event with Collect.
     *
     * @param event the event
     *
     * @return this {@link DDNA} instance
     *
     * @throws IllegalArgumentException if the {@code event} is null
     */
    public abstract DDNA recordEvent(Event event);
    
    /**
     * Record when a push notification has been opened.
     *
     * @param launch    whether the notification launched the app
     * @param payload   the payload of the push notification
     *
     * @return this {@link DDNA} instance
     */
    public abstract DDNA recordNotificationOpened(boolean launch, Bundle payload);
    
    /**
     * Record when a push notification has been dismissed.
     *
     * @param payload the payload of the push notification
     *
     * @return this {@link DDNA} instance
     */
    public abstract DDNA recordNotificationDismissed(Bundle payload);
    
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
     *
     * @see EngageFactory
     */
    public abstract DDNA requestEngagement(
            String decisionPoint,
            EngageListener<Engagement> listener);
    
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
     *
     * @see EngageFactory
     */
    public abstract <E extends Engagement> DDNA requestEngagement(
            E engagement,
            EngageListener<E> listener);
    
    /**
     * Sends pending events to our platform.
     * <p>
     * This is usually called automatically, but in the case that automatic
     * event uploads are disabled the game will be responsible for calling
     * this method at suitable points in the game flow.
     *
     * @return this {@link DDNA} instance
     */
    public abstract DDNA upload();
    
    /**
     * Gets the registration id for push notifications.
     *
     * @return the registration id, may be {@code null} if not set
     */
    @Nullable
    public abstract String getRegistrationId();
    
    /**
     * Sets the registration id for push notifications.
     *
     * @param registrationId the registration id, may be {@code null}
     *                       in order to unregister from notifications
     *
     * @return this {@link DDNA} instance
     */
    public abstract DDNA setRegistrationId(@Nullable String registrationId);
    
    /**
     * Clears the registration id associated with this device for disabling
     * push notifications.
     *
     * @return this {@link DDNA} instance
     */
    public abstract DDNA clearRegistrationId();
    
    /**
     * Clears persistent data, such as the user ID, Collect events, and Engage
     * cache.
     * <p>
     * This also normally stops the SDK, as a re-start will be required in order
     * to re-initialise the user ID.
     *
     * @return this {@link DDNA} instance
     */
    public abstract DDNA clearPersistentData();
    
    /**
     * Forgets the current user and stops them from being tracked.
     * <p>
     * Any subsequent calls on the SDK will succeed, but not send/request
     * anything to/from the Platform.
     * <p>
     * The status can be cleared by starting the SDK with a new user through
     * {@link #startSdk(String)} or by clearing the persistent data with
     * {@link #clearPersistentData()}.
     *
     * @return this {@link DDNA} instance
     */
    public abstract DDNA forgetMe();
    
    abstract File getEngageStoragePath();
    
    abstract Map<String, Integer> getIso4217();
    
    /**
     * Changes the session id.
     *
     * @return this {@link DDNA} instance
     */
    public final DDNA newSession() {
        return newSession(false);
    }
    
    public final String getSessionId() {
        return sessionId;
    }
    
    /**
     * Gets the user id.
     *
     * @return  the user id, may be {@code null} if the user id hasn't
     *          been set or generated by the SDK at this point
     */
    @Nullable
    public final String getUserId() {
        return preferences.getUserId();
    }
    
    /**
     * Gets the Engage factory which provides an easier way of requesting
     * Engage actions.
     *
     * @return the {@link EngageFactory}
     */
    public final EngageFactory getEngageFactory() {
        return engageFactory;
    }
    
    public final Settings getSettings() {
        return settings;
    }
    
    // FIXME should not be exposed
    public final NetworkManager getNetworkManager() {
        return network;
    }
    
    public final DDNA register(EventListener listener) {
        eventListeners.add(listener);
        return this;
    }
    
    public final DDNA unregister(EventListener listener) {
        eventListeners.remove(listener);
        return this;
    }
    
    final DDNA setUserId(@Nullable String userId) {
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
    
    final DDNA newSession(boolean suppressWarning) {
        if (!suppressWarning && settings.getSessionTimeout() > 0) {
            Log.w(  BuildConfig.LOG_TAG,
                    "Automatic session refreshing is enabled");
        }
        
        sessionId = UUID.randomUUID().toString();
        
        for (final EventListener listener : eventListeners) {
            listener.onNewSession();
        }
        
        return this;
    }
    
    static String getCurrentTimestamp() {
        return TIMESTAMP_FORMAT.format(new Date());
    }
    
    /**
     * Class for providing a configuration when initialising the
     * SDK through {@link DDNA#initialise(Configuration)} inside of an
     * {@link Application} class.
     */
    public static final class Configuration {
        
        protected final Application application;
        final String environmentKey;
        final String collectUrl;
        final String engageUrl;
        
        @Nullable
        String hashSecret;
        @Nullable
        String clientVersion;
        @Nullable
        String userId;
        @Nullable
        String platform;
        
        protected final Settings settings;
        
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
            this.collectUrl = fixUrl(collectUrl);
            this.engageUrl = fixUrl(engageUrl);
            
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
         * Sets the value for the platform field.
         * <p>
         * If not set the value will default to {@link ClientInfo#platform()}.
         *
         * @param platform the platform
         *
         * @return this {@link Configuration} instance
         *
         * @see ClientInfo#PLATFORM_ANDROID
         * @see ClientInfo#PLATFORM_AMAZON
         */
        public Configuration platform(String platform) {
            this.platform = platform;
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
        
        private static String fixUrl(String url) {
            if (    !url.toLowerCase(Locale.US).startsWith("http://")
                    && !url.toLowerCase(Locale.US).startsWith("https://")) {
                return "https://" + url;
            } else if (url.toLowerCase(Locale.US).startsWith("http://")) {
                Log.w(BuildConfig.LOG_TAG, "Changing " + url + " to use HTTPS");
                return "https://" + url.substring("http://".length(), url.length());
            } else {
                return url;
            }
        }
    }
    
    interface SettingsModifier {
        
        void modify(Settings settings);
    }
}
