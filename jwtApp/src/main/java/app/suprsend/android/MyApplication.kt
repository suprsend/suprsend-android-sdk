package app.suprsend.android

import android.app.Application
import android.content.Context
import android.util.Log
import app.suprsend.AppInfo
import app.suprsend.NotificationCallbackListener
import app.suprsend.RefreshUserTokenCallback
import app.suprsend.SuprSend
import app.suprsend.base.NetworkClient
import app.suprsend.log.LogLevel
import app.suprsend.log.LoggerCallback
import app.suprsend.notification.NotificationActionVo
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONObject
import java.net.URLEncoder

class MyApplication : Application() {

    override fun onCreate() {

        SuprSend.initialize(
            context = this,
            publicApiKey = BuildConfig.SS_PUBLIC_API_KEY,
            host = BuildConfig.SS_BASE_URL,
            appInfo = AppInfo(
                name = "Ecommerce",
                version = BuildConfig.VERSION_NAME
            )
        )

        SuprSend.getInstance().setLogLevel(LogLevel.VERBOSE)

        val jwtTokenBoolean = defaultSharedPreferences.getBoolean("jwtToken", true)
        if (jwtTokenBoolean) {
            SuprSend.setRefreshUserToken(RefreshUserTokenCallbackImpl())
        } else {
            SuprSend.setRefreshUserToken(null)
        }

        super.onCreate()
        AppCreator.context = this

        SuprSend.setLogger(object : LoggerCallback {
            override fun v(tag: String, message: String) {
                // you will receive sdk logs here
            }
            override fun i(tag: String, message: String) {
                // you will receive sdk info logs here
            }

            override fun e(tag: String, message: String, throwable: Throwable?) {
                throwable ?: return
                FirebaseCrashlytics.getInstance().recordException(throwable)
            }
        })

        SuprSend.setNotificationCallback(object : NotificationCallbackListener {
            override fun onPushPayloadReceived(context: Context,data: Map<String, String>) {
                Log.i(AppConstants.TAG, "onPushPayloadReceived : $data")
            }

            override fun onNotificationClicked(notificationActionVo: NotificationActionVo, data: Map<String, String>) {
                Log.i(
                    AppConstants.TAG,
                    "onNotificationClicked : id=${notificationActionVo.notificationId}, link=${notificationActionVo.link}, actionType=${notificationActionVo.notificationActionType}, data=$data"
                )
            }
        })
    }
}

class RefreshUserTokenCallbackImpl : RefreshUserTokenCallback {

    private val networkClient = NetworkClient()

    override fun getToken(distinctId: String): String {
        return try {
            val response = networkClient.httpCall(
                requestMethod = "GET",
                url = "${BuildConfig.SS_BASE_URL}/authentication-token/${URLEncoder.encode(distinctId, "utf-8")}"
            )
            val responseJo = JSONObject(response.body ?: "{}")
            val token = responseJo.optString("token")
            Log.i(AppConstants.TAG, "Token Received $token")
            token
        } catch (e: Exception) {
            ""
        }
    }
}