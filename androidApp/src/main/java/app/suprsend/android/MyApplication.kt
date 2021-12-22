package app.suprsend.android

import android.app.Application
import app.suprsend.SSApi

class MyApplication : Application() {

    override fun onCreate() {

        SSApi.init(context = this)

        super.onCreate()

        SSApi.initXiaomi(context = this, appId = BuildConfig.XIAOMI_APP_ID, apiKey = BuildConfig.XIAOMI_APP_KEY)

        initializeSdk()
    }

    private fun initializeSdk() {
        CommonAnalyticsHandler.initialize(applicationContext)
    }
}
