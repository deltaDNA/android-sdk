/*
 * Copyright (c) 2017 deltaDNA Ltd. All rights reserved.
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

/**
 * {@link BroadcastReceiver} which can be extended for listening to events
 * related to notifications. Override any of the {@code on*} event callback
 * methods to listen to the respective events as they happen.
 * <p>
 * The class will need to be registered in your application's manifest file in
 * order to receive the event callbacks, for example:
 * <pre>{@code
 * <receiver
 *     name="your.package.name.YourClassName"
 *     exported="false"
 *     
 *     <intent-filter>
 *         <action android:name="com.deltadna.android.sdk.notifications.REGISTERED"/>
 *         <action android:name="com.deltadna.android.sdk.notifications.REGISTRATION_FAILED"/>
 *         <action android:name="com.deltadna.android.sdk.notifications.MESSAGE_RECEIVED"/>
 *         <action android:name="com.deltadna.android.sdk.notifications.NOTIFICATION_POSTED"/>
 *         <action android:name="com.deltadna.android.sdk.notifications.NOTIFICATION_OPENED"/>
 *         <action android:name="com.deltadna.android.sdk.notifications.NOTIFICATION_DISMISSED"/>
 *     </intent-filter>
 *  </receiver>
 * }</pre>
 * <p>
 * If targeting API 26 or higher the receiver needs to be registered explicitly:
 * <pre>{@code
 * DDNANotifications.setReceiver(MyEventReceiver.class);
 * }</pre>
 */
public abstract class EventReceiver extends BroadcastReceiver {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + EventReceiver.class.getSimpleName();
    
    @Override
    public final void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null) {
            final NotificationInfo info = (NotificationInfo)
                    (intent.hasExtra(Actions.NOTIFICATION_INFO)
                            ? intent.getSerializableExtra(Actions.NOTIFICATION_INFO)
                            : null);
            
            switch (intent.getAction()) {
                case Actions.REGISTERED:
                    final String token = intent.getStringExtra(
                            Actions.REGISTRATION_TOKEN);
                    if (TextUtils.isEmpty(token)) {
                        Log.w(TAG, "Registration token is null or empty");
                    } else {
                        onRegistered(context, token);
                    }
                    break;
                
                case Actions.REGISTRATION_FAILED:
                    final Throwable reason = (Throwable) intent.getSerializableExtra(
                            Actions.REGISTRATION_FAILURE_REASON);
                    if (reason == null) {
                        Log.w(TAG, "Failed to deserialise registration failure reason");
                    } else {
                        onRegistrationFailed(context, reason);
                    }
                    break;
                
                case Actions.MESSAGE_RECEIVED:
                    final PushMessage message = (PushMessage) intent
                            .getSerializableExtra(Actions.PUSH_MESSAGE);
                    if (message == null) {
                        Log.w(TAG, "Failed to find or deserialise push message");
                    } else {
                        onMessageReceived(context, message);
                    }
                    break;
                
                case Actions.NOTIFICATION_POSTED:
                    if (info == null) {
                        Log.w(TAG, "Failed to find or deserialise notification info");
                    } else {
                        onNotificationPosted(context, info);
                    }
                    break;
                
                case Actions.NOTIFICATION_OPENED:
                    if (info == null) {
                        Log.w(TAG, "Failed to find or deserialise notification info");
                    } else {
                        onNotificationOpened(context, info);
                    }
                    break;
                
                case Actions.NOTIFICATION_DISMISSED:
                    if (info == null) {
                        Log.w(TAG, "Failed to find or deserialise notification info");
                    } else {
                        onNotificationDismissed(context, info);
                    }
                    break;
                
                default:
                    Log.w(TAG, "Unknown action: " + intent.getAction());
            }
        }
    }
    
    /**
     * Will be called when the SDK registers for notifications and receives
     * a registration token from the Google backend.
     *
     * @param context           the context of the receiver
     * @param registrationId    the registration token/id
     */
    protected void onRegistered(Context context, String registrationId) {}
    
    /**
     * Will be called when the SDK fails to register for push notifications.
     *
     * @param context           the context of the receiver
     * @param reason            the reason for the failure
     */
    protected void onRegistrationFailed(Context context, Throwable reason) {}
    
    /**
     * Will be called when the SDK constructs a push message from a remote
     * message sent by the Google backend, to be posted as as a user
     * notification.
     *
     * @param context   the context of the receiver
     * @param message   the push message
     */
    protected void onMessageReceived(Context context, PushMessage message) {}
    
    /**
     * Will be called when the SDK posts a notification.
     *
     * @param context   the context of the receiver
     * @param info      the posted notification info
     */
    protected void onNotificationPosted(Context context, NotificationInfo info) {}
    /**
     * Will be called when the user opens a notification posted by the SDK.
     *
     * @param context   the context of the receiver
     * @param info      the opened notification info
     */
    protected void onNotificationOpened(Context context, NotificationInfo info) {}
    /**
     * Will be called when the user dismissed a notification posted by the SDK.
     *
     * @param context   the context of the receiver
     * @param info      the dismissed notification info
     */
    protected void onNotificationDismissed(Context context, NotificationInfo info) {}
}
