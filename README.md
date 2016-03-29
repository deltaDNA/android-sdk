![deltaDNA logo](https://deltadna.com/wp-content/uploads/2015/06/deltadna_www@1x.png)

# deltaDNA Android SDK
[![Build Status](https://travis-ci.org/deltaDNA/android-sdk.svg)](https://travis-ci.org/deltaDNA/android-sdk)
[![Download](https://api.bintray.com/packages/deltadna/android/deltadna-sdk/images/download.svg)](https://bintray.com/deltadna/android/deltadna-sdk/_latestVersion)

## Contents
* [Overview](#overview)
* [Adding to a project](#adding-to-a-project)
* [Initialising](#initialising)
* [Starting and stopping](#starting-and-stopping)
* [Recording events](#recording-events)
 * [Anatomy of an event](#anatomy-of-an-event)
 * [Simple event](#simple-event)
 * [Complex event](#complex-event)
 * [Transactions](#transactions)
* [Engage](#engage)
* [Image Messaging](#image-messaging)
* [Push notifications](#push-notifications)
* [Settings](#settings)
* [ProGuard](#proguard)
* [Changelog](#changelog)
* [Migrations](#migrations)
* [License](#license)

## Overview
The deltaDNA SDK allows your Android games to record in-game events and upload
player actions. It contains event caching, numerous helper methods, and some
automated behaviours to help simplify your integration.

Events are sent to the deltaDNA platform as JSON objects. These events are
managed and tracked using the online dashboard.

When your game records events the SDK will store them locally and upload them
at regular intervals when a connection is available, or at a time of your
choosing. This allows the SDK to collect events regardless of connectivity and
gives you control over the timing and frequency of uploads.

Events vary in complexity, but are all derived from a common event schema.
This document and the accompanying example application provide examples of
increasingly complex events.

## Adding to a project
The deltaDNA SDK can be used in Android projects using minimum SDK version 9
and newer (Android 2.3+).

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
compile 'com.deltadna.android:deltadna-sdk:4.0.2-SNAPSHOT'
```

## Initialising
The SDK needs to be initialised with the following parameters in an
`Application` subclass:
* `Application` instance
* `environmentKey`, a unique 32 character string assigned to your application.
You will be assigned separate application keys for development and production
builds of your game. You will need to change the environment key that you
initialise the SDK with as you move from development and testing to production.
* `collectUrl`, this is the address of the server that will be collecting your
events.
* `engageUrl`, this is the address of the server that will provide real-time A/B
Testing and Targeting. This is only required if your game uses these features.
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

After the `initialise()` call the SDK will be available throughout the entire
lifecycle of your application by calling `DDNA.instance()`.

You may also set optional attributes on the `Configuration`, such as the client
version, or user id, amongst other options.

## Starting and stopping
Inside of your `Activity` class you will need to start the SDK with
`DDNA.instance().startSdk()` from the `onCreate(Bundle)` method, and likewise
stop the SDK with `DDNA.instance().stopSdk()` from the `onDestroy()` method.
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
This is the minimum amount of code needed to initialise the SDK and start
sending events. It will automatically send the *newPlayer* event the first
time the SDK is run, and the *gameStarted* and *clientDevice* events each
time the game runs.

## Recording events
### Anatomy of an event
All events are recorded as JSON documents with a shared basic schema. The JSON
will be different for every event type, but all of them should adhere to the
following minimal schema:
```json
{
    "eventName": "gameEnded",
    "userID": "a2e92bdd-f59d-498f-9385-2ae6ada432e3",
    "sessionID": "0bc56224-8939-4639-b5ba-197f84dad4f4",
    "eventTimestamp":"2014-07-04 11:09:42.491",
    "eventParams": {
        "platform": "ANDROID",
        "sdkVersion": "Android SDK v4.0",
    }
}
```
The SDK will automatically populate the above fields. When you add additional
parameters to an event they will be placed inside of the `eventParams`
element.

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
If you create more complicated events which get reused throughout of your game
then instead of building up the event each time you can subclass from `Event`
and add your parameters into the constructor
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
And you will be able to record events by creating a new instance and passing
it to `recordEvent(Event)`
```java
DDNA.instance().recordEvent(new MissionStartedEvent(
        "Mission01",
        "M001",
        false,
        "EASY"));
```

### Transactions
A transaction is a complex event which introduces nesting, arrays, and some
special objects that you will encounter when the player buys, trades, wins,
exchanges currency and items with the game or other players. To help with this
we provide `Transaction`, which is an `Event` with additional properties
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
It is also worth noting that the currency value is always sent as an integer
in the minor currency unit and with the ISO-4217 3 character currency code.

This event may be more complex, but the structure is logical, flexible, and
provides a mechanism for players spending or receiving any combination of
currencies and items.

## Engage
Games can retrieve time-sensitive information from Engage to determine if a
particular action should be taken for the user at a specific time, based on
A/B Test results or Targeting. Essentially your game should make engage
requests at predetermined decision points in your game and the response will
allow you to personalise the gameplay for that user instantly.
```java
requestEngagement(
        new Engagement("outOfCredits")
                .putParam("userLevel", 4)
                .putParam("userXP", 1000)
                .putParam("missionName", "Diso Volante"),
        new OutOfCreditsListener());
```
And the response handler for the above example
```java
private class OutOfCreditsListener implements EngageListener {

    public void onSuccess(JSONObject result) {
        // do something with result
    }

    public void onFailure(Throwable t) {
        // act on failure
    }
}
```
You will receive a JSON response
```json
{
    "transactionID": 1898710706656641000,
    "parameters": {
        "creditPackPrice": 99,
        "creditPackSize": 1
    }
}
```
With a a `transactionID` and `parameters` object containing any parameters
relevant to this player at this point in time.

You may receive a response containing a `transactionID` but no parameters with
personalisation values. This indicates that the player has failed to meet any
qualification criteria or has not been allocated to a control group.

If there was an error processing your Engage request at the server then the
`onFailure(Throwable)` method will be invoked with a `RequestException`
containing a `Response` with a status code, which may be one of the following:
* 400, if the inputs were malformed, incorrect, or you are sending real-time
parameters that haven't been added to your Game Parameter list.
* 403, if the secret hash is incorrect or Engage is not enabled on your account.
* 404, incorrect URL or unknown environment key.

## Image Messaging
An Image Messaging request is performed in a similar way to an Engage
request
```java
DDNA.instance().requestImageMessage(
        new Engagement("missionDifficulty"),
        new ImageMessageListener(MyActivity.this, MY_REQUEST_CODE));
```
When the `onPrepared(ImageMessage)` of your listener gets invoked you
may show the `ImageMessage` by calling `show(ImageMessage)`, or not do
anything if the application is no longer in a state for showing the
Image Message.

To handle the result of the action performed on the Image Message you
will need to override the `onActivityResult(int, int, Intent)` method
of your `Activity`
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
                        // act on value/params
                    }

                    @Override
                    public void onCancelled() {
                        // act on cancellation
                    }
                });
    }
}
```

## Push notifications
The SDK can store the Android Registration Id for the device and send it to
deltaDNA so that you may send targeted push notification messages to players.

If your application already handles retrieving of the id then you can set it on
the SDK by calling
```java
DDNA.instance().setRegistrationId("your_id");
```
You may however also make use of the
[deltadna-notifications](https://github.com/deltaDNA/android-notifications-sdk)
addon which requires less work on your side for refreshing the GCM
id/token.

If you would like to unregister the client from receiving push notifications
then you should call
```Java
DDNA.instance().clearRegistrationId();
```

## Settings
If you need further customisation on how the SDK works, such as disabling the
automatic event uploads, or changing the number of retries for failed requests
then you may do so through the `Settings` class, which can be retrieved through
```java
DDNA.instance().getSettings();
```
Settings can also be set during the initialisation step on the `Configuration`.

## ProGuard
There is no need to add additional directives in your ProGuard configuration if
you are setting `minifyEnabled true` for your application as the library
provides its own configuration file which gets included by the Android build
tools during the build process.

## Changelog
Can be found [here](CHANGELOG.md).

## Migrations
* [version 4](docs/migrations/4.md)

## License

The sources are available under the Apache 2.0 license.
