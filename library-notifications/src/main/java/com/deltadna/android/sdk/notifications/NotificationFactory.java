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

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Factory class responsible for filling details from a push message and
 * creating a notification to be posted on the UI.
 * <p>
 * The default implementation uses the 'title' and 'alert' fields from the push
 * message as the title and message respectively for the notification. If the
 * title has not been defined in the message then the application's name will
 * be used instead. Upon selection the notification will open the launch
 * {@code Intent} of your application.
 * <p>
 * The default behaviour can be customised by extending the class and overriding
 * {@link #configure(NotificationCompat.Builder, PushMessage)}. The
 * {@link NotificationListenerService} will then need to be extended in order
 * to define the new factory to be used for creating notifications.
 * <p>
 * Customisation can be also performed by setting either of the following
 * {@code meta-data} attributes inside the {@code application} tag of your
 * manifest file:
 * <pre>{@code
 * <meta-data
 *     android:name="ddna_notification_title"
 *     android:resource="@string/your_title_resource"/>
 *
 * <meta-data
 *     android:name="ddna_notification_title"
 *     android:value="your-literal-title"/>
 *
 * <meta-data
 *     android:name="ddna_notification_icon"
 *     android:value="your_icon_resource_name"/>
 *
 * <meta-data
 *     android:name="ddna_start_launch_intent"
 *     android:value="false"/>
 * }</pre>
 */
public class NotificationFactory {
    
    protected final Context context;
    
    public NotificationFactory(Context context) {
        this.context = context;
    }
    
    /**
     * Fills a {@link android.support.v4.app.NotificationCompat.Builder}
     * with details from a {@link PushMessage}.
     *
     * @param builder   the notification builder to be configured
     * @param message   the push message
     *
     * @return          configured notification builder
     */
    public NotificationCompat.Builder configure(
            NotificationCompat.Builder builder,
            PushMessage message) {
        
        return builder
                .setSmallIcon(message.icon)
                .setContentTitle(message.title)
                .setContentText(message.message)
                .setAutoCancel(true);
    }
    
    /**
     * Creates a {@link Notification} from 
     *
     * Implementations which call
     * {@link NotificationCompat.Builder#setContentIntent(PendingIntent)}
     * or
     * {@link NotificationCompat.Builder#setDeleteIntent(PendingIntent)}
     * on the {@link NotificationCompat.Builder} and thus override the default
     * behaviour should notify the SDK that the push notification has been
     * opened or dismissed respectively.
     *
     * @param builder   the configured notification builder
     * @param info      the notification info
     *
     * @return          notification to post on the UI, or {@code null} if a
     *                  notification shouldn't be posted
     *
     * @see NotificationInteractionReceiver
     * @see EventReceiver
     */
    @Nullable
    public Notification create(
            NotificationCompat.Builder builder,
            NotificationInfo info) {
        
        // to make the behaviour consistent with iOS on Unity
        final boolean backgrounded = !Utils.inForeground(context);
        if (!backgrounded) {
            Log.d(BuildConfig.LOG_TAG, "Notifying SDK of notification opening");
            DDNANotifications.recordNotificationOpened(
                    Utils.convert(info.message.data),
                    false);
        }
        
        builder.setContentIntent(PendingIntent.getBroadcast(
                context,
                0,
                new Intent(Actions.NOTIFICATION_OPENED)
                        .putExtra(Actions.NOTIFICATION_INFO, info)
                        .putExtra(Actions.LAUNCH, backgrounded),
                PendingIntent.FLAG_UPDATE_CURRENT));
        builder.setDeleteIntent(PendingIntent.getBroadcast(
                context,
                0,
                new Intent(Actions.NOTIFICATION_DISMISSED)
                        .putExtra(Actions.NOTIFICATION_INFO, info),
                PendingIntent.FLAG_UPDATE_CURRENT));
        
        return builder.build();
    }
}
