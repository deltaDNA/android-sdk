![deltaDNA logo](https://deltadna.com/wp-content/uploads/2015/06/deltadna_www@1x.png)

# deltaDNA Android SDK
[![Build Status](https://travis-ci.org/deltaDNA/android-sdk.svg)](https://travis-ci.org/deltaDNA/android-sdk)
[![codecov.io](https://codecov.io/github/deltaDNA/android-sdk/coverage.svg)](https://codecov.io/github/deltaDNA/android-sdk)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/b5546fd90d3b4b2182961602da6086d8)](https://www.codacy.com/app/deltaDNA/android-sdk)
[![Apache2 licensed](https://img.shields.io/badge/license-Apache-blue.svg)](./LICENSE)
[![Download](https://api.bintray.com/packages/deltadna/android/deltadna-sdk/images/download.svg)](https://bintray.com/deltadna/android/deltadna-sdk/_latestVersion)

## Contents
* [Overview](#overview)
* [Adding to a project](#adding-to-a-project)
* [Initialising](#initialising)
 * [Amazon](#amazon)
* [Starting and stopping](#starting-and-stopping)
* [Recording events](#recording-events)
 * [Simple event](#simple-event)
 * [Complex event](#complex-event)
 * [Transactions](#transactions)
 * [Event triggers](#event-triggers)
* [Engage](#engage)
 * [Image Messaging](#image-messaging)
* [Forgetting a user](#forgetting-a-user-(gdpr))
* [Push notifications](#push-notifications)
* [Settings](#settings)
* [ProGuard](#proguard)
* [Changelog](#changelog)
* [Migrations](#migrations)
* [License](#license)

## Overview
The deltaDNA SDK allows your Android games to record in-game events and upload player actions. It contains event caching, numerous helper methods, and some automated behaviours to help simplify your integration.

## Adding to a project
The deltaDNA SDK can be used in Android projects using minimum SDK version 15 and newer (Android 4.0.3+).

### Gradle
In your top-level build script:
```groovy
allprojects {
    repositories {
        maven { url 'http://deltadna.bintray.com/android' }
    }
}
```
In your app's build script:
```groovy
dependencies {
    implementation 'com.deltadna.android:deltadna-sdk:4.9.1'
}
```
The Java source and target compatibility needs to be set to 1.8 in you app's build script:
```groovy
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```

## Initialising
The SDK needs to be initialised with the following parameters in an `Application` subclass:
* `Application` instance
* `environmentKey`, a unique 32 character string assigned to your application. You will be assigned separate application keys for development and production builds of your game. You will need to change the environment key that you initialise the SDK with as you move from development and testing to production.
* `collectUrl`, this is the address of the server that will be collecting your events.
* `engageUrl`, this is the address of the server that will provide real-time A/B Testing and Targeting.
```java
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        DDNA.initialise(new DDNA.Configuration(
                this,
                "environmentKey",
                "collectUrl",
                "engageUrl"));
    }
}
```

You will need to register your `Application` subclass in the manifest file
```xml
<application
    android:name=".MyApplication"
    ...>
</application>
```

After the `initialise()` call the SDK will be available throughout the entire lifecycle of your application by calling `DDNA.instance()`.

You may also set optional attributes on the `Configuration`, such as the client version, or user id, amongst other options.

### Amazon
When building an APK to be distributed on the Amazon Appstore then the platform needs to be changed to the `ClientInfo.Platform.AMAZON` enum during initialisation
```java
DDNA.initialise(new DDNA.Configuration(
        this,
        "environmentKey",
        "collectUrl",
        "engageUrl")
        .platform(ClientInfo.PLATFORM_AMAZON));
```

## Starting and stopping
Inside of your `Activity` class you will need to start the SDK with `DDNA.instance().startSdk()` from the `onCreate(Bundle)` method, and likewise stop the SDK with `DDNA.instance().stopSdk()` from the `onDestroy()` method.
```java
public class MyActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DDNA.instance().startSdk();
    }

    @Override
    public void onDestroy() {
        DDNA.instance().stopSdk();

        super.onDestroy();
    }
}
```
This is the minimum amount of code needed to initialise the SDK and start sending events. It will automatically send the *newPlayer* event the first time the SDK is run, and the *gameStarted* and *clientDevice* events each time the game runs.

## Recording events
### Simple event
By using one of the standard event schemas we can record an event such as
```java
DDNA.instance().recordEvent(new Event("options")
        .putParam("option", "Music")
        .putParam("action", "Disabled"));
```
Which would be uploaded with the following JSON
```json
{
    "eventName": "options",
    "userID": "a2e92bdd-f59d-498f-9385-2ae6ada432e3",
    "sessionID": "0bc56224-8939-4639-b5ba-197f84dad4f4",
    "eventTimestamp":"2014-07-04 11:09:42.491",
    "eventParams": {
        "platform": "ANDROID",
        "sdkVersion": "Android SDK v4.0",
        "option": "Music",
        "action": "Disabled"
    }
}
```

### Complex event
If you create more complicated events which get reused throughout of your game then instead of building up the event each time you can subclass from `Event` and add your parameters into the constructor
```java
public class MissionStartedEvent extends Event {

    public MissionStartedEvent(
            String name,
            String id,
            boolean isTutorial,
            String difficulty) {

        super("missionStarted")

        putParam("missionName", name)
        putParam("missionID", id)
        putParam("isTutorial", isTutorial)
        putParam("missionDifficulty", difficulty);
    }
}
```
And you will be able to record events by creating a new instance and passing it to `recordEvent(Event)`
```java
DDNA.instance().recordEvent(new MissionStartedEvent(
        "Mission01",
        "M001",
        false,
        "EASY"));
```

### Transactions
A transaction is a complex event which introduces nesting, arrays, and some special objects that you will encounter when the player buys, trades, wins, exchanges currency and items with the game or other players. To help with this we provide `Transaction`, which is an `Event` with additional properties
```java
recordEvent(new Transaction(
        "IAP - Large Treasure Chest",
        "PURCHASE",
        new Product()
                .addItem("Golden Battle Axe", "Weapon", 1)
                .addItem("Mighty Flaming Sword of the First Age", "Legendary Weapon", 1)
                .addItem("Jewel Encrusted Shield", "Armour", 1)
                .addVirtualCurrency("Gold", "PREMIUM", 100),
        new Product().setRealCurrency("USD", 499))
        .setId("47891208312996456524019-178.149.115.237:51787")
        .setProductId("4019"));
```
It is also worth noting that the currency value is always sent as an integer in the minor currency unit and with the ISO-4217 3 character currency code.

This event may be more complex, but the structure is logical, flexible, and provides a mechanism for players spending or receiving any combination of currencies and items.

### Event triggers
All `recordEvent` methods return an `EventAction` instance on which `EventActionHandler`s can be registered through the `add` method, for handling triggers which match the conditions setup on the Platform for Event-Triggered Campaigns. Once all the handlers have been registered `run()` needs to be called in order for the event triggers to be evaluated and for a matching handler to be run. This happens on the client without any network use and as such it is instantaneous.
```java
recordEvent(new Event("missionStarted").putParam("missionLevel", 1))
        .add(new EventActionHandler.GameParametersHandler(gameParameters -> {
            // do something with the game parameters
        }))
        .add(new EventActionHandler.ImageMessageHandler(imageMessage -> {
            // the image message is already prepared so it will show instantly
            imageMessage.show(MyActivity.this, MY_REQUEST_CODE);
        }))
        .run();
``` 

## Engage
An Engage request can be performed by calling `requestEngagement(Engagement, EngageListener)`, providing your `Engagement` and a an `EngageListener` for listening to the completion or error.
```java
requestEngagement(
        new Engagement("outOfCredits")
                .putParam("userLevel", 4)
                .putParam("userXP", 1000)
                .putParam("missionName", "Diso Volante"),
        new OutOfCreditsListener());
```
The `Engagement` object which was sent will be returned in the listener's `onCompleted(Engagement)` callback method, at which point it has been populated with data from the platform ready to be retrieved by calling `getJson()` on the `Engagement`.
```java
class OutOfCreditsListener implements EngageListener<Engagement> {
    
    public void onCompleted(Engagement engagement) {
        // do something with the result
        if (engagement.isSuccessful()) {
            // for example with parameters
            JSONObject parameters = engagement.getJson()
        }
    }
    
    public void onError(Throwable t) {
        // act on error
    }
}
```
If there was an error processing your Engage request at the server then the details will be available in the `Engagement` by calling `getError()`. Any non-server errors, such as due to an Internet connection not being available, will be propagated into the `onError(Throwable)` callback method. In this case `onCompleted(Engagement)` will never be called.

### Image Messaging
An Image Messaging request is performed in a similar way to an Engage request with
an `ImageMessage` instance being built up from the returned `Engagement` in the
`onCompleted(Engagement)` callback method. Since the decision point may not have
been set-up to show an Image Message the returned value needs to be null checked.
```java
DDNA.instance().getEngageFactory().requestImageMessage(
        "missionDifficulty",
        new EngageFactory.Callback<ImageMessage>() {
            @Override
            public void onCompleted(@Nullable ImageMessage action) {
                if (action != null) {
                    action.prepare(MyPrepareListener());
                }
            }
        });
```
An alternative way of requesting an Image Message is by using the `DDNA` instance
directly instead of the `EngageFactory`.
```java
DDNA.instance().requestEngagement(
        new Engagement("missionDifficulty"),
        new EngageListener<Engagement>() {
            @Override
            public void onComplete(Engagement engagement) {
                ImageMessage imageMessage = ImageMessage.create(engagement);
                if (imageMessage != null) {
                    imageMessage.prepare(MyPrepareListener());
                }
            }
            
            @Override
            public void onError(Throwable t) {
                // act on error
            }
        });
```
When the `onPrepared(ImageMessage)` of your `ImageMessage.PrepareListener` listener gets invoked you may show the Image Message by calling `show(Activity, int)` on the `ImageMessage` instance, or not do anything if the application is no longer in a state for showing the Image Message.
```java
class MyPrepareListener implements ImageMessage.PrepareListener {
    
    @Override
    public void onPrepared(ImageMessage src) {
        src.show(MyActivity.this, MY_REQUEST_CODE);
    }
    
    @Override
    public void onError() {
        // act on error
    }
}
```
To handle the result of the action performed on the Image Message you will need to override the `onActivityResult(int, int, Intent)` method of your `Activity`
```java
@Override
public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == MY_REQUEST_CODE) {
        ImageMessageActivity.handleResult(
                resultCode,
                data,
                new ImageMessageResultListener() {
                    @Override
                    public void onAction(String value, String params) {
                        // act on action button with value/params
                    }
                    
                    public void onLink(String value, String params) {
                        // act on link button with value/params
                    }
                    
                    @Override
                    public void onCancelled() {
                        // act on cancellation
                    }
                });
    }
}
```

## Forgetting a user (GDPR)
If a user no longer wishes to be tracked and would like to be forgotten the `forgetMe()` API can be used. This will stop the SDK from sending/receiving any further information to/from the Platform, as well as initiating a data deletion request on behalf of the user. The SDK will continue to work as it normally would, without any additional work required.

If the game supports changing of users then calling `startSdk(userId)` with a new user ID or `clearPersistentData()` will restore the previous SDK functionality. 

## Push notifications
The SDK can store the Android Registration Id for the device and send it to deltaDNA so that you may send targeted push notification messages to players.

If your application already handles retrieving of the id then you can set it on the SDK by calling
```java
DDNA.instance().setRegistrationId("your_id");
```
You may however also make use of the [deltadna-sdk-notifications](library-notifications) addon which requires less work on your side for refreshing the registration id/token.

If you would like to unregister the client from receiving push notifications then you should call
```Java
DDNA.instance().clearRegistrationId();
```

## Settings
If you need further customisation on how the SDK works, such as disabling the automatic event uploads, or changing the number of retries for failed requests then you may do so through the `Settings` class, which can be retrieved through
```java
DDNA.instance().getSettings();
```
Settings can also be set during the initialisation step on the `Configuration`, which is the recommended approach.

## ProGuard
There is no need to add additional directives in your ProGuard configuration if you are setting `minifyEnabled true` for your application as the library provides its own configuration file which gets included by the Android build tools during the build process.

## Changelog
Can be found [here](CHANGELOG.md).

## Migrations
* [Version 4.0](docs/migrations/4.0.md)
* [Version 4.1](docs/migrations/4.1.md)
* [Version 4.3](docs/migrations/4.3.md)
* Version 4.9
  * The SDK has been updated to use Java 8 features, as such projects will need to be updated to use 1.8 for the Java source and target compatibility as per the [official documentation](https://developer.android.com/studio/write/java8-support).
  * `recordEvent` methods have been changed to to return an `EventAction` object, which can be used for Event-Triggered Campaigns. This means that chaining calls on the `DDNA` SDK instance after calling `recordEvent` is no longer supported.

## License
The sources are available under the Apache 2.0 license.

## Contact Us
For more information, please visit [deltadna.com](https://deltadna.com/). For questions or assistance, please email us at [support@deltadna.com](mailto:support@deltadna.com).
