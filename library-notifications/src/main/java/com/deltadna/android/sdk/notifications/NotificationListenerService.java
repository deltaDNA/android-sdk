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
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import java.util.Locale;

/**
 * {@link GcmListenerService} which listens to incoming downstream messages
 * from GCM.
 * <p>
 * The default implementation posts a notification on the
 * {@link NotificationManager} with id {@link #NOTIFICATION_ID}, using 'title'
 * and 'alert' values from the downstream message as the notification's title
 * and message. If the title has not been defined in the message then the
 * application's name will be used instead. Upon selection the notification
 * will open the launch {@code Intent} of your game. Default behaviour can be
 * customized, with more details further on.
 * <p>
 * The following entry will also need to be added to the manifest file:
 * <pre><code>
 * {@literal<}service
 *     android:name="com.deltadna.android.sdk.notifications.NotificationListenerService"
 *     android:exported="false"{@literal>}
 *     
 *     {@literal<}intent-filter{@literal>}
 *         {@literal<}action android:name="com.google.android.c2dm.intent.RECEIVE"/{@literal>}
 *     {@literal<}/intent-filter{@literal>}
 * {@literal<}/service{@literal>}
 * </code></pre>
 * <p>
 * Behaviour can be customized by overriding {@link #createNotification(Bundle)}
 * and/or {@link #notify(Notification)} at runtime using your own subclass, or
 * by setting either of the following {@code meta-data} attributes inside the
 * {@code application} tag of your manifest file:
 * <pre><code>
 * {@literal<}meta-data
 *     android:name="ddna_notification_title"
 *     android:resource="@string/your_title_resource"/{@literal>}
 * 
 * {@literal<}meta-data
 *     android:name="ddna_notification_title"
 *     android:value="your-literal-title"/{@literal>}
 * 
 * {@literal<}meta-data
 *     android:name="ddna_notification_icon"
 *     android:value="your_icon_resource_name"/{@literal>}
 * 
 * {@literal<}meta-data
 *     android:name="ddna_start_launch_intent"
 *     android:value="false"/{@literal>}
 * </code></pre>
 */
public class NotificationListenerService extends GcmListenerService {

    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + NotificationListenerService.class.getSimpleName();
    private static final String PLATFORM_TITLE = "title";
    private static final String PLATFORM_ALERT = "alert";
    
    /**
     * Default id used by the service for posting notifications on the
     * {@link NotificationManager}.
     */
    protected static final int NOTIFICATION_ID = 0;
    
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
    public void onMessageReceived(String from, Bundle data) {
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
            notify(createNotification(data).build());
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
     * @param data  the data from the message
     *
     * @return      configured notification builder
     *
     * @see NotificationInteractionReceiver
     */
    protected NotificationCompat.Builder createNotification(Bundle data) {
        final String title = getTitle(data);
        final String alert;
        if (data.containsKey(PLATFORM_ALERT)) {
            alert = data.getString(PLATFORM_ALERT);
        } else {
            Log.w(TAG, "Missing 'alert' key in message");
            alert = "Missing 'alert' key";
        }
        
        final NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(getIcon())
                        .setContentTitle(title)
                        .setContentText(alert)
                        .setAutoCancel(true);
        
        // to make the behaviour consistent with iOS on Unity
        final boolean backgrounded = !Utils.inForeground(this);
        if (!backgrounded) {
            Log.d(TAG, "Notifying SDK of notification opening");
            DDNANotifications.recordNotificationOpened(data, false);
        }
        
        builder.setContentIntent(PendingIntent.getBroadcast(
                this,
                0,
                new Intent(Actions.NOTIFICATION_OPENED)
                        .putExtra(DDNANotifications.EXTRA_PAYLOAD, data)
                        .putExtra(DDNANotifications.EXTRA_LAUNCH, backgrounded),
                PendingIntent.FLAG_ONE_SHOT));
        builder.setDeleteIntent(PendingIntent.getBroadcast(
                this,
                0,
                new Intent(Actions.NOTIFICATION_DISMISSED)
                        .putExtra(DDNANotifications.EXTRA_PAYLOAD, data),
                PendingIntent.FLAG_ONE_SHOT));
        
        return builder;
    }
    
    /**
     * Posts a {@link Notification} on the {@link NotificationManager}.
     *
     * @param notification the notification
     */
    protected void notify(Notification notification) {
        manager.notify(NOTIFICATION_ID, notification);
    }
    
    private String getTitle(Bundle data) {
        if (data.containsKey(PLATFORM_TITLE)) {
            return data.getString(PLATFORM_TITLE);
        } else if (metaData.containsKey(MetaData.NOTIFICATION_TITLE)) {
            final Object value = metaData.get(MetaData.NOTIFICATION_TITLE);
            if (value instanceof String) {
                return (String) value;
            } else if (value instanceof Integer) {
                try {
                    return getString((Integer) value);
                } catch (Resources.NotFoundException e) {
                    throw new RuntimeException(
                            "Failed to find string resource for "
                                    + MetaData.NOTIFICATION_TITLE,
                            e);
                }
            } else {
                throw new RuntimeException(String.format(
                        Locale.US,
                        "Found %s for %s, only string or string resource allowed",
                        value,
                        MetaData.NOTIFICATION_TITLE));
            }
        } else {
            return String.valueOf(getPackageManager().getApplicationLabel(
                    getApplicationInfo()));
        }
    }
    
    @DrawableRes
    private int getIcon() {
        if (metaData.containsKey(MetaData.NOTIFICATION_ICON)) {
            try {
                final int value = getResources().getIdentifier(
                        metaData.getString(MetaData.NOTIFICATION_ICON),
                        "drawable",
                        getPackageName());
                
                if (value == 0) {
                    throw new Resources.NotFoundException();
                } else {
                    return value;
                }
            } catch (Resources.NotFoundException e) {
                throw new RuntimeException(
                        "Failed to find drawable resource for "
                                + MetaData.NOTIFICATION_ICON,
                        e);
            }
        } else {
            return R.drawable.ddna_ic_stat_logo;
        }
    }
}
