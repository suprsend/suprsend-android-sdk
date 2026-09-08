# Gson — required by auth0 jwtdecode (JWTDeserializer)
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

# JWT decode
-keep class com.auth0.android.jwt.** { *; }

# Enum.name() must match FCM payloads and prefs API JSON (see KotlinExtensions.mapToEnum)
-keepclassmembers enum app.suprsend.notification.NotificationPriority { *; }
-keepclassmembers enum app.suprsend.notification.NotificationChannelImportance { *; }
-keepclassmembers enum app.suprsend.notification.NotificationChannelVisibility { *; }
-keepclassmembers enum app.suprsend.notification.NotificationActionType { *; }
-keepclassmembers enum app.suprsend.user.preference.PreferenceOptions { *; }
-keepclassmembers enum app.suprsend.feed.APIResponseStatus { *; }

# Feed / inbox public API (Flutter / JNI / reflection)
-keep class app.suprsend.feed.** { *; }

# socket.io-client uses reflection for transports; R8 strips it in release without these.
-keep class io.socket.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn io.socket.**
-dontwarn okhttp3.**
-dontwarn okio.**