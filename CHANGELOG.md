# Change Log

## [4.13.1](https://github.com/deltaDNA/android-sdk/releases/tag/4.13.1)

### Fixed
- Time consuming calls on engage response errors no longer block the UI thread

## [4.13.0](https://github.com/deltaDNA/android-sdk/releases/tag/4.13.0)

### Added
- A EventActionEvaluateCompleteHandler that allow developer to execute a command after the EventAction is evaluated.

## [4.12.0](https://github.com/deltaDNA/android-sdk/releases/tag/4.12.0)

### Added
- Support for Rich Push Notifications.

### Fixed
- Support for latest versions of firebase cloud messaging added in `library-notifications`. 
  - Please follow the migration guide to ensure that you populate the new required values for this functionality.

## [4.11.4](https://github.com/deltaDNA/android-sdk/releases/tag/4.11.4)
### Added
- Support for automatically open Link action URLs on an Image Message.

### Fixed 
- Issues with producing duplicate events

## [4.11.3.2](https://github.com/deltaDNA/android-sdk/releases/tag/4.11.3.2) 
### Fixed 
- Issues with long-running tasks.

## [4.11.3.1](https://github.com/deltaDNA/android-sdk/releases/tag/4.11.3.1) 
### Fixed 
- SQLLite related crashes when upgrading internal database. 

## [4.11.3](https://github.com/deltaDNA/android-sdk/releases/tag/4.11.3) 
### Added 
- Improved support for event triggered campaigns.
- Automatic Session Configuration retry mechanism to better deal with network related failures.
- New `stopTrackingMe` method to stop sending analytics events on request

## [4.11.2](https://github.com/deltaDNA/android-sdk/releases/tag/4.11.2) 
### Fixed 
- Evaluating non-whitelisted events for Event-Triggered Campaigns no longer fails.

## [4.11.1](https://github.com/deltaDNA/android-sdk/releases/tag/4.11.1) 
### Added 
- Support for specifying default event-triggered action handlers.

### Fixed 
- Event-Triggered Campaigns will now correctly trigger off internal events.

## [4.11.0](https://github.com/deltaDNA/android-sdk/releases/tag/4.11.0)
### Added
- Support for cross promotion.
- Support for image message store action.
- Support for multiple Event-Triggered campaign actions from a single event. 

### Fixed
- Image messages not redrawing correctly on device re-orientation 

### Changed
- `ImageMessageResultListener` returns parameters as `JSONObject` types.
- `ImageMessageResultListener` callback methods are optional.

## [4.10.2](https://github.com/deltaDNA/android-sdk/releases/tag/4.10.2) (2018-11-26)
### Fixed
- Engage requests resulting in client error responses will no longer use the Engage cache.

## [4.10.1](https://github.com/deltaDNA/android-sdk/releases/tag/4.10.1) (2018-11-07)
### Fixed
- Missing fields to ddnaEventTriggeredAction event.

## [4.10.0](https://github.com/deltaDNA/android-sdk/releases/tag/4.10.0) (2018-08-22)
Added support for SmartAds reading session configuration.  
Added support for passing the GAID into forget me events.  
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
