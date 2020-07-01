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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Locale;
import java.util.Map;

/**
 * {@link FirebaseMessagingService} which listens to incoming downstream
 * messages and posts them on the UI.
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
 * The look of the notification can be customised by overriding
 * {@link #createFactory(Context)} and extending the {@link NotificationFactory}
 * to change the default behaviour.
 */
public class NotificationListenerService extends FirebaseMessagingService {
    
    protected static final String NOTIFICATION_TAG =
            "com.deltadna.android.sdk.notifications";
    protected static final IntentFilter RECEIVER_FILTER = new IntentFilter();
    static {
        RECEIVER_FILTER.addAction(Actions.NOTIFICATION_OPENED);
        RECEIVER_FILTER.addAction(Actions.NOTIFICATION_DISMISSED);
    }
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + NotificationListenerService.class.getSimpleName();
    
    protected Bundle metaData;
    protected NotificationManager manager;
    protected NotificationFactory factory;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        metaData = MetaData.get(this);
        manager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        factory = createFactory(this);
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
        } else if (!from.equals(getString(metaData.getInt(MetaData.SENDER_ID)))) {
            Log.d(TAG, "Not handling message due to sender ID mismatch");
        } else if (data == null || data.isEmpty()) {
            Log.w(TAG, "Message data is null or empty");
        } else {
            final PushMessage pushMessage = new PushMessage(
                    this,
                    message.getFrom(),
                    message.getData());
            sendBroadcast(Utils.wrapWithReceiver(
                    this,
                    new Intent(Actions.MESSAGE_RECEIVED)
                            .setPackage(getPackageName())
                            .putExtra(Actions.PUSH_MESSAGE, pushMessage)));
            
            final int id = (int) pushMessage.id;
            final NotificationInfo info = new NotificationInfo(id, pushMessage);

            final Notification notification = factory.create(
                    factory.configure(
                            new NotificationCompat.Builder(this),
                            pushMessage),
                    info);
            if (notification != null) {
                notify(id, notification);
                sendBroadcast(Utils.wrapWithReceiver(
                        this,
                        new Intent(Actions.NOTIFICATION_POSTED)
                                .setPackage(getPackageName())
                                .putExtra(Actions.NOTIFICATION_INFO, info)));
            }
        }
    }
    
    /**
     * Creates the notification factory to be used for creating notifications
     * when a push message is received.
     *
     * @param context   the context
     *
     * @return          notification factory
     */
    protected NotificationFactory createFactory(Context context) {
        return new NotificationFactory(context);
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
