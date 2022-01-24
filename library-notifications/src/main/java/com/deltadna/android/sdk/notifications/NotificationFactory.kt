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
package com.deltadna.android.sdk.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

/**
 * Factory class responsible for filling details from a push message and
 * creating a notification to be posted on the UI.
 *
 *
 * The default implementation uses the 'title' and 'alert' fields from the push
 * message as the title and message respectively for the notification. If the
 * title has not been defined in the message then the application's name will
 * be used instead. Upon selection the notification will open the launch
 * `Intent` of your application.
 *
 *
 * The default behaviour can be customised by extending the class and overriding
 * [.configure]. The
 * [NotificationListenerService] will then need to be extended in order
 * to define the new factory to be used for creating notifications.
 */
open class NotificationFactory(protected val context: Context) {
    companion object {
        /**
         * Identifier for the default [NotificationChannel] used for
         * notifications.
         */
        const val DEFAULT_CHANNEL = "com.deltadna.default"
    }

    /**
     * Fills a [androidx.core.app.NotificationCompat.Builder]
     * with details from a [PushMessage].
     *
     * @param context   the context for the notification
     * @param message   the push message
     *
     * @return          configured notification builder
     */
    open fun configure(context: Context?, message: PushMessage): NotificationCompat.Builder? {
        var builder: NotificationCompat.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(context!!, getChannel().id)
        } else {
            NotificationCompat.Builder(context)
        }

        builder = builder
            .setSmallIcon(message.icon)
            .setContentTitle(message.title)
            .setContentText(message.message)
            .setAutoCancel(true)

        if (message.imageUrl != null) {
            builder.setLargeIcon(message.imageUrl)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(message.imageUrl).bigLargeIcon(null)
                )
        }
        return builder
    }

    /**
     * Creates a [Notification] from a previously configured
     * [NotificationCompat.Builder] and a [NotificationInfo]
     * instance.
     *
     * Implementations which call
     * [NotificationCompat.Builder.setContentIntent]
     * or
     * [NotificationCompat.Builder.setDeleteIntent]
     * on the [NotificationCompat.Builder] and thus override the default
     * behaviour should notify the SDK that the push notification has been
     * opened or dismissed respectively.
     *
     * @param builder   the configured notification builder
     * @param info      the notification info
     *
     * @return          notification to post on the UI, or `null` if a
     * notification shouldn't be posted
     *
     * @see EventReceiver
     */
    fun create(builder: NotificationCompat.Builder, info: NotificationInfo): Notification? {
        val intentFlags: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        builder.setContentIntent(createContentIntent(info, intentFlags))
        builder.setDeleteIntent(createDeleteIntent(info, intentFlags))
        return builder.build()
    }

    /**
     * Gets the [NotificationChannel] to be used for configuring the
     * push notification.
     *
     * The [.DEFAULT_CHANNEL] is used as the default identifier.
     *
     * @return  notification channel to be used
     */
    @RequiresApi(Build.VERSION_CODES.O)
    protected fun getChannel(): NotificationChannel {
        val channel = NotificationChannel(
            DEFAULT_CHANNEL,
            context.getString(R.string.ddna_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
        return channel
    }

    /**
     * Creates the intent which is used when a user opens the host app from a notification. In most
     * cases, this will be a set of two activities - our DeltaDNA tracking activity that records the
     * opened notification, and the host app's launch intent. Once the deltaDNA Intent completes, the
     * launch intent will be shown as part of Android's normal behaviour, and won't be blocked by the
     * trampolining prevention that blocks launching intents from Broadcast Receivers.
     */
    private fun createContentIntent(info: NotificationInfo, intentFlags: Int): PendingIntent {
        val notificationOpenedHandlerClass = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            NotificationOpenedHandlerAndroid23AndHigher::class.java
        } else {
            NotificationOpenedHandlerPreAndroid23::class.java
        }

        val notificationOpenedHandlerIntent = Intent(context, notificationOpenedHandlerClass)
            .setPackage(context.packageName)
            .setAction(Actions.NOTIFICATION_OPENED_INTERNAL)
            .putExtra(Actions.NOTIFICATION_INFO, info)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP) // Only ever have one notification opened tracking activity

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)

        if (launchIntent == null) {
            // If the app hasn't specified a launch intent, we just use our notification handler activity, to still enable notificationOpened tracking.
            return PendingIntent.getActivity(context, info.id, notificationOpenedHandlerIntent, intentFlags)
        } else {
            // If the app specifies a launch intent, we add it to the activity stack, so that once we've finished capturing the notificationOpened behaviour
            // the app will open as normal, without the need for a BroadcastReceiver trampoline which is no longer allowed in Android 12.

            launchIntent.setPackage(null) // This makes the app start as if it was launched externally, preventing duplicate activities from being created
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED

            val intents: Array<Intent> = arrayOf(launchIntent, notificationOpenedHandlerIntent)
            return PendingIntent.getActivities(context, info.id, intents, intentFlags)
        }
    }

    /**
     * Creates the intent which is used when a user dismisses our push notification without opening it. Now
     * that we've moved to using a custom activity to track notification opens, and we previously had no custom
     * behaviour on notification dismissed, we can instead directly signal our notifications receiver that the notification
     * was dismissed, instead of relaying via an intermediate broadcast receiver.
     */
    private fun createDeleteIntent(info: NotificationInfo, intentFlags: Int): PendingIntent {
        val intent = Intent(Actions.NOTIFICATION_DISMISSED)

        synchronized(DDNANotifications::class.java) {
            if (DDNANotifications.receiver != null) {
                intent.setClass(context, DDNANotifications.receiver!!)
            }
        }

        return PendingIntent.getBroadcast(
            context,
            info.id,
            intent,
            intentFlags
        )
    }

}