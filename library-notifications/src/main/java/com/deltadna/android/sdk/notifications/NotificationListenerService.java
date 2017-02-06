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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Locale;
import java.util.Map;

/**
 * {@link FirebaseMessagingService} which listens to incoming downstream
 * messages.
 * <p>
 * The default implementation posts a notification on the
 * {@link NotificationManager} with the id of the campaign, using 'title'
 * and 'alert' values from the downstream message as the notification's title
 * and message respectively. If the title has not been defined in the message
 * then the application's name will be used instead. Upon selection the
 * notification will open the launch {@code Intent} of your game. Default
 * behaviour can be customized, with more details further on.
 * <p>
 * The following entry will also need to be added to the manifest file:
 * <pre>{@code
 * <service
 *     android:name="com.deltadna.android.sdk.notifications.NotificationListenerService">
 *     
 *     <intent-filter>
 *         <action android:name="com.google.firebase.MESSAGING_EVENT"/>
 *     </intent-filter>
 * </service>
 * }</pre>
 * Behaviour can be customized by overriding
 * {@link #createNotification(NotificationInfo)} and/or
 * {@link #notify(long, Notification)} at runtime using your own subclass, or
 * by setting either of the following {@code meta-data} attributes inside the
 * {@code application} tag of your manifest file:
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
public class NotificationListenerService extends FirebaseMessagingService {
    
    protected static final String NOTIFICATION_TAG =
            "com.deltadna.android.sdk.notifications";
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + NotificationListenerService.class.getSimpleName();
    
    
    protected NotificationManager manager;
    protected Bundle metaData;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        manager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        metaData = MetaData.get(this);
    }
    
    @Override
    public void onMessageReceived(RemoteMessage message) {
        final String from = message.getFrom();
        final Map<String, String> data = message.getData();
        
        Log.d(  TAG, String.format(
                Locale.US,
                "Received message %s from %s",
                data,
                from));
        
        if (from == null) {
            Log.w(TAG, "Message sender is unknown");
        // TODO PTL-2693: do we still need it? can we still do it?
        //} else if (!from.equals(getString(metaData.getInt(MetaData.SENDER_ID)))) {
        //    Log.d(TAG, "Not handling message due to sender ID mismatch");
        } else if (data == null || data.isEmpty()) {
            Log.w(TAG, "Message data is null or empty");
        } else {
            final PushMessage pushMessage = new PushMessage(
                    this,
                    message.getFrom(),
                    message.getData());
            sendBroadcast(new Intent(Actions.MESSAGE_RECEIVED).putExtra(
                    Actions.PUSH_MESSAGE,
                    pushMessage));
            
            final int id = (int) pushMessage.id;
            final NotificationInfo info = new NotificationInfo(id, pushMessage);
            
            notify(id, createNotification(info).build());
            sendBroadcast(new Intent(Actions.NOTIFICATION_POSTED).putExtra(
                    Actions.NOTIFICATION_INFO,
                    info));
        }
    }
    
    /**
     * Creates a {@link android.support.v4.app.NotificationCompat.Builder}
     * whose built result will be posted on the {@link NotificationManager}.
     *
     * Implementations which call
     * {@link NotificationCompat.Builder#setContentIntent(PendingIntent)}
     * or
     * {@link NotificationCompat.Builder#setDeleteIntent(PendingIntent)}
     * on the {@link NotificationCompat.Builder} and thus override the default
     * behaviour should notify the SDK that the push notification has been
     * opened or dismissed respectively.
     *
     * @param info  the information for the notification to be posted
     *
     * @return      configured notification builder
     *
     * @see NotificationInteractionReceiver
     * @see EventReceiver
     */
    protected NotificationCompat.Builder createNotification(
            NotificationInfo info) {
        
        final NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(info.message.icon)
                        .setContentTitle(info.message.title)
                        .setContentText(info.message.message)
                        .setAutoCancel(true);
        
        // to make the behaviour consistent with iOS on Unity
        final boolean backgrounded = !Utils.inForeground(this);
        if (!backgrounded) {
            Log.d(TAG, "Notifying SDK of notification opening");
            DDNANotifications.recordNotificationOpened(
                    Utils.convert(info.message.data),
                    false);
        }
        
        builder.setContentIntent(PendingIntent.getBroadcast(
                this,
                0,
                new Intent(Actions.NOTIFICATION_OPENED)
                        .putExtra(Actions.NOTIFICATION_INFO, info)
                        .putExtra(Actions.LAUNCH, backgrounded),
                PendingIntent.FLAG_UPDATE_CURRENT));
        builder.setDeleteIntent(PendingIntent.getBroadcast(
                this,
                0,
                new Intent(Actions.NOTIFICATION_DISMISSED)
                        .putExtra(Actions.NOTIFICATION_INFO, info),
                PendingIntent.FLAG_UPDATE_CURRENT));
        
        return builder;
    }
    
    /**
     * Posts a {@link Notification} on the {@link NotificationManager}.
     *
     * @param id            the id
     * @param notification  the notification
     */
    protected void notify(long id, Notification notification) {
        manager.notify(NOTIFICATION_TAG, (int) id, notification);
    }
}
