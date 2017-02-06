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

package com.deltadna.android.sdk.notifications.example;

import android.content.Context;
import android.util.Log;

import com.deltadna.android.sdk.notifications.EventReceiver;
import com.deltadna.android.sdk.notifications.NotificationInfo;
import com.deltadna.android.sdk.notifications.PushMessage;

/**
 * Example {@link com.deltadna.android.sdk.notifications.EventReceiver}
 * demonstrating how to listen for events related to push notifications.
 */
public class ExampleReceiver extends EventReceiver {
    
    @Override
    protected void onRegistered(Context context, String registrationId) {
        Log.d(BuildConfig.LOG_TAG, "onRegistered with: " + registrationId);
    }
    
    @Override
    protected void onMessageReceived(Context context, PushMessage message) {
        Log.d(BuildConfig.LOG_TAG, "onMessageReceived with: " + message);
    }
    
    @Override
    protected void onNotificationPosted(Context context, NotificationInfo info) {
        Log.d(BuildConfig.LOG_TAG, "onNotificationPosted with: " + info);
    }
    
    @Override
    protected void onNotificationOpened(Context context, NotificationInfo info) {
        Log.d(BuildConfig.LOG_TAG, "onNotificationOpened with: " + info);
    }
    
    @Override
    protected void onNotificationDismissed(Context context, NotificationInfo info) {
        Log.d(BuildConfig.LOG_TAG, "onNotificationDismissed with: " + info);
    }
}
