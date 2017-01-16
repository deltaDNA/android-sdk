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
 * [Notification](#notification)
 * [Token retrieval](#token-retrieval)
 * [Notification style](#notification-style)
  * [Unity](#unity)
 * [ProGuard](#proguard)
* [FAQs](#faqs)
* [Changelog](#changelog)
* [License](#license)

## Overview
This is an add-on module for the deltaDNA Android SDK which allows for easy integration of push notifications into a project.

When sending a push notification to clients the implementation will show the message of the notification from the `Alert` field in the Platform and the application's name as the title, unless a value under the `title` key has been added to the push notification's payload. Once the notification is tapped by the user

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
compile 'com.deltadna.android:deltadna-sdk:4.2.7'
compile 'com.deltadna.android:deltadna-sdk-notifications:4.2.7'
```

## Integration
Once you have the SDK and the Notifications addon in your project you will need to add the following two permissions inside the `manifest` section of your `AndroidManifest.xml` file
```xml
<permission
    android:name="your.package.name.permission.C2D_MESSAGE"
    android:protectionLevel="signature"/>
<uses-permission
    android:name="your.package.name.permission.C2D_MESSAGE"/>
```
where `your.package.name` needs to be replaced with the package name of your application.

Next step is to add the following definition inside of the `application` section
```xml
<receiver
    android:name="com.google.android.gms.gcm.GcmReceiver"
    android:exported="true"
    android:permission="com.google.android.c2dm.permission.SEND">
    
    <intent-filter>
        <action android:name="com.google.android.c2dm.intent.RECEIVE"/>
        <category android:name="your.package.name"/>
    </intent-filter>
</receiver>
```
where `your.package.name` needs to be replaced with the package name of your application.

Now you will need to add a [`NotificationListenerService`](src/main/java/com/deltadna/android/sdk/notifications/NotificationListenerService.java) at the same element level as for the previous step
```xml
<service
    android:name="com.deltadna.android.sdk.notifications.NotificationListenerService"
    android:exported="false">
    
    <intent-filter>
        <action android:name="com.google.android.c2dm.intent.RECEIVE"/>
    </intent-filter>
</service>
```
While the above definition could have been provided by the library this service is quite important as it takes care of showing a notification on the UI when a push message is sent from the Platform, as such we are allowing for customization on how it behaves.

The last step is to add your Sender ID (also known as the Project Number from the Google Developer Console) into the application's string resources, and add a reference to it in the manifest file inside the `application` section
```xml
<meta-data
    android:name="ddna_sender_id"
    android:resource="@string/sender_id"/>
```

You can always refer to the example implementation [here](../examples/notifications).

## Registering
In order to register the client for push notifications with the Platform the `register()` method needs to be called from [`DDNANotifications`](src/main/java/com/deltadna/android/sdk/notifications/DDNANotifications.java). This will initiate a request for retrieving a registration token from GCM, and send it to the deltaDNA servers. Please note that the latter will only take place when `DDNA.startSdk()` is called, as such if the registration token is retrieved while the application is running the new token will be sent to the servers when the application will start next time and thus sending notifications to this client will not be available until this takes place.

A good time to call `register()` would be, for example, when the user enables notifications for the application in the settings or when a previous attempt to retrieve the token fails (more details on this can be found [here](#token-retrieval)).

It is also possible to unregister the client from push notifications by calling `unregister()` from `DDNANotifications`.

## Advanced
### Notification
By default the `NotificationListenerService` will search the payload of the pushed message for a value under the `title` key. If not found, then the application's name will be used instead for the title of the notification.

The easiest way to customise this behavior is to set the `ddna_notification_title` `meta-data` property in your manifest file inside of the `application` section
```xml
<meta-data
    android:name="ddna_notification_title"
    android:resource="@string/notification_title"/>
```

In a similar fashion the icon which gets displayed can be customized by setting the `ddna_notification_icon` property
```xml
<meta-data
    android:name="ddna_notification_icon"
    android:value="ic_stat_logo"/>
```
It is worth noting that using a resource for the drawable is not supported at the moment due to a limitation in Android, so in the above example `ic_stat_logo` would actually be resolved by the implementation at runtime as `@drawable/ic_stat_logo`. If not set then a default icon will be used by the library.

Finally, by default the notification will start the activity defined as the application's launch intent. This behaviour can be disabled with
```xml
<meta-data
    android:name="ddna_start_launch_intent"
    android:value="false"/>
```

If the properties of the notification need to be changed in a more dynamic way at runtime then the `NotificationListenerService` can be extended and either of the `createNotification` or `notify` method implementations overridden. More details can be found [here](#notification-style).

### Token retrieval
While the retrieval of the GCM registration token is done by the library under the hood, it may be useful to know when this succeeds or fails. For this reason the library will send a broadcast over the [`LocalBroadcastManager`](http://developer.android.com/reference/android/support/v4/content/LocalBroadcastManager.html) with the action `DDNANotifications.ACTION_TOKEN_RETRIEVAL_SUCCESSFUL` and the token will be contained in the `Intent` under the `DDNANotifications.EXTRA_REGISTRATION_TOKEN` `String` extra.

Likewise, if the retrieval fails then a broadcast with the `DDNANotifications.ACTION_TOKEN_RETRIEVAL_FAILED` action will be sent. The reason for the failure can be found under the `DDNANotifications.EXTRA_FAILURE_REASON` `Serializable` extra, which will be a `Throwable` type.

We provide an [`IntentFilter`](http://developer.android.com/reference/android/content/IntentFilter.html) which is set to listen to both of the above actions in `DDNANotifications.FILTER_TOKEN_RETRIEVAL` for ease of use.

An example implementation of a [`BroadcastReceiver`](http://developer.android.com/reference/android/content/BroadcastReceiver.html) can be found [here](../examples/notifications/src/main/java/com/deltadna/android/sdk/notifications/example/ExampleReceiver.java).

### Notification style
If you would like to change the style of the notification, for example to use expandable text, the [`NotificationListenerService`](src/main/java/com/deltadna/android/sdk/notifications/NotificationListenerService.java) can be extended in order to modify the default behaviour.

An example can be found [here](examples/notifications-style/src/main/java/com/deltadna/android/sdk/notifications/example/StyledNotificationListenerService.java). You will also need to change the `service` definition in the manifest file to point to the new class.

#### Unity
Changing the style on Unity is a bit more involved, but the steps below describe how to achieve this;
1.  You will need to checkout the [android-sdk](https://github.com/deltaDNA/android-sdk) project and open it in Android studio. Make sure that you've got all the neccessary dependencies downloaded in order to be able to build the project.
2.  Checkout the version of the project which you need in order to match the version used in the deltaDNA Unity SDK. You can find this out by navigating under `Assets/DeltaDNA/Plugins/Android` and finding the version of the `deltadna-sdk-notifications-*.aar` file. For example, if the file in the directory was named `deltadna-sdk-notifications-4.2.3.aar` you would run `git checkout 4.2.3` in the `android-sdk` project.
3.  Now you can make changes to the [`NotificationListenerService`](src/main/java/com/deltadna/android/sdk/notifications/NotificationListenerService.java) class, either directly or by creating a new class extending from it and overriding the appropriate method.
4.  After you have made the changes you can build the SDK by running `./gradlew clean build check` from the root directory of the project. Once successfully built the new ARR can be copied from `library-notifications/build/outputs/aar` (make sure to use the release version) to `Assets/DeltaDNA/Plugins/Android` in order to replace the stock AAR. If you have made the changes in a new class then you will also need to change the `service` entry in the manifest file for your Unity project to use your new class instead.

### ProGuard
There is no need to add additional directives in your ProGuard configuration if you are setting `minifyEnabled true` for your application as the library provides its own configuration file which gets included by the Android build tools during the build process.

## FAQs
1.  My project has a dependency on a newer version of Google Play Services, can I use a different version of GCM than what is documented?
    
    Yes, by excluding the GCM module from the Notifications dependency and grabbing it separately.
    ```groovy
    compile('com.deltadna.android:deltadna-sdk-notifications:VERSION') {
        exclude module: 'play-services-gcm'
    }
    compile 'com.google.android.gms:play-services-gcm:8.4.0'
    ```
    We have verified so far that versions 8, 9, and 10 can be used instead.

    If using Unity then you will need to replace the Play Service AARs in the `Assets/DeltaDNA/Plugins/Android` folder with newer versions from `<android-sdk-dir>/extras/google/m2repository`. Please note that newer versions of Play Services may use different dependencies, as such you may need to start with the `play-services-gcm` AAR and look at the POM file to work out which dependencies will also need to be added into the directory (you will also need to do the same for any transitive dependencies). `support-annotations` should not be removed as it is required by the deltaDNA notifications library.
