package app.suprsend.android

import android.content.Context

object NotificationJson {

    fun getJson(context: Context): String {
        return context.assets.open("notification/notification_sample.json")
            .bufferedReader()
            .use { it.readText() }
    }
}