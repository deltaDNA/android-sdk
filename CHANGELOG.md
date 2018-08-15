# Change Log

## [???](https://github.com/deltaDNA/android-sdk/releases/tag/???) (YYYY-MM-DD)
Added support for SmartAds reading session configuration.  
Updated Firebase Messaging for notifications.  

## [4.9.3](https://github.com/deltaDNA/android-sdk/releases/tag/4.9.3) (2018-08-07)
Public release of 4.9.  

## [4.9.2](https://github.com/deltaDNA/android-sdk/releases/tag/4.9.2) (2018-07-31)
Updated Engage cache to eject stale items.  

## [4.9.1](https://github.com/deltaDNA/android-sdk/releases/tag/4.9.1) (2018-07-25)
Fixed notifications compatibility with Unity.  

## [4.9.0](https://github.com/deltaDNA/android-sdk/releases/tag/4.9.0) (2018-06-26)
Added session configuration.  
Added event and decision point whitelisting.  
Added Event-Triggered Campaign support.  

## [4.8.1](https://github.com/deltaDNA/android-sdk/releases/tag/4.8.1) (2018-06-21)
Fixed event listeners not being called.  

## [4.8.0](https://github.com/deltaDNA/android-sdk/releases/tag/4.8.0) (2018-05-18)
Added API for forgetting the user and stopping tracking (GDPR).  
Added Image Message asset caching.  

## [4.7.0](https://github.com/deltaDNA/android-sdk/releases/tag/4.7.0) (2018-04-17)
Added EngageFactory to simplify Engage requests.  
Added copy constructor to Params.  
Fixed game and image message parameters to be non-null.  
Updated build and runtime dependencies.  

## [4.6.3](https://github.com/deltaDNA/android-sdk/releases/tag/4.6.3) (2018-02-06)
Updated Collect and Engage URLs to be forced to HTTPS.  

## [4.6.2](https://github.com/deltaDNA/android-sdk/releases/tag/4.6.2) (2017-12-01)
Added explicit event receiver registration if targeting API 26 or higher.  
Fixed push notification broadcast events if targeting API 26 or higher.  
Fixed push notifications not opening app if targeting API 26 or higher.  
Fixed push notification events to not be broadcast outside of the app.  

## [4.6.1](https://github.com/deltaDNA/android-sdk/releases/tag/4.6.1) (2017-11-23)
Fixed crash when notifications icon defined as drawable resource.  
Fixed possible leaked cursors.  

## [4.6.0](https://github.com/deltaDNA/android-sdk/releases/tag/4.6.0) (2017-10-23)
Improved support for Amazon platform.  
Updated build and runtime dependencies.  

## [4.5.4](https://github.com/deltaDNA/android-sdk/releases/tag/4.5.4) (2017-10-17)
Fixed push notification compatibility with Android 8.  
Updated target API to 26 (Android 8).  
Updated Android support library.  

## [4.5.3](https://github.com/deltaDNA/android-sdk/releases/tag/4.5.3) (2017-09-20)
Fixed incorrect clearing of events on unavailable storage.  
Fixed unable to send long values as virtual currency.  
Improved database handling when uploading events.  

## [4.5.2](https://github.com/deltaDNA/android-sdk/releases/tag/4.5.2) (2017-09-04)
Fixed background with no action dismissing image message.  

## [4.5.1](https://github.com/deltaDNA/android-sdk/releases/tag/4.5.1) (2017-08-10)
Removed reference to Unity method.  

## [4.5.0](https://github.com/deltaDNA/android-sdk/releases/tag/4.5.0) (2017-06-27)
Added image message action event.  
Fixed database cursor not being closed.  
Fixed event sending on notification interactions.  

## [4.3.3](https://github.com/deltaDNA/android-sdk/releases/tag/4.3.3) (2017-06-12)
Fixed writing to engage archive some devices.  

## [4.3.2](https://github.com/deltaDNA/android-sdk/releases/tag/4.3.2) (2017-05-17)
Fixed back button closing app during an image message.  

## [4.3.1](https://github.com/deltaDNA/android-sdk/releases/tag/4.3.1) (2017-05-03)
Fixed buttons with links not working in image messages.  

## [4.3.0](https://github.com/deltaDNA/android-sdk/releases/tag/4.3.0) (2017-03-13)
Updated notifications from Google Messaging to Firebase Messaging.  
Added broadcast receiver for listening to notification events.  
Added notification factory for customising notifications.  
Fixed notifications from campaigns overwriting other notifications.  
Fixed application icon not being picked up for notifications.  
Deprecated some manifest properties for customising notifications.  
Removed deprecated methods and classes.  

## [4.2.7](https://github.com/deltaDNA/android-sdk/releases/tag/4.2.7) (2017-01-16)
Fixed campaign and cohort ids not being picked up from a notification.  

## [4.2.6](https://github.com/deltaDNA/android-sdk/releases/tag/4.2.6) (2016-12-13)
Added ability to set the platform field during initialisation.  
Fixed session expiry handling when using multiple activities.  

## [4.2.5](https://github.com/deltaDNA/android-sdk/releases/tag/4.2.5) (2016-12-12)
Fixed engagement status code propagation consistency wrt other SDKs.  

## [4.2.4](https://github.com/deltaDNA/android-sdk/releases/tag/4.2.4) (2016-10-06)
Fixed slow initialisation.  

## [4.2.3](https://github.com/deltaDNA/android-sdk/releases/tag/4.2.3) (2016-09-14)
Fixed notification callbacks not triggered consistently on Unity.  

## [4.2.2](https://github.com/deltaDNA/android-sdk/releases/tag/4.2.2) (2016-09-13)
Fixed crash when notification received on Unity.  

## [4.2.1](https://github.com/deltaDNA/android-sdk/releases/tag/4.2.1) (2016-09-09)
Fixed handling of push notifications from other senders.  

## [4.2.0](https://github.com/deltaDNA/android-sdk/releases/tag/4.2.0) (2016-09-05)
Added helper in Product for converting currencies from a floating point representation.  

## [4.1.7](https://github.com/deltaDNA/android-sdk/releases/tag/4.1.7) (2016-08-18)
Fixed empty notification title in Unity.  

## [4.1.6](https://github.com/deltaDNA/android-sdk/releases/tag/4.1.6) (2016-08-09)
Fixed image messages not scaling when using cover resize mode.  

## [4.1.5](https://github.com/deltaDNA/android-sdk/releases/tag/4.1.5) (2016-05-27)
Updates to support new version of SmartAds.  

## [4.1.4](https://github.com/deltaDNA/android-sdk/releases/tag/4.1.4) (2016-05-24)
Fixed invalid events due to userLocale formatting error on some devices.  

## [4.1.3](https://github.com/deltaDNA/android-sdk/releases/tag/4.1.3) (2016-05-24)
Fixed network requests not executing on API16.  

## [4.1.2](https://github.com/deltaDNA/android-sdk/releases/tag/4.1.2) (2016-05-17)
Fixed event sending on push notifications to match Unity/iOS.  
Fixed missing push notification events in Unity.

## [4.1.1](https://github.com/deltaDNA/android-sdk/releases/tag/4.1.1) (2016-05-06)
Fixed activity restarting when opening notifications on Unity.  
Fixed crash when opening notification in-game on Unity.

## [4.1.0](https://github.com/deltaDNA/android-sdk/releases/tag/4.1.0) (2016-04-29)
Added automatic session refreshing.  
Added improved APIs for Engagements and Image Messaging.  
Added separate notifications project as an optional module.  
Fixed JavaDoc links to Android/Java classes.  
Fixed Image Message background action.  
Fixed requesting of WRITE_EXTERNAL_STORAGE permission on API 19+.

## [4.0.3](https://github.com/deltaDNA/android-sdk/releases/tag/4.0.3) (2016-03-29)
Added support for event de-duplication.

## [4.0.2](https://github.com/deltaDNA/android-sdk/releases/tag/4.0.2) (2016-03-25)
Fixed bulk events not being sent to correct endpoint.  
Fixed product currencies and items not being added correctly.  
Fixed manual event upload requests being stacked.  
Fixed missing fields from engagement requests.  
Fixed Image Messaging crashing on an absent image.  
Fixed Image Message obscuring activity underneath it.  
Updated Image Messaging API to match other platforms.

## [4.0.1](https://github.com/deltaDNA/android-sdk/releases/tag/4.0.1) (2016-03-18)
Fixed events not being sent correctly.

## [4.0.0](https://github.com/deltaDNA/android-sdk/releases/tag/4.0.0) (2016-03-16)
Initial version 4.0 release.
