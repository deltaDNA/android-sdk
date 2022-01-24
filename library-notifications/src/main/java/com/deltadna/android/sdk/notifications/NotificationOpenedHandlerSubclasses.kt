package com.deltadna.android.sdk.notifications

/*
Two subclasses of the notification handler are required as different Android manifest entries
are needed on the different Android versions, to ensure the activity behaves as expected once
the application resumes (one needs the taskAffinity entry and the other does not).
 */
internal class NotificationOpenedHandlerPreAndroid23: NotificationOpenedHandler()
internal class NotificationOpenedHandlerAndroid23AndHigher: NotificationOpenedHandler()