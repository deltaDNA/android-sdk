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

package com.deltadna.android.sdk.notifications;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import com.deltadna.android.sdk.DDNA;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Helper class for easily registering/un-registering for/from push
 * notifications.
 */
@UnityInterOp
public final class DDNANotifications {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + DDNANotifications.class.getSimpleName();
    private static final String NAME = "deltadna-sdk-notifications";
    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();
    
    /**
     * Register the client for push notifications.
     * <p>
     * A good time to perform this would be for example when the user enables
     * push notifications from the game's settings, or when a previous
     * registration attempt fails as notified by an {@link EventReceiver}.
     * <p>
     * This method registers the Firebase Cloud Messaging used by deltaDNA as
     * the main one.
     *
     * @param context   the context
     *
     * @throws IllegalStateException    if the configuration meta-data entries
     *                                  are missing from the manifest
     *
     * @see #register(Context, boolean)
     * @see DDNA#setRegistrationId(String)
     */
    public static void register(Context context) {
        register(context, false);
    }
    
    /**
     * Register the client for push notifications.
     * <p>
     * A good time to perform this would be for example when the user enables
     * push notifications from the game's settings, or when a previous
     * registration attempt fails as notified by an {@link EventReceiver}.
     * <p>
     * If you have multiple Firebase Cloud Messaging senders in your project
     * then you can use the deltaDNA sender either as a main one or as a
     * secondary one by setting the {@code secondary} parameter. If you set
     * {@code secondary} to {@code true} then the default FCM sender will need
     * to have been initialised beforehand.
     *
     * @param context   the context
     * @param secondary whether the {@link FirebaseApp} instance used for
     *                  deltaDNA notifications should be registered as a
     *                  secondary (non-main) instance
     *
     * @throws IllegalStateException    if the configuration meta-data entries
     *                                  are missing from the manifest
     *
     * @see DDNA#setRegistrationId(String)
     */
    public static void register(final Context context, boolean secondary) {
        Log.d(TAG, "Registering for push notifications");
        
        final String applicationId;
        final String senderId;
        try {
            final Bundle metaData = MetaData.get(context);
            
            applicationId = context.getString(
                    metaData.getInt(MetaData.APPLICATION_ID));
            senderId = context.getString(
                    metaData.getInt(MetaData.SENDER_ID));
        } catch (final Resources.NotFoundException e) {
            throw new IllegalStateException(
                    String.format(
                            Locale.US,
                            "Failed to find configuration meta-data, have %s and %s been defined in the manifest?",
                            MetaData.APPLICATION_ID,
                            MetaData.SENDER_ID),
                    e);
        }
        
        synchronized (DDNANotifications.class) {
            final String name = secondary ? NAME : FirebaseApp.DEFAULT_APP_NAME;
            
            boolean found = false;
            for (FirebaseApp app : FirebaseApp.getApps(context)) {
                if (app.getName().equals(name)) {
                    found = true;
                }
            }
            
            if (!found) {
                /*
                 * Running this the first time will force a token refresh, in
                 * other use cases the service will be invoked when the token
                 * will need to be refreshed so there is no need to perform
                 * a forced refresh at this point.
                 */
                FirebaseApp.initializeApp(
                        context,
                        new FirebaseOptions.Builder()
                                .setApplicationId(applicationId)
                                .setGcmSenderId(senderId)
                                .build(),
                        name);
            } else {
                EXECUTOR.execute(new Runnable() {
                    @Override
                    public void run() {
                        RegistrationTokenFetcher.fetch(context);
                    }
                });
            }
        }
    }
    
    /**
     * Unregister the client from push notifications.
     *
     * @see DDNA#clearRegistrationId()
     *
     * @throws UnsupportedOperationException if called from Unity
     */
    public static void unregister() {
        if (UnityForwarder.isPresent()) {
            throw new UnsupportedOperationException(
                    "Unity SDK should unregister from its own code");
        }
        
        Log.d(TAG, "Unregistering from push notifications");
        
        DDNA.instance().clearRegistrationId();
    }
    
    /**
     * Notifies the SDK that a push notification has been opened by the user.
     *
     * @param payload   the payload of the push notification
     * @param launch    whether the notification launched the app
     */
    public static void recordNotificationOpened(
            Bundle payload,
            boolean launch) {
        
        if (UnityForwarder.isPresent()) {
            final Bundle copy = new Bundle(payload);
            copy.putBoolean("_ddLaunch", launch);
            
            UnityForwarder.getInstance().forward(
                    "DeltaDNA.AndroidNotifications",
                    "DidReceivePushNotification",
                    Utils.convert(copy));
        } else {
            DDNA.instance().recordNotificationOpened(launch, payload);
        }
    }
    
    public static void markUnityLoaded() {
        UnityForwarder.getInstance().markLoaded();
    }
    
    private DDNANotifications() {}
}
