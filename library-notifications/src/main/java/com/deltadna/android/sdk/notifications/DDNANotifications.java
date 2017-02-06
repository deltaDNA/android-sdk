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

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.deltadna.android.sdk.DDNA;
import com.google.firebase.iid.FirebaseInstanceId;

/**
 * Helper class for easily registering/un-registering for/from push
 * notifications.
 */
public final class DDNANotifications {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + DDNANotifications.class.getSimpleName();
    
    /**
     * Register the client for push notifications.
     *
     * @see DDNA#setRegistrationId(String)
     *
     * @throws UnsupportedOperationException if called from Unity
     */
    public static void register() {
        if (UnityForwarder.isPresent()) {
            throw new UnsupportedOperationException(
                    "Unity SDK should unregister from its own code");
        }
        
        Log.d(TAG, "Registering for push notifications");
        
        final String token = FirebaseInstanceId.getInstance().getToken();
        if (TextUtils.isEmpty(token)) {
            Log.w(TAG, "Registration token is not available");
        } else {
            DDNA.instance().setRegistrationId(token);
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
            copy.putString("_ddCommunicationSender", "GOOGLE_NOTIFICATION");
            copy.putBoolean("_ddLaunch", launch);
            
            UnityForwarder.getInstance().forward(
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
     * @param payload the payload of the push notification
     */
    public static void recordNotificationDismissed(Bundle payload) {
        if (!UnityForwarder.isPresent()) {
            DDNA.instance().recordNotificationDismissed(payload);
        } // `else` Unity doesn't have this method
    }
    
    public static void markUnityLoaded() {
        UnityForwarder.getInstance().markLoaded();
    }
    
    private DDNANotifications() {}
}
