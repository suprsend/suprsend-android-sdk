package app.suprsend

import android.os.Build

data class AppInfo(
    var name: String,
    var version: String
)

data class ClientInfo(
    val sdk: String = "suprsend-android-sdk",
    val sdkVersion: String = BuildConfig.SS_SDK_VERSION_NAME,
    val lang: String = "kotlin",
    val langVersion: String = "disabled",
    val platform: String = "android",
    val environment: String = "mobile",
    val os: String = "android",
    val osVersion: String = "${Build.VERSION.RELEASE}(${Build.VERSION.SDK_INT})",
    val deviceModel: String = "${Build.MANUFACTURER}(${Build.MODEL})",
    var appInfo: AppInfo? = null
)
