package com.deltadna.android.sdk.notifications.example;

import android.support.v4.app.NotificationCompat;

import com.deltadna.android.sdk.notifications.NotificationInfo;
import com.deltadna.android.sdk.notifications.NotificationListenerService;

/**
 * Example of a {@link NotificationListenerService} which changes the style
 * of the notification after a push message is received.
 */
public class StyledNotificationListenerService extends NotificationListenerService {
    
    @Override
    protected NotificationCompat.Builder createNotification(
            NotificationInfo info) {
        
        return super.createNotification(info)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(info.message.message));
    }
}
