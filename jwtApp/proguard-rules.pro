# SuprSend Sdk
-dontwarn app.suprsend.**
-keep class app.suprsend.**{*;}

# JWT needs this
-keep class com.auth0.android.jwt.** { *; }
-keep class sun.misc.** { *; }
-keep class com.google.gson.** { *; }