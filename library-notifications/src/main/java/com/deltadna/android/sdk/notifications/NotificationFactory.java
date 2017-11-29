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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

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
 */
public class NotificationFactory {
    
    /**
     * Identifier for the default {@link NotificationChannel} used for
     * notifications.
     */
    public static final String DEFAULT_CHANNEL = "com.deltadna.default";
    
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
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(getChannel().getId());
        }
        
        return builder
                .setSmallIcon(message.icon)
                .setContentTitle(message.title)
                .setContentText(message.message)
                .setAutoCancel(true);
    }
    
    /**
     * Creates a {@link Notification} from a previously configured
     * {@link NotificationCompat.Builder} and a {@link NotificationInfo}
     * instance.
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
        
        builder.setContentIntent(PendingIntent.getBroadcast(
                context,
                0,
                new Intent(Actions.NOTIFICATION_OPENED_INTERNAL)
                        .setPackage(context.getPackageName())
                        .setClass(context, NotificationInteractionReceiver.class)
                        .putExtra(Actions.NOTIFICATION_INFO, info),
                PendingIntent.FLAG_UPDATE_CURRENT));
        builder.setDeleteIntent(PendingIntent.getBroadcast(
                context,
                0,
                new Intent(Actions.NOTIFICATION_DISMISSED_INTERNAL)
                        .setPackage(context.getPackageName())
                        .setClass(context, NotificationInteractionReceiver.class)
                        .putExtra(Actions.NOTIFICATION_INFO, info),
                PendingIntent.FLAG_UPDATE_CURRENT));
        
        return builder.build();
    }
    
    /**
     * Gets the {@link NotificationChannel} to be used for configuring the
     * push notification.
     * <p>
     * The {@link #DEFAULT_CHANNEL} is used as the default identifier.
     *
     * @return  notification channel to be used
     */
    @TargetApi(Build.VERSION_CODES.O)
    protected NotificationChannel getChannel() {
        final NotificationChannel channel = new NotificationChannel(
                DEFAULT_CHANNEL,
                context.getString(R.string.ddna_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        
        ((NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE))
                .createNotificationChannel(channel);
        
        return channel;
    }
}
