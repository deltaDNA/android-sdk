![deltaDNA logo](https://deltadna.com/wp-content/uploads/2015/06/deltadna_www@1x.png)

# deltaDNA Android SDK
[![Build Status](https://travis-ci.org/deltaDNA/android-sdk.svg)](https://travis-ci.org/deltaDNA/android-sdk)
[![Download](https://api.bintray.com/packages/deltadna/android/deltadna-sdk/images/download.svg)](https://bintray.com/deltadna/android/deltadna-sdk/_latestVersion)

## 目录
* [概述](#概述)
* [添加至项目](#添加至项目)
* [初始化](#初始化)
* [启用和停止](#启用和停止)
* [记录事件](#记录事件)
 * [事件剖析](#事件剖析)
 * [简单事件](#简单事件)
 * [复杂事件](#复杂事件)
 * [交易（Transaction）](#交易（Transaction）)
* [吸引（Engage）](#吸引（Engage）)
* [图片消息](#图片消息)
* [推送通知](#推送通知)
* [设置](#设置)
* [防反编译（ProGuard）](#防反编译（ProGuard）)
* [更新日志](#更新日志)
* [迁移](#迁移)
* [授权](#授权)

## 概述
deltaDNA SDK允许你的Android游戏记录游戏中的事件和上传玩家的操作。它包括事件存储、众多辅助工具和一些自动的配置以帮助简化你的整合。

事件被通过JSON对象发送到deltaDNA平台。这些事件通过在线的信息界面被管理和跟踪。

当你的游戏记录事件后，这个SDK将在本地存储这些事件并在网络正常连接时将其定期上传，或者在你选择的某个固定时间上传。无论网络是否正常连接，这个SDK都可收集事件，并且这提供给你上传时间和频率的控制权。

事件的复杂度有所不同，但是所有的事件均采用一个共同的事件结构。本文档和附带的案例应用程序将提供各种复杂度的事件示例。

## 添加至项目
deltaDNA SDK可以用于基于第15版和更新版本（Android 4.0.3+）内核SDK的Android项目。

### Gradle
在你的顶层构建脚本
```groovy
allprojects {
    repositories {
        maven { url 'http://deltadna.bintray.com/android' }
        // repositories为你其他的dependencies...
    }
}
```
在你APP的构建脚本
```groovy
compile 'com.deltadna.android:deltadna-sdk:4.1.0-SNAPSHOT'
```

## 初始化
在一个`Application`子类中，这个SDK需要通过以下参数进行初始化：
* `Application`实例
* `environmentKey`，分配给你的应用的一个唯一的32位字符串。你将被分配两个相互分离的应用键值分别对应你的游戏开发测试和产品应用。当你从开发测试转换到产品应用时，你需要改变这个初始化SDK时的环境键值。
* `collectUrl`，这是收集你的事件的服务器地址。
* `engageUrl`，这是提供实时A/B测试和命中目标（real-time A/B Testing and Targeting）的服务器地址。只有你的游戏使用这些功能时才需要。
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

你需要在manifest文件中注册你的`Application`子类
```xml
<application
    android:name=".MyApplication"
    ...>
</application>
```

在调用`initialise()`后，这个SDK将可以通过调用`DDNA.instance()`在你的应用的全生命周期内正常使用。

你也可以在`Configuration`中设置可选属性，例如客户端版本或者用户ID以及其他选项。

## 启用和停止
在你的`Activity`类中，你需要使用`onCreate(Bundle)`方法中的`DDNA.instance().startSdk()`函数启用这个SDK。同样地，使用`onDestroy()`方法中的`DDNA.instance().stopSdk()`函数停止这个SDK。
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
这是初始化这个SDK并开始发送事件的最少代码。它将在这个SDK第一次运行时自动发送*新玩家（newPlayer）*事件，并在游戏每次运行时发送*游戏开始（gameStarted）*和*客户端设备（clientDevice）*事件。

## 记录事件
### 事件剖析
所有的事件都使用一个共享的基础结构通过JSON文件记录。JSON文件对于每一个事件类型都是不同的，但是所有的JSON文件都遵循如下的最小结构：
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
这个SDK将自动填充上述的区域。当你添加额外的参数到一个事件中时，它们将会被放置于`eventParams`元素内。

### 简单事件
通过使用标准事件结构中的一种，我们可以记录一个事件。例如
```java
DDNA.instance().recordEvent(new Event("options")
        .putParam("option", "Music")
        .putParam("action", "Disabled"));
```
这将通过如下的JSON文件上传
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

### 复杂事件
如果你创建在你的游戏中会重复使用的更加复杂的事件，这时不需要每次都创建这个事件，而是可以从`Event`类中派生出子类并将你的参数添加到构造器
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
而且你将能够通过创建一个新的实例并将其传到`recordEvent(Event)`函数以记录事件
```java
DDNA.instance().recordEvent(new MissionStartedEvent(
        "Mission01",
        "M001",
        false,
        "EASY"));
```

### 交易（Transaction）
交易（Transaction）是一个复杂事件，可以在当你遇到玩家从游戏提供商或其他玩家那里购买、交易、赢得或交换游戏币和装备时提供嵌套、数组和一些特殊对象。为了帮助实现这些功能我们提供了`Transaction`方法，这是一个拥有额外属性的`Event`类的方法
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
还值得一提的是，货币币值总是被以整数为最小货币单位发送，并采用ISO-4217标准的三位货币代码。

这个事件可以被设计的更复杂，但结构是合乎逻辑的、灵活的，并为玩家消费或者接收任何货币和装备的组合提供一种机制。

## 吸引（Engage）
游戏可以从吸引（Engage）检索对时间响应敏感的信息，从而决定是否在特定时间基于A/B测试（A/B Test）的结果或者命中目标（Targeting）对用户施加一个特别的行为。基本上你的游戏都应该在游戏中预先确定的决策点设置吸引（Engage）请求，同时响应将允许你立刻为用户提供个性化的游戏行为。
```java
requestEngagement(
        new Engagement("outOfCredits")
                .putParam("userLevel", 4)
                .putParam("userXP", 1000)
                .putParam("missionName", "Diso Volante"),
        new OutOfCreditsListener());
```
上述例子的响应处理程序是
```java
private class OutOfCreditsListener implements EngageListener {

    public void onSuccess(JSONObject result) {
        // 基于结果（result）做一些事情
    }

    public void onFailure(Throwable t) {
        // 失败时执行
    }
}
```
你将接收到一个JSON响应
```json
{
    "transactionID": 1898710706656641000,
    "parameters": {
        "creditPackPrice": 99,
        "creditPackSize": 1
    }
}
```
包括含有和玩家在这一时间点相关的任意参数的`transactionID`和`parameters`对象。

你可能接收到一个包括`transactionID`的响应，但是没有任何带有个性化值的参数。这表明玩家没能符合任何资质标准或者还没有被分配到一个对照组。

如果你的吸引（Engage）请求服务器时有一个错误进程，这时`onFailure(Throwable)`方法将被使用包括带有状态码的`Response`的`RequestException`调用。这个状态码可能是如下情形之一：
* 400，如果输入的格式错误、内容不正确，或者你发送的实时参数还没有被添加到你的游戏参数列表。
* 403，如果哈希密码错误或者吸引（Engage）还没有在你的账号中激活。
* 404，不正确的路径地址或者未知的环境键值。

## 图片消息
图片消息请求采用与吸引（Engage）请求相似的方式执行
```java
DDNA.instance().requestImageMessage(
        new Engagement("missionDifficulty"),
        new ImageMessageListener(MyActivity.this, MY_REQUEST_CODE));
```
当你的监听程序使用`onPrepared(ImageMessage)`方法被调用时，你可以通过调用`show(ImageMessage)`方法展示`ImageMessage`，或者在这个应用不再处于展示图片消息的状态时不做任何事情。

为了处理图片消息执行的结果，你需要重写`Activity`类中的`onActivityResult(int, int, Intent)`方法
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
                        // 执行值/参数
                    }

                    @Override
                    public void onCancelled() {
                        // 执行取消
                    }
                });
    }
}
```

## 推送通知
这个SDK可以存储设备的Android注册ID并将其发送到deltaDNA的服务器，所以你可以向玩家发送有针对性的推送通知消息。

如果你的应用已经检索了ID，那么你可以在SDK通过调用如下代码中设置
```java
DDNA.instance().setRegistrationId("your_id");
```
然而你可能还会使用[deltadna-notifications](https://github.com/deltaDNA/android-notifications-sdk)插件，这要求在你开发中的一点儿工作来刷新GCM ID或token.

如果你想要在接收推送通知时注销客户端，你需要调用
```Java
DDNA.instance().clearRegistrationId();
```

## 设置
如果你需要进一步的关于这个SDK如何工作的自定义设置，例如禁用事件自动上传或者改变在请求失败时重试的次数，你可能需要通过设置`Settings`类来实现。这可以通过如下源码实现
```java
DDNA.instance().getSettings();
```
Settings类也可以在初始化`Configuration`时被设置。

## 防反编译（ProGuard）
如果你为你的应用设置`minifyEnabled true`，那么没有必要在你的ProGuard配置中添加额外的代码。因为这个库提供了其自己的配置文件，可以在编译过程中被Android编译工具包含进去。

## 更新日志
可以从[这里](CHANGELOG.md)找到。

## 迁移
* [第四版](docs/migrations/4.md)

## 授权

该资源适用于Apache 2.0授权。
