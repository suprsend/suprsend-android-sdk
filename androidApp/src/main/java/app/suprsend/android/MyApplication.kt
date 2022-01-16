package app.suprsend.android

import android.app.Application
import app.suprsend.SSApi

class MyApplication : Application() {

    override fun onCreate() {

        SSApi.init(context = this)

        super.onCreate()

        SSApi.initXiaomi(context = this.applicationContext, appId = BuildConfig.XIAOMI_APP_ID, apiKey = BuildConfig.XIAOMI_APP_KEY)
        SSApi.initOppo(context = this.applicationContext, appKey = BuildConfig.OPPO_APP_KEY ,appSecret = BuildConfig.OPPO_APP_SECRET)

        initializeSdk()
    }

    private fun initializeSdk() {
        CommonAnalyticsHandler.initialize(applicationContext)
    }
}
