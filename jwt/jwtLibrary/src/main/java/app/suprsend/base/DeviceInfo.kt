package app.suprsend.base

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import app.suprsend.BuildConfig
import app.suprsend.SSInternal
import app.suprsend.log.Logger
import org.json.JSONObject
import java.util.Locale

internal object DeviceInfo {
    fun addDeviceInfoProperties(jsonObject: JSONObject): JSONObject {
        jsonObject.put(SSConstants.APP_VERSION_STRING, versionName())
        jsonObject.put(SSConstants.APP_BUILD_NUMBER, versionCode())
        jsonObject.put(SSConstants.OS, os())
        jsonObject.put(SSConstants.MANUFACTURER, manufacturer())
        jsonObject.put(SSConstants.BRAND, brand())
        jsonObject.put(SSConstants.MODEL, model())
        jsonObject.put(SSConstants.DEVICE_ID, getDeviceId())
        val ssSdkVersion = "android-" + BuildConfig.SS_SDK_TYPE.toLowerCase(Locale.getDefault()) + "/" + BuildConfig.SS_SDK_VERSION_NAME
        jsonObject.put(SSConstants.SS_SDK_VERSION, ssSdkVersion)
        jsonObject.put(SSConstants.NETWORK, NetworkInfo.getNetworkType().readableName)
        jsonObject.put(SSConstants.CONNECTED, NetworkInfo.isConnected().toString())
        return jsonObject
    }

    fun getDeviceWidthPixel(): Int {
        return SSInternal.context.resources.displayMetrics.widthPixels
    }

    fun getDeviceHeightPixels(): Int {
        return SSInternal.context.resources.displayMetrics.heightPixels
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(): String {
        return Settings.Secure.getString(SSInternal.context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun versionName(): String {
        return try {
            val info: PackageInfo = SSInternal.context.packageManager.getPackageInfo(SSInternal.context.packageName, 0)
            info.versionName ?: UNKNOWN
        } catch (e: PackageManager.NameNotFoundException) {
            Logger.i(SSConstants.TAG_SUPRSEND, "Unable to get app version details")
            UNKNOWN
        }
    }

    private fun versionCode(): String {
        return try {
            val info: PackageInfo = SSInternal.context.packageManager.getPackageInfo(SSInternal.context.packageName, 0)
            info.versionCode.toString()
        } catch (e: PackageManager.NameNotFoundException) {
            Logger.i(SSConstants.TAG_SUPRSEND, "Unable to get app version details")
            UNKNOWN
        }
    }

    private fun brand(): String {
        return if (Build.BRAND == null) UNKNOWN else Build.BRAND
    }

    private fun manufacturer(): String {
        return if (Build.MANUFACTURER == null) UNKNOWN else Build.MANUFACTURER
    }

    private fun model(): String {
        return if (Build.MODEL == null) UNKNOWN else Build.MODEL
    }

    private fun os(): String {
        return "android"
    }

    private const val UNKNOWN = "UNKNOWN"
}