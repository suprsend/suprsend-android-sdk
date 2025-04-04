# SuprSend Sdk
-dontwarn app.suprsend.**
-keep class app.suprsend.**{*;}

# JWT needs this to generate token only required for testing
# Idealy this will be generated at server
-keep class com.auth0.android.jwt.** { *; }
-keep class sun.misc.** { *; }
-keep class com.google.gson.** { *; }