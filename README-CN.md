![deltaDNA logo](https://deltadna.com/wp-content/uploads/2015/06/deltadna_www@1x.png)

# deltaDNA Android SDK
[![Build Status](https://travis-ci.org/deltaDNA/android-sdk.svg)](https://travis-ci.org/deltaDNA/android-sdk)
[![codecov.io](https://codecov.io/github/deltaDNA/android-sdk/coverage.svg)](https://codecov.io/github/deltaDNA/android-sdk)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/b5546fd90d3b4b2182961602da6086d8)](https://www.codacy.com/app/deltaDNA/android-sdk)
[![Apache2 licensed](https://img.shields.io/badge/license-Apache-blue.svg)](./LICENSE)
[![Download](https://api.bintray.com/packages/deltadna/android/deltadna-sdk/images/download.svg)](https://bintray.com/deltadna/android/deltadna-sdk/_latestVersion)

## 目录
* [概述](#概述)
* [添加至项目](#添加至项目)
* [初始化](#初始化)
* [启用和停止](#启用和停止)
* [记录事件](#记录事件)
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
deltaDNA SDK允许你的Android游戏记录游戏中的事件和上传玩家的操作。它包括事件存储、众多辅助工具和一些自动的行为以帮助简化你的整合。

## 添加至项目
deltaDNA SDK可以用于基于第15版和更新版本（Android 4.0.3+）内核SDK的Android项目。

### Gradle
在你的顶层构建脚本
```groovy
allprojects {
    repositories {
        maven { url 'http://deltadna.bintray.com/android' }
        // 存放你的其他依赖...
    }
}
```
在你APP的构建脚本
```groovy
compile 'com.deltadna.android:deltadna-sdk:4.11.2-SNAPSHOT'
```

## 初始化
在一个`Application`子类中，这个SDK需要通过以下参数进行初始化：
* `Application`实例
* `environmentKey`，分配给你的应用的一个唯一的32位字符串。你将被分配两个相互分离的应用键值分别对应你的游戏开发测试和产品应用。当你从开发测试转换到产品应用时，你需要改变这个初始化SDK时的环境键值。
* `collectUrl`，这是收集你的事件的服务器地址。
* `engageUrl`，这是提供实时A/B测试和命中目标（real-time A/B Testing and Targeting）的服务器地址。
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
这是初始化这个SDK并开始发送事件的最少代码。它将在这个SDK第一次运行时自动发送*newPlayer*事件，并在游戏每次运行时发送*gameStarted*和*clientDevice*事件。

## 记录事件
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
一个吸引（Engage）请求可以通过调用`requestEngagement(Engagement, EngageListener)`实现。提供给你`Engagement`和`EngageListener`来监听是否执行完成或者出现错误。
```java
requestEngagement(
        new Engagement("outOfCredits")
                .putParam("userLevel", 4)
                .putParam("userXP", 1000)
                .putParam("missionName", "Diso Volante"),
        new OutOfCreditsListener());
```
发送的`Engagement`对象将会返回到监听者的`onCompleted(Engagement)`回调方法。此时其已经填充了来自平台的数据，可以通过在`Engagement`调用`getJson()`来取回。
```java
class OutOfCreditsListener implements EngageListener<Engagement> {
    
    public void onCompleted(Engagement engagement) {
        // 对结果进行操作
        if (engagement.isSuccessful()) {
            // 用参数举例
            JSONObject parameters = engagement.getJson()
        }
    }
    
    public void onError(Throwable t) {
        // 获取错误
    }
}
```
如果你在服务器的吸引（Engage）请求出现错误的进程，那么通过在`Engagement`调用`getError()`将可以获得细节信息。任何非服务器错误，例如由于网络连接不可用，将会传送到`onError(Throwable)`回调方法。在这种情况下`onCompleted(Engagement)`将永远不会被调用。

### 图片信息
一个图片信息请求的执行与一个吸引（Engage）请求类似。一个`ImageMessage`实例通过`onCompleted(Engagement)`回调方法返回的`Engagement`被创建。由于决策点可能还没有被设置来显示一个图片信息，`ImageMessage.create(Engagement)`的返回值需要被检查是否为空。
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
                // 获取错误
            }
        });
```
当你的`ImageMessage.PrepareListener`监听者的`onPrepared(ImageMessage)`被调用，你可以通过在`ImageMessage`实例调用`show(Activity, int)`来显示图片信息，或者如果应用不是显示图片信息的状态时不做任何操作。
```java
class MyPrepareListener implements ImageMessage.PrepareListener {
    
    @Override
    public void onPrepared(ImageMessage src) {
        src.show(MyActivity.this, MY_REQUEST_CODE);
    }
    
    @Override
    public void onError(Throwable cause) {
        // 获取错误
    }
}
```
为了处理执行图片信息行为的结果，你将需要重写你的`Activity`中的`onActivityResult(int, int, Intent)`方法
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
                        // 获取带有值/参数的行为按钮
                    }
					
                    public void onLink(String value, String params) {
                        // 获取带有值/参数的链接按钮
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

如果你的应用已经检索了ID，那么你可以在SDK通过调用如下代码来设置
```java
DDNA.instance().setRegistrationId("your_id");
```
然而你可能还会使用[deltadna-sdk-notifications](library-notifications)插件，这要求在你开发中的一点儿工作来刷新GCM ID或token。

如果你想要在接收推送通知时注销客户端，你需要调用
```Java
DDNA.instance().clearRegistrationId();
```

## 设置
如果你需要进一步的关于这个SDK如何工作的自定义设置，例如禁用事件自动上传或者改变在请求失败时重试的次数，你可能需要通过设置`Settings`类来实现。这可以通过如下源码实现
```java
DDNA.instance().getSettings();
```
Settings类也可以在初始化`Configuration`时被设置，这是被推荐的方法。

## 防反编译（ProGuard）
如果你为你的应用设置`minifyEnabled true`，那么没有必要在你的ProGuard配置中添加额外的代码。因为这个库提供了其自己的配置文件，可以在编译过程中被Android编译工具包含进去。

## 更新日志
可以从[这里](CHANGELOG.md)找到。

## 迁移
* [版本4.0](docs/migrations/4.0.md)
* [版本4.1](docs/migrations/4.1.md)
* [版本4.3](docs/migrations/4.3.md)

## 授权
该资源适用于Apache 2.0授权。
