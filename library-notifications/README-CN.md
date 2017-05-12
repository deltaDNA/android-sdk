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
 * [样式和行为](#样式和行为)
  * [Unity](#unity)
 * [事件](#事件)
 * [防反编译（ProGuard）](#防反编译（ProGuard）)
* [常见问题解答](#常见问题解答)
* [更新日志](#更新日志)
* [授权](#授权)

## 概述
这是一个deltaDNA Android SDK的附加模块，可以很容易的将推送通知功能整合到一个项目中。

当发送一个推送通知到客户端时，这个程序将展示来自平台的`alert`区域的通知消息并将应用的名字作为标题，除非`title`密钥下的一个值已经被添加到推送消息的装置中。当用户点击这个通知后，此模块将通过SDK发送适当的事件。

更多关于整合和定制的细节可以在这个文档中进一步找到。

## 添加至项目
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
compile 'com.deltadna.android:deltadna-sdk:4.4.0-SNAPSHOT'
compile 'com.deltadna.android:deltadna-sdk-notifications:4.4.0-SNAPSHOT'
```

## 整合
当你在你的项目中添加了这个SDK和通知插件后，你将需要在`application`部分添加一个[`NotificationListenerService`](src/main/java/com/deltadna/android/sdk/notifications/NotificationListenerService.java)定义
```xml
<service
    android:name="com.deltadna.android.sdk.notifications.NotificationListenerService">
    
    <intent-filter>
        <action android:name="com.google.android.c2dm.intent.RECEIVE"/>
    </intent-filter>
</service>
```
上述的定义可以由这个库提供。这个服务非常重要，因为其需要在平台发出推送消息后在用户界面显示通知，例如我们可以定制其行为。

最后一步是从Firebase控制面板添加你的应用（Application）和发件人ID（Sender IDs）到清单（manifest）文件。如果你的应用已经设置为使用Google开发者控制面板，那么这时你可以按照[此处](https://firebase.google.com/support/guides/google-android#migrate_your_console_project)的技术指导轻松的迁移这个项目到Firebase。
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

你可以随时参考[这里](../examples/notifications)的案例项目。

## 注册
为了通过这个平台给推送通知注册客户端，`register()`方法需要被从[`DDNANotifications`](src/main/java/com/deltadna/android/sdk/notifications/DDNANotifications.java)调用。这将初始化一个请求来从GCM搜索一个注册Token，并将其发送到deltaDNA的服务器。

例如，一个调用`register()`的好的时机将是当用户为其应用程序在设置中启用通知或者一个之前的尝试搜索Token失败时。

也可以通过从DDNANotifications调用unregister()注销推送通知到客户端。如果你想要稍后注册，那么应该调用`register()`。

## 高级
### 样式和行为
如果你想要改变通知的样式，例如使用可扩展的文本，那么可以扩展[`NotificationListenerService`](src/main/java/com/deltadna/android/sdk/notifications/NotificationListenerService.java)以修改默认的行为。

一个例子可以从[这里](../examples/notifications-style/src/main/java/com/deltadna/android/sdk/notifications/example/StyledNotificationListenerService.java)找到。你还将需要改变清单（manifest）文件中的`service`定义以指向新的类。

#### Unity
改变Unity上的样式略微复杂一些，但是下面的步骤描述了如何实现这一点；
1.  你将需要导入（checkout）[android-sdk](https://github.com/deltaDNA/android-sdk)项目并在Android Studio中打开它。确保你已经下载了所有需要的依赖，从而能够构建这个项目。
2.  导入你需要的项目版本从而匹配在deltaDNA Unity SDK中使用的版本。你可以通过跳转进入`Assets/DeltaDNA/Plugins/Android`查找以找出`deltadna-sdk-notifications-*.aar`文件的版本。例如，如果在这个路径下的文件被命名为`deltadna-sdk-notifications-4.2.3.aar`，那么你将需要在`android-sdk`项目中执行`git checkout 4.2.3`。
3.  现在你可以对[`NotificationListenerService`](src/main/java/com/deltadna/android/sdk/notifications/NotificationListenerService.java)类进行修改，直接进行或者创建一个新的从其派生出的类并覆盖适当的方法。
4.  更改以后，你可以通过从项目的跟目录运行`./gradlew clean build check`构建这个SDK。一旦成功构建，新的ARR就可以从`library-notifications/build/outputs/aar`（确保使用发行版本）复制到`Assets/DeltaDNA/Plugins/Android`以替代已有的AAR。如果你已经在一个新的类中做出了更改，那么你将还需要在通知配置UI为你的Unity项目更改*Listener Service*入口，以使用你的新替换的类。

### 事件
这个模块发送一些与注册推送通知，推送它们到UI和在其上面监听用户交互相关的事件。你可以通过扩展[`EventReceiver`](src/main/java/com/deltadna/android/sdk/notifications/EventReceiver.java)并覆盖所需的方法来监听这些事件。

你还需要在你应用的清单（manifest）文件注册你的接收器，例如：
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

[`EventReceiver`](src/main/java/com/deltadna/android/sdk/notifications/EventReceiver.java)的示例实现可以从[这里](../examples/notifications/src/main/java/com/deltadna/android/sdk/notifications/example/ExampleReceiver.java)找到。

### 防反编译（ProGuard）
如果你为你的应用设置`minifyEnabled true`，那么没有必要在你的ProGuard配置中添加额外的代码。因为这个库提供了其自己的配置文件，可以在编译过程中被Android编译工具包含进去。
