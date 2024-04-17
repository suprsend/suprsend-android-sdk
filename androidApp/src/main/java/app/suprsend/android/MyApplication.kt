package app.suprsend.android

import android.app.Application
import app.suprsend.SSApi
import app.suprsend.log.LoggerCallback
import com.google.firebase.crashlytics.FirebaseCrashlytics

class MyApplication : Application() {

    override fun onCreate() {

        SSApi.init(
            context = this,
            apiKey = BuildConfig.SS_TOKEN,
            apiSecret = BuildConfig.SS_SECRET,
            apiBaseUrl = BuildConfig.SS_API_BASE_URL,
            inboxApiBaseUrl = BuildConfig.SS_API_INBOX_BASE_URL,
            inboxSocketApiBaseUrl = BuildConfig.SS_API_INBOX_SOCKET_URL
        )

        super.onCreate()

        SSApi.initXiaomi(context = this, appId = BuildConfig.XIAOMI_APP_ID, apiKey = BuildConfig.XIAOMI_APP_KEY)
        SSApi.setLogger(object : LoggerCallback {
            override fun i(tag: String, message: String) {
                // you will receive sdk info messages here
            }

            override fun e(tag: String, message: String, throwable: Throwable?) {
                throwable ?: return
                FirebaseCrashlytics.getInstance().recordException(throwable)
            }
        })
    }
}
