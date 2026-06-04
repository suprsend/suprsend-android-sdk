# Mobile Push Setup

Step-by-step guide to integrate FCM Push notifications into your android app using SuprSend.

## Integration

### 1. Create Firebase project in firebase console

To start sending notifications from FCM, you'll have to first create a firebase project. Create firebase project and application in [firebase console](https://firebase.google.com/) with your applications package name which you can find in `MainApplication.java` or `AndroidManifest.xml`

### 2. Adding google-services.json

You can get your Service Account JSON by [following these instructions](https://firebase.google.com/docs/cloud-messaging/auth-server#provide_credentials_manually). Download **google-services.json** and add the file inside your android>app folder.

  <img src="https://mintcdn.com/suprsend/jhGzZpggWCp1KSgu/images/docs/e2d76a2-Group_6.png?w=1650&fit=max&auto=format&n=jhGzZpggWCp1KSgu&q=85&s=b4b557c2d369b0f39fc0c684e6da773d" alt="Adding google-services.json to the android/app folder" />

### 3. Adding Firebase dependencies and plugins

Add below dependency inside projects `build.gradle` inside dependencies

```groovy
dependencies {
        ...
        classpath 'com.google.gms:google-services:4.3.10' // or latest version
}
```

Add below plugin inside apps _build.gradle_

```groovy
apply plugin: 'com.google.gms.google-services'
```

Add below dependency inside apps _build.gradle_ inside dependencies

```groovy
implementation("com.google.firebase:firebase-messaging:20.2.4") // or latest version
```

### 4. Implementing push

Push feature can be implemented in two ways:

#### Token Generation and Notification handled By SDK [Recommended]

You may use this option if all of your android push notifications are to be handled via SuprSend SDK. We recommend you to use this method as it is just a single step process to just register the service in your application manifest and everything else will be ready.

```xml
<uses-permission android:name="android.permission.INTERNET" />
<!--If you are targeting to API 33 (Android 13) you will additional need to add POST_NOTIFICATIONS -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<service
    android:name="app.suprsend.fcm.SSFirebaseMessagingService"
    android:enabled="true"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

#### Token Generation and Notification handled By Your Application

Use this approach when your app must own **FCM token registration** and **notification rendering** — for example, multiple push providers, to mix Suprsend campaigns with your own push types, silent/data messages.

Firebase allows only **one** `FirebaseMessagingService` per app. Register **your** service in the manifest and **do not** declare `SSFirebaseMessagingService`.

1.  Declare your `FirebaseMessagingService` in `AndroidManifest.xml`.
2.  In `onNewToken`, forward token rotations to Suprsend (initial registration is handled when you call `SuprSend.getInstance()` after `initialize()` and `identify()` — no `FirebaseMessaging.getInstance().token` setup required).
3.  In `onMessageReceived`, branch on payload type: Suprsend vs everything else.

```xml
<uses-permission android:name="android.permission.INTERNET" />
<!--If you are targeting to API 33 (Android 13) you will additional need to add POST_NOTIFICATIONS -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<service
    android:name=".fcm.AppFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

```kotlin
// AppFirebaseMessagingService.kt
import app.suprsend.SuprSend
import app.suprsend.notification.SSNotificationHelper
import app.suprsend.notification.isSuprSendRemoteMessage
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Token rotation only — initial token is registered by the SDK on getInstance().
        SuprSend.getInstance().user.setAndroidFcmPushAsync(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.isSuprSendRemoteMessage()) {
            // Suprsend campaign: SDK helper handles rendering, channels, grouping,
            // deep links, and delivery/click/dismiss tracking.
            SSNotificationHelper.showFCMNotification(applicationContext, remoteMessage)

            // Optional: read extra data keys from your Suprsend template
            // (exclude supr_send_n_pl — that key holds the structured notification JSON).
            // remoteMessage.data["your_custom_key"]?.let { ... }
        } else {
            // Non-Suprsend push: build your own notification or handle data-only payloads.
            val title = remoteMessage.notification?.title ?: remoteMessage.data["title"]
            val body = remoteMessage.notification?.body ?: remoteMessage.data["body"]
            // showYourNotification(title, body, remoteMessage.data)
        }
    }
}
```

| Step                          | API                                                                | Notes                                                                   |
| ----------------------------- | ------------------------------------------------------------------ | ----------------------------------------------------------------------- |
| Detect Suprsend payload       | `remoteMessage.isSuprSendRemoteMessage()`                          | Checks for `supr_send_n_pl` in `RemoteMessage.data`                     |
| Render Suprsend UI            | `SSNotificationHelper.showFCMNotification(context, remoteMessage)` | No-op if the message is not a Suprsend payload                          |
| Register FCM token (rotation) | `user.setAndroidFcmPushAsync(token)` in `onNewToken`               | Initial token: automatic on `SuprSend.getInstance()` after `identify()` |
| Track notification tap        | `SuprSend.getInstance().notificationClicked(...)`                  | After the user opens a Suprsend notification                            |

### Asking for permission - Android 13(API-33)

```kotlin
// YourActivity.kt
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class YourActivity : AppCompatActivity() {

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                // Explain why notification permission matters, then open app settings if denied.
                AlertDialog.Builder(this)
                    .setView(R.layout.notification_permission_desc)
                    .setTitle(getString(R.string.app_name))
                    .setPositiveButton("Proceed") { _, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            data = Uri.fromParts("package", packageName, null)
                        }
                        startActivity(intent)
                    }
                    .setNegativeButton("Deny") { _, _ -> }
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activityResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

}
```

If the androidx dependency is not present then you will have to add the below dependency in your app dependencies

```Gradle
// app/build.gradle.kts
dependencies {
	implementation("androidx.appcompat:appcompat:1.3.1")
}
```
