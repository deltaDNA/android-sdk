![deltaDNA logo](https://deltadna.com/wp-content/uploads/2015/06/deltadna_www@1x.png)

# deltaDNA Android SDK通知
[![Build Status](https://travis-ci.org/deltaDNA/android-sdk.svg)](https://travis-ci.org/deltaDNA/android-sdk)
[![codecov.io](https://codecov.io/github/deltaDNA/android-sdk/coverage.svg)](https://codecov.io/github/deltaDNA/android-sdk)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/b5546fd90d3b4b2182961602da6086d8)](https://www.codacy.com/app/deltaDNA/android-sdk)
[![Apache2 licensed](https://img.shields.io/badge/license-Apache-blue.svg)](./LICENSE)
[![Download](https://api.bintray.com/packages/deltadna/android/deltadna-sdk/images/download.svg)](https://bintray.com/deltadna/android/deltadna-sdk/_latestVersion)

## 目录
* [概述](#概述)
* [添加至项目](#添加至项目)
* [整合](#整合)
* [注册](#注册)
* [高级](#高级)
 * [通知](#通知)
 * [Token检索](#Token检索)
 * [防反编译（ProGuard）](#防反编译（ProGuard）)
* [常见问题解答](#常见问题解答)
* [更新日志](#更新日志)
* [授权](#授权)

## 概述
这是一个deltaDNA Android SDK的附加模块，可以很容易的将推送通知功能整合到一个项目中。

当发送一个推送通知到客户端时，这个程序将展示来自平台的`Alert`区域的通知消息并将应用的名字作为标题，除非`title`密钥下的一个值已经被添加到推送消息的装置中。一旦这个通知被用户触发

更多关于整合和定制的细节可以在这个文档中进一步找到。

## 添加至项目
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
compile 'com.deltadna.android:deltadna-sdk:4.1.2-SNAPSHOT'
compile 'com.deltadna.android:deltadna-sdk-notifications:4.1.2-SNAPSHOT'
```

## 整合
当你将这个SDK和这个通知插件添加到你的项目后，你将需要添加如下两个权限（permission）到你的`AndroidManifest.xml`文件的`manifest`部分
```xml
<permission
    android:name="your.package.name.permission.C2D_MESSAGE"
    android:protectionLevel="signature"/>
<uses-permission
    android:name="your.package.name.permission.C2D_MESSAGE"/>
```
其中`your.package.name`需要用你的应用程序的包名替代。

下一步是将如下的定义添加到`application`中
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
其中`your.package.name`需要用你的应用程序的包名替代。

现在你将需要将[`NotificationListenerService`](src/main/java/com/deltadna/android/sdk/notifications/NotificationListenerService.java)添加到上一步骤的同一个元素层级
```xml
<service
    android:name="com.deltadna.android.sdk.notifications.NotificationListenerService"
    android:exported="false">
    
    <intent-filter>
        <action android:name="com.google.android.c2dm.intent.RECEIVE"/>
    </intent-filter>
</service>
```
上述的定义可以由这个库提供。这个服务非常重要，因为其需要在平台发出推送消息后在用户界面显示通知，例如我们可以定制其行为。

最后一步是添加你的发送者ID（也就是从Google Developer Console获得的项目编号）到这个应用程序的字符串资源。并在清单的`application`部分添加一个参考
```xml
<meta-data
    android:name="ddna_sender_id"
    android:resource="@string/sender_id"/>
```

你可以随时参考[这里](../examples/notifications)的案例项目。

## 注册
为了通过这个平台给推送通知注册客户端，`register()`方法需要被从[`DDNANotifications`](src/main/java/com/deltadna/android/sdk/notifications/DDNANotifications.java)调用。这将初始化一个请求来从GCM搜索一个注册Token，并将其发送到deltaDNA的服务器。请注意后者将只在`DDNA.startSdk()`被调用时发生。例如如果应用程序运行时这个注册Token被搜索到，当这个应用程序将再次开始时这个新的Token将被发送到服务器，因此发送通知到客户端将不会发生直至上述事件发生以后。

例如，一个调用`register()`的好的时机将是当用户为其应用程序在设置中启用通知或者一个之前的尝试搜索Token失败时（更多的细节可以从[这里](#Token检索)找到）。

也可以通过从`DDNANotifications`调用`unregister()`注销推送通知到客户端。

## 高级
### 通知
默认`NotificationListenerService`将为`title`键的值查询推送消息的载荷。如果没有，那么应用程序的名称将被用于取代通知的标题。

自定义这个行为的最简单方式是在`application`部分的清单文件设置`ddna_notification_title` `meta-data`属性
```xml
<meta-data
    android:name="ddna_notification_title"
    android:resource="@string/notification_title"/>
```

用一个相似的方式，这个被显示的图标可以通过设置`ddna_notification_icon`属性定制
```xml
<meta-data
    android:name="ddna_notification_icon"
    android:value="ic_stat_logo"/>
```
值得注意的是由于Android里面的一个限制，这时不支持使用可拉伸的资源。因此上述的例子`ic_stat_logo`事实上将在运行时被以`@drawable/ic_stat_logo`的方式解决。如果没有设置，那么一个默认的图片将被库使用。

最后，在默认情况下，通知将启用被定义为应用程序启用意图的活动。这个行为可以被禁用，通过
```xml
<meta-data
    android:name="ddna_start_launch_intent"
    android:value="false"/>
```

如果通知的属性需要在运行时被动态改变，那么`NotificationListenerService`可以被扩展，`createNotification`或`notify`方法将被覆盖。在这种情况下，你将需要改变在清单文件中的`service`定义以指向你自己的类。

### Token检索
在Hood下通过库搜索GCM注册Token时，知道何时其成功或失败是有用的。基于这个原因，库将用`DDNANotifications.ACTION_TOKEN_RETRIEVAL_SUCCESSFUL`行为通过[`LocalBroadcastManager`](http://developer.android.com/reference/android/support/v4/content/LocalBroadcastManager.html)发送一个广播，这个Token将被包含在附加的`DDNANotifications.EXTRA_REGISTRATION_TOKEN` `String`下的`Intent`中。

同样的，如果搜索失败，一个广播将通过`DDNANotifications.ACTION_TOKEN_RETRIEVAL_FAILED`行为被发送。失败的原因可以从附加的`DDNANotifications.EXTRA_FAILURE_REASON` `Serializable`中找到，这将是一个`Throwable`类型。

我们为了应用方便，提供了一个[`IntentFilter`](http://developer.android.com/reference/android/content/IntentFilter.html)，被设置用于在`DDNANotifications.FILTER_TOKEN_RETRIEVAL`中监听的上述的两个行为。

一个[`BroadcastReceiver`](http://developer.android.com/reference/android/content/BroadcastReceiver.html)的应用实例可以从[这里](../examples/notifications/src/main/java/com/deltadna/android/sdk/notifications/example/ExampleReceiver.java)找到。

### 防反编译（ProGuard）
如果你为你的应用设置`minifyEnabled true`，那么没有必要在你的ProGuard配置中添加额外的代码。因为这个库提供了其自己的配置文件，可以在编译过程中被Android编译工具包含进去。

## 常见问题解答
1.  我的项目有一个Dependency在较新版本的Google Play Services，我是否可以使用一个不同于文档中GCM版本的其他版本？
    
    是的，通过从通知Dependency中移除GCM并分别抓取它。
    ```java
    compile('com.deltadna.android:deltadna-sdk-notifications:VERSION') {
        exclude module: 'play-services-gcm'
    }
    compile 'com.google.android.gms:play-services-gcm:8.4.0'
    ```
    到目前为止，我们已经确认8.*版本可以替代7.8版本。
