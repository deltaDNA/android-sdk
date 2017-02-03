# Change Log

## [4.3.0](https://github.com/deltaDNA/android-sdk/releases/tag/4.3.0) (YYYY-MM-DD)
Fixed notifications from campaigns overwriting other notifications.  

## [4.2.7](https://github.com/deltaDNA/android-sdk/releases/tag/4.2.7) (YYYY-MM-DD)
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
