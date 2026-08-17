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