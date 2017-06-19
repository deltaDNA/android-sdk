![deltaDNA logo](https://deltadna.com/wp-content/uploads/2015/06/deltadna_www@1x.png)

# deltaDNA Android SDK Notifications
[![Build Status](https://travis-ci.org/deltaDNA/android-sdk.svg)](https://travis-ci.org/deltaDNA/android-sdk)
[![codecov.io](https://codecov.io/github/deltaDNA/android-sdk/coverage.svg)](https://codecov.io/github/deltaDNA/android-sdk)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/b5546fd90d3b4b2182961602da6086d8)](https://www.codacy.com/app/deltaDNA/android-sdk)
[![Apache2 licensed](https://img.shields.io/badge/license-Apache-blue.svg)](./LICENSE)
[![Download](https://api.bintray.com/packages/deltadna/android/deltadna-sdk/images/download.svg)](https://bintray.com/deltadna/android/deltadna-sdk/_latestVersion)

## Contents
* [Overview](#overview)
* [Adding to a project](#adding-to-a-project)
* [Integration](#integration)
* [Registering](#registering)
* [Advanced](#advanced)
 * [Styling and behaviour](#styling-and-behaviour)
  * [Unity](#unity)
 * [Events](#events)
 * [ProGuard](#proguard)
* [FAQs](#faqs)
* [Changelog](#changelog)
* [License](#license)

## Overview
This is an add-on module for the deltaDNA Android SDK which allows for easy integration of push notifications into a project.

When sending a push notification to clients the implementation will show the message of the notification from the `alert` field in the Platform and the application's name as the title, unless a value under the `title` key has been added to the push notification's payload. When the notification is tapped by the user the module sends the appropriate events through the SDK.

More details on integration and customization can be found further on in this document.

## Adding to a project
### Gradle
In your top-level build script
```groovy
allprojects {
    repositories {
        maven { url 'http://deltadna.bintray.com/android' }
        // repositories for your other dependencies...
    }
}
```
In your app's build script
```groovy
compile 'com.deltadna.android:deltadna-sdk:4.5.0-SNAPSHOT'
compile 'com.deltadna.android:deltadna-sdk-notifications:4.5.0-SNAPSHOT'
```

## Integration
Once you have the SDK and the Notifications addon in your project you will need to add a [`NotificationListenerService`](src/main/java/com/deltadna/android/sdk/notifications/NotificationListenerService.java) definition inside of the `application` section
```xml
<service
    android:name="com.deltadna.android.sdk.notifications.NotificationListenerService">
    
    <intent-filter>
        <action android:name="com.google.android.c2dm.intent.RECEIVE"/>
    </intent-filter>
</service>
```
While the above definition could have been provided by the library this service is quite important as it takes care of showing a notification on the UI when a push message is sent from the Platform, as such we are allowing for customization on how it behaves.

The last step is to add your Application and Sender IDs from the Firebase Console into the manifest file. If your application has been setup using the Google Developer Console then you can easily migrate the project to Firebase by following the instructions [here](https://firebase.google.com/support/guides/google-android#migrate_your_console_project).
```xml
<application ...>
    ...
    
    <meta-data
        android:name="ddna_application_id"
        android:resource="@string/application_id"/>
    <meta-data
        android:name="ddna_sender_id"
        android:resource="@string/sender_id"/>
</application>
```

You can always refer to the example implementation [here](../examples/notifications).

## Registering
In order to register the client for push notifications with the Platform the `register()` method needs to be called from [`DDNANotifications`](src/main/java/com/deltadna/android/sdk/notifications/DDNANotifications.java). This will initiate a request for retrieving a registration token from GCM, and send it to the deltaDNA servers.

A good time to call `register()` would be, for example, when the user enables notifications for the application in the settings or when a previous attempt to retrieve the token fails.

It is possible to unregister the client from push notifications by calling `unregister()` from `DDNANotifications`. If you wish to register later on then `register()` should be called.

## Advanced
### Styling and behaviour
If you would like to change the style of the notification, for example to use expandable text, the [`NotificationListenerService`](src/main/java/com/deltadna/android/sdk/notifications/NotificationListenerService.java) can be extended in order to modify the default behaviour.

An example can be found [here](../examples/notifications-style/src/main/java/com/deltadna/android/sdk/notifications/example/StyledNotificationListenerService.java). You will also need to change the `service` definition in the manifest file to point to the new class.

#### Unity
Changing the style on Unity is a bit more involved, but the steps below describe how to achieve this;
1.  You will need to checkout the [android-sdk](https://github.com/deltaDNA/android-sdk) project and open it in Android studio. Make sure that you've got all the necessary dependencies downloaded in order to be able to build the project.
2.  Checkout the version of the project which you need in order to match the version used in the deltaDNA Unity SDK. You can find this out by navigating under `Assets/DeltaDNA/Plugins/Android` and finding the version of the `deltadna-sdk-notifications-*.aar` file. For example, if the file in the directory was named `deltadna-sdk-notifications-4.2.3.aar` you would run `git checkout 4.2.3` in the `android-sdk` project.
3.  Now you can make changes to the [`NotificationListenerService`](src/main/java/com/deltadna/android/sdk/notifications/NotificationListenerService.java) class, either directly or by creating a new class extending from it and overriding the appropriate method.
4.  After you have made the changes you can build the SDK by running `./gradlew clean build check` from the root directory of the project. Once successfully built the new ARR can be copied from `library-notifications/build/outputs/aar` (make sure to use the release version) to `Assets/DeltaDNA/Plugins/Android` in order to replace the stock AAR. If you have made the changes in a new class then you will also need to change the *Listener Service* entry in the notifications configuration UI for your Unity project to use your new class instead.

### Events
The module sends a number of events related to registering for push notifications, posting them on the UI, and listening for user interactions on them. You can listen to these events by extending [`EventReceiver`](src/main/java/com/deltadna/android/sdk/notifications/EventReceiver.java) and overriding the required methods.

You will need to register your receiver in the manifest file of your application, for example:
```xml
<receiver
    android:name="your.package.name.YourClassName"
    android:exported="false">
    
    <intent-filter>
        <action android:name="com.deltadna.android.sdk.notifications.REGISTERED"/>
        <action android:name="com.deltadna.android.sdk.notifications.REGISTRATION_FAILED"/>
        <action android:name="com.deltadna.android.sdk.notifications.MESSAGE_RECEIVED"/>
        <action android:name="com.deltadna.android.sdk.notifications.NOTIFICATION_POSTED"/>
        <action android:name="com.deltadna.android.sdk.notifications.NOTIFICATION_OPENED"/>
        <action android:name="com.deltadna.android.sdk.notifications.NOTIFICATION_DISMISSED"/>
    </intent-filter>
</receiver>
```

An example implementation of an [`EventReceiver`](src/main/java/com/deltadna/android/sdk/notifications/EventReceiver.java) can be found [here](../examples/notifications/src/main/java/com/deltadna/android/sdk/notifications/example/ExampleReceiver.java).

### ProGuard
There is no need to add additional directives in your ProGuard configuration if you are setting `minifyEnabled true` for your application as the library provides its own configuration file which gets included by the Android build tools during the build process.
