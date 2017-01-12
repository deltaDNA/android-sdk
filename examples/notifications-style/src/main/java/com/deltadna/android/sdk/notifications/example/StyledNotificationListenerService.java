package com.deltadna.android.sdk.notifications.example;

import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.deltadna.android.sdk.notifications.NotificationListenerService;

/**
 * Example of a {@link NotificationListenerService} which changes the style
 * of the notification after a push message is received.
 */
public class StyledNotificationListenerService extends NotificationListenerService {
    
    @Override
    protected NotificationCompat.Builder createNotification(Bundle data) {
        return super.createNotification(data)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(data.getString("alert")));
    }
}
