package app.suprsend.android

import android.app.Application
import android.util.Log
import app.suprsend.NotificationCallbackListener
import app.suprsend.SuprSend
import app.suprsend.UserTokenFetcher
import app.suprsend.base.NetworkClient
import app.suprsend.log.LoggerCallback
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONObject
import java.net.URLEncoder

class MyApplication : Application() {

    override fun onCreate() {


        SuprSend.initialize(
            context = this,
            publicApiKey = BuildConfig.SS_PUBLIC_API_KEY,
            baseUrl = BuildConfig.SS_BASE_URL
        )
        CommonAnalyticsHandler.initialize(this)

        val jwtTokenBoolean = defaultSharedPreferences.getBoolean("jwtToken", true)
        if (jwtTokenBoolean) {
            SuprSend.setUserTokenFetcher(UserTokenFetcherImpl())
        } else {
            SuprSend.setUserTokenFetcher(null)
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
            override fun onPushPayloadReceived(data: Map<String, String>) {
                Log.i(AppConstants.TAG, "onPushPayloadReceived : $data")
            }

        })
    }
}

class UserTokenFetcherImpl : UserTokenFetcher {

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