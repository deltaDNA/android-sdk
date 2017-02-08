package com.deltadna.android.sdk.notifications.example;

import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.deltadna.android.sdk.notifications.NotificationFactory;
import com.deltadna.android.sdk.notifications.PushMessage;

/**
 * Example of a {@link NotificationFactory} which changes the style of the
 * notification after a push message is received.
 */
public class StyledNotificationFactory extends NotificationFactory {
    
    public StyledNotificationFactory(Context context) {
        super(context);
    }
    
    @Override
    public NotificationCompat.Builder configure(
            NotificationCompat.Builder builder,
            PushMessage message) {
        
        return super.configure(builder, message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message.message));
    }
}
