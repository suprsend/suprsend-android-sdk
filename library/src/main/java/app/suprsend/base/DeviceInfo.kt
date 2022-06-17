package app.suprsend.base

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import app.suprsend.BuildConfig
import org.json.JSONObject
import java.util.Locale

internal class DeviceInfo {
    fun getDeviceInfoProperties(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put(SSConstants.APP_VERSION_STRING, versionName())
        jsonObject.put(SSConstants.APP_BUILD_NUMBER, versionCode())
        jsonObject.put(SSConstants.OS, os())
        jsonObject.put(SSConstants.MANUFACTURER, manufacturer())
        jsonObject.put(SSConstants.BRAND, brand())
        jsonObject.put(SSConstants.MODEL, model())
        jsonObject.put(SSConstants.DEVICE_ID, getDeviceId())
        val ssSdkVersion  = "android-"+BuildConfig.SS_SDK_TYPE.toLowerCase(Locale.getDefault())+"/"+BuildConfig.SS_SDK_VERSION_NAME
        jsonObject.put(SSConstants.SS_SDK_VERSION, ssSdkVersion)
        val networkInfo = SdkAndroidCreator.networkInfo
        jsonObject.put(SSConstants.NETWORK, networkInfo.getNetworkType().readableName)
        jsonObject.put(SSConstants.CONNECTED, networkInfo.isConnected().toString())
        return jsonObject
    }

    fun getDeviceWidthPixel(): Int {
        return SdkAndroidCreator.context.resources.displayMetrics.widthPixels
    }

    fun getDeviceHeightPixels(): Int {
        return SdkAndroidCreator.context.resources.displayMetrics.heightPixels
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(): String {
        return Settings.Secure.getString(SdkAndroidCreator.context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun versionName(): String {
        return try {
            val info: PackageInfo = SdkAndroidCreator.context.packageManager.getPackageInfo(SdkAndroidCreator.context.packageName, 0)
            info.versionName ?: UNKNOWN
        } catch (e: PackageManager.NameNotFoundException) {
            Logger.i("init", "Unable to get app version details")
            UNKNOWN
        }
    }

    private fun versionCode(): String {
        return try {
            val info: PackageInfo = SdkAndroidCreator.context.packageManager.getPackageInfo(SdkAndroidCreator.context.packageName, 0)
            info.versionCode.toString()
        } catch (e: PackageManager.NameNotFoundException) {
            Logger.i("init", "Unable to get app version details")
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

    companion object {
        const val UNKNOWN = "UNKNOWN"
    }
}

private enum class BuildType {
    NATIVE, RN, FLUTTER
}
