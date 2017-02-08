package com.deltadna.android.sdk.notifications.example;

import android.content.Context;

import com.deltadna.android.sdk.notifications.NotificationFactory;
import com.deltadna.android.sdk.notifications.NotificationListenerService;

/**
 * Example of a {@link NotificationListenerService} which changes the style of
 * the notification after a push message is received.
 */
public class StyledNotificationListenerService extends NotificationListenerService {
    
    @Override
    protected NotificationFactory createFactory(Context context) {
        return new StyledNotificationFactory(context);
    }
}
