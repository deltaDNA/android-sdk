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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * {@link BroadcastReceiver} which notifies the SDK when the user has
 * interacted with a push notification.
 */
public final class NotificationInteractionReceiver extends BroadcastReceiver {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + NotificationInteractionReceiver.class.getSimpleName();
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received " + intent);
        
        final String action = intent.getAction();
        if (action != null) {
            if (action.equals(Actions.NOTIFICATION_OPENED)) {
                // to make the behaviour consistent with iOS on Unity
                if (intent.getBooleanExtra(
                        DDNANotifications.EXTRA_LAUNCH, false)) {
                    
                    Log.d(TAG, "Notifying SDK of notification opening");
                    DDNANotifications.recordNotificationOpened(
                            intent.getBundleExtra(DDNANotifications.EXTRA_PAYLOAD),
                            true);
                }
                
                if (MetaData.get(context).getBoolean(
                        MetaData.START_LAUNCH_INTENT, true)) {
                    
                    Log.d(TAG, "Starting activity with launch intent");
                    
                    context.startActivity(context
                            .getPackageManager()
                            .getLaunchIntentForPackage(
                                    context.getPackageName()));
                }
            } else if (action.equals(Actions.NOTIFICATION_DISMISSED)) {
                Log.d(TAG, "Notifying SDK of notification dismissal");
                DDNANotifications.recordNotificationDismissed(
                        intent.getBundleExtra(DDNANotifications.EXTRA_PAYLOAD));
            } else {
                Log.w(TAG, "Unexpected action " + action);
            }
        } else {
            Log.w(TAG, "Null action");
        }
    }
}
