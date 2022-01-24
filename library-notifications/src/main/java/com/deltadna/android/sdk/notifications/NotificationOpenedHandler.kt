package com.deltadna.android.sdk.notifications

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

internal open class NotificationOpenedHandler: Activity() {
    companion object {
        const val TAG = BuildConfig.LOG_TAG + " Notifications"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingNotification(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingNotification(intent)
        finish()
    }

    private fun handleIncomingNotification(intent: Intent) {
        Log.d(TAG, "Received intent: $intent")

        val notificationInfo = intent.getSerializableExtra(Actions.NOTIFICATION_INFO) as? NotificationInfo

        // This intent will be passed to whatever receiver (if any) is registered on DDNANotifications
        val broadcastIntent = Intent()
        broadcastIntent.action = Actions.NOTIFICATION_OPENED

        if (notificationInfo != null) {
            DDNANotifications.recordNotificationOpened(
                Utils.convert(notificationInfo.message.data),
                true // This will be true here as this activity only runs once the user has tapped on the notification, launching the app.
            )
            broadcastIntent.putExtra(Actions.NOTIFICATION_INFO, notificationInfo)
        } else {
            Log.w(TAG, "Notification info was missing when attempting to process an opened notification.")
        }

        // Pass on the info as a broadcast so that any user specified receiver will receive the relevant notification opened data.
        applicationContext.sendBroadcast(Utils.wrapWithReceiver(applicationContext, broadcastIntent))
    }
}