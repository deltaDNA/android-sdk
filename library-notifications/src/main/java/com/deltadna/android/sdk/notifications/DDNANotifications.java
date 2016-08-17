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
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.deltadna.android.sdk.DDNA;

/**
 * Helper class for easily registering/un-registering for/from push
 * notifications.
 */
public final class DDNANotifications {
    
    /**
     * Action which will be broadcast over the
     * {@link android.support.v4.content.LocalBroadcastManager}
     * when retrieving a registration token from GCM succeeds.
     * <p>
     * The token will be included in the {@link Intent} under the
     * {@link #EXTRA_REGISTRATION_TOKEN} key as a {@link String} value.
     *
     * @see #EXTRA_REGISTRATION_TOKEN
     */
    public static final String ACTION_TOKEN_RETRIEVAL_SUCCESSFUL =
            "com.deltadna.android.sdk.notifications.TOKEN_RETRIEVAL_SUCCESSFUL";
    
    /**
     * Action which will be broadcast over the
     * {@link android.support.v4.content.LocalBroadcastManager}
     * when retrieving a registration token from GCM fails.
     * <p>
     * The reason for the failure will be included in the {@link Intent} under
     * the {@link #EXTRA_FAILURE_REASON} key as a serialized {@link Throwable}
     * value.
     *
     * @see #EXTRA_FAILURE_REASON
     */
    public static final String ACTION_TOKEN_RETRIEVAL_FAILED =
            "com.deltadna.android.sdk.notifications.TOKEN_RETRIEVAL_FAILED";
    
    public static final String EXTRA_REGISTRATION_TOKEN = "token";
    public static final String EXTRA_FAILURE_REASON = "reason";
    
    public static final String EXTRA_PAYLOAD = "payload";
    public static final String EXTRA_LAUNCH = "launch";
    
    /**
     * {@link IntentFilter} to be used when registering a
     * {@link android.content.BroadcastReceiver} for listening to both token
     * retrieval successes and failures.
     *
     * @see #ACTION_TOKEN_RETRIEVAL_SUCCESSFUL
     * @see #ACTION_TOKEN_RETRIEVAL_FAILED
     */
    public static final IntentFilter FILTER_TOKEN_RETRIEVAL;
    static {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_TOKEN_RETRIEVAL_SUCCESSFUL);
        filter.addAction(ACTION_TOKEN_RETRIEVAL_FAILED);
        
        FILTER_TOKEN_RETRIEVAL = filter;
    }
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + DDNANotifications.class.getSimpleName();
    
    /**
     * Register the client for push notifications.
     * <p>
     * A good time to perform this would be for example when the user enables
     * push notifications from the game's settings, or when a previous
     * registration attempt fails as notified by
     * {@link #ACTION_TOKEN_RETRIEVAL_FAILED}.
     * <p>
     * Method can be safely called from the Unity SDK, but local broadcasts
     * will not be sent.
     *
     * @see DDNA#setRegistrationId(String)
     */
    public static void register(Context context) {
        Log.d(TAG, "Registering for push notifications");
        
        context.startService(new Intent(
                context,
                RegistrationIntentService.class));
    }
    
    /**
     * Unregister the client from push notifications.
     *
     * @see DDNA#clearRegistrationId()
     *
     * @throws UnsupportedOperationException if called from Unity
     */
    public static void unregister() {
        if (Unity.isPresent()) {
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
        
        if (Unity.isPresent()) {
            final Bundle copy = new Bundle(payload);
            copy.putString("_ddCommunicationSender", "GOOGLE_NOTIFICATION");
            copy.putBoolean("_ddLaunch", launch);
            
            Unity.sendMessage(
                    "DeltaDNA.AndroidNotifications",
                    launch  ? "DidLaunchWithPushNotification"
                            : "DidReceivePushNotification",
                    Utils.convert(copy));
        } else {
            DDNA.instance().recordNotificationOpened(launch, payload);
        }
    }
    
    /**
     * Notifies the SDK that a push notification has been dismissed by the user.
     *
     * @deprecated  as of version 4.1.6, replaced by
     *              {@link #recordNotificationDismissed(Bundle)}
     */
    @Deprecated
    public static void recordNotificationDismissed() {
        if (!Unity.isPresent()) {
            DDNA.instance().recordNotificationDismissed();
        } // `else` Unity doesn't have this method
    }
    
    /**
     * Notifies the SDK that a push notification has been dismissed by the user.
     *
     * @param payload the payload of the push notification
     */
    public static void recordNotificationDismissed(Bundle payload) {
        if (!Unity.isPresent()) {
            DDNA.instance().recordNotificationDismissed(payload);
        } // `else` Unity doesn't have this method
    }
    
    private DDNANotifications() {}
}
