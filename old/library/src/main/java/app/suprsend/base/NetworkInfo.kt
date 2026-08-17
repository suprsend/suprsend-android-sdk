package app.suprsend.base

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.telephony.TelephonyManager

internal class NetworkInfo {

    fun getNetworkType(): NetworkType {
        val context = SdkAndroidCreator.context
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info = cm.activeNetworkInfo

        if (info == null || !info.isConnected)
            return NetworkType.UNKNOWN // not connected

        if (info.type == ConnectivityManager.TYPE_WIFI)
            return NetworkType.WIFI

        if (info.type == ConnectivityManager.TYPE_MOBILE) {
            return when (info.subtype) {
                TelephonyManager.NETWORK_TYPE_GPRS,
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_CDMA,
                TelephonyManager.NETWORK_TYPE_1xRTT,
                TelephonyManager.NETWORK_TYPE_IDEN,
                TelephonyManager.NETWORK_TYPE_GSM -> NetworkType.G2

                TelephonyManager.NETWORK_TYPE_UMTS,
                TelephonyManager.NETWORK_TYPE_EVDO_0,
                TelephonyManager.NETWORK_TYPE_EVDO_A,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_EVDO_B,
                TelephonyManager.NETWORK_TYPE_EHRPD,
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_TD_SCDMA -> NetworkType.G3

                TelephonyManager.NETWORK_TYPE_LTE,
                TelephonyManager.NETWORK_TYPE_IWLAN,
                19 -> NetworkType.G4 // LTE_CA
                20 -> NetworkType.G5 // api<29: replace by 20
                else -> NetworkType.UNKNOWN
            }
        }
        return NetworkType.UNKNOWN
    }

    @SuppressLint("MissingPermission")
    fun isConnected(): Boolean {
        val context = SdkAndroidCreator.context
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }
}

enum class NetworkType(val readableName: String) {
    WIFI("wifi"),
    G2("2G"),
    G3("3G"),
    G4("4G"),
    G5("5G"),
    UNKNOWN("-")
}
