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

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * {@link BroadcastReceiver} which notifies the SDK when the user has
 * interacted with a push notification.
 */
public final class NotificationInteractionReceiver extends BroadcastReceiver {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + NotificationInteractionReceiver.class.getSimpleName();
    private static final short DELAY = 5000;
    
    private final Handler notifier = new Handler(Looper.myLooper());
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received " + intent);
        
        final String action = intent.getAction();
        if (action != null) {
            final Intent intentCopy = new Intent();
            
            switch (action) {
                case Actions.NOTIFICATION_OPENED_INTERNAL:
                    intentCopy.setAction(Actions.NOTIFICATION_OPENED);
                    
                    if (MetaData.get(context).containsKey(MetaData.START_LAUNCH_INTENT)) {
                        Log.w(  TAG,
                                "Use of ddna_start_launch_intent in the manifest has been deprecated");
                    }
                    
                    if (MetaData.get(context).getBoolean(
                            MetaData.START_LAUNCH_INTENT,
                            true)) {
                        
                        final NotificationInfo info = (NotificationInfo)
                                intent.getSerializableExtra(Actions.NOTIFICATION_INFO);
                        if (info == null) {
                            Log.w(  TAG,
                                    "Failed to find/deserialise notification info");
                        } else {
                            intentCopy.putExtra(Actions.NOTIFICATION_INFO, info);
                        }
                        
                        final Intent launchIntent = context
                                .getPackageManager()
                                .getLaunchIntentForPackage(context.getPackageName());
                        
                        if (info != null) {
                            final Checker checker = new Checker(launchIntent);
                            final Application app = (Application) context.getApplicationContext();
                            app.registerActivityLifecycleCallbacks(checker);
                            
                            notifier.postDelayed(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            Log.d(  TAG,
                                                    "Notifying SDK of notification opening");
                                            DDNANotifications.recordNotificationOpened(
                                                    Utils.convert(info.message.data),
                                                    checker.launched);
                                        }
                                    },
                                    DELAY);
                        }
                        
                        Log.d(TAG, "Starting activity with launch intent");
                        context.startActivity(launchIntent);
                    }
                    break;
                
                case Actions.NOTIFICATION_DISMISSED_INTERNAL:
                    intentCopy.setAction(Actions.NOTIFICATION_DISMISSED);
                    Log.d(TAG, "Notification has been dismissed");
                    break;
                
                default:
                    Log.d(TAG, "Ignoring " + action);
                    return;
            }
            
            /*
             * We need to pass the intent on as we have received it as an
             * explicit broadcast, thus inadvertently stopping anyone else from
             * receiving it. We've also made sure to change the actions so we
             * don't receive it a second time.
             */
            context.sendBroadcast(Utils.wrapWithReceiver(context, intentCopy));
        } else {
            Log.w(TAG, "Null action");
        }
    }
    
    private class Checker implements Application.ActivityLifecycleCallbacks {
        
        final Intent intent;
        boolean launched;
        
        Checker(Intent intent) {
            this.intent = intent;
        }
        
        private boolean intentMatches(Activity activity) {
            final Intent other = activity.getIntent();
            return (other != null
                    && other.getComponent() != null
                    && other.getComponent().equals(intent.getComponent())
                    && other.getAction() != null
                    && other.getAction().equals(intent.getAction()));
        }
        
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            if (intentMatches(activity)) {
                launched = true;
            }
        }
        
        @Override
        public void onActivityStarted(Activity activity) {
            if (intentMatches(activity)) {
                launched = true;
            }
        }
        
        @Override
        public void onActivityResumed(Activity activity) {}
        
        @Override
        public void onActivityPaused(Activity activity) {}
        
        @Override
        public void onActivityStopped(Activity activity) {}
        
        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
        
        @Override
        public void onActivityDestroyed(Activity activity) {}
    }
}
