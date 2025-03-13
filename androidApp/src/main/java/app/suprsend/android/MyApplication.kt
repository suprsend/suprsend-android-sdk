package app.suprsend.android

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import app.suprsend.SSApi
import app.suprsend.fcm.CustomNotificationClickReceiver
import app.suprsend.log.LoggerCallback
import com.google.firebase.crashlytics.FirebaseCrashlytics

class MyApplication : Application() {

    override fun onCreate() {

        SSApi.init(
            context = this,
            apiKey = BuildConfig.SS_TOKEN,
            apiSecret = BuildConfig.SS_SECRET,
            apiBaseUrl = BuildConfig.SS_BASE_URL,
            inboxApiBaseUrl = BuildConfig.SS_INBOX_BASE_URL,
            inboxSocketApiBaseUrl = BuildConfig.SS_INBOX_SOCKET_URL
        )

        super.onCreate()
        AppCreator.context = this

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
        SSApi.renderCollapsedNotificationView { notificationId, data ->
            //Return null if you don't want to render custom collapsed notification
            //If this payload is targeted to render collapsed notification then render collapsed notification here
            if (data.containsKey("collapsed")) {
                val smallView = RemoteViews(packageName, R.layout.custom_collapsed_notification)
                //This is how we can update text
                var title = "New Alert!"
                if(data.containsKey("collapsed_title")){
                    title = data.getValue("collapsed_title")
                }
                smallView.setTextViewText(R.id.tv_title, title)
                //This is how we can handle click
                smallView.setOnClickPendingIntent(
                    R.id.tv_title,
                    PendingIntent.getBroadcast(
                        this,
                        1,
                        Intent(this, CustomNotificationClickReceiver::class.java).apply {
                            action = "ACTION_BUTTON_CLICK"
                            //Notification id is mandatory to be passed for tracking purpose
                            putExtra("id", notificationId)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                return@renderCollapsedNotificationView smallView
            }
            return@renderCollapsedNotificationView null
        }
        SSApi.renderNotificationExpandedView { _, data ->
            //If this payload is targeted to render expanded notification then render expanded notification here
            if (data.containsKey("expanded")) {
                val largeView = RemoteViews(packageName, R.layout.custom_expanded_notification)
                var title = "Expanded Alert"
                if(data.containsKey("expanded_title")){
                    title = data.getValue("expanded_title")
                }
                //This is how we can update text
                largeView.setTextViewText(R.id.expandedTitle, title)
                var desc = "This notification contains extra details when expanded."
                if(data.containsKey("expanded_desc")){
                    desc = data.getValue("expanded_desc")
                }
                //This is how we can update text
                largeView.setTextViewText(R.id.expandedDescription, desc)
                return@renderNotificationExpandedView largeView
            }
            return@renderNotificationExpandedView null
        }

        SSApi.notificationClickedListener { notificationId, data ->
            Log.i("app","notificationClickedListener:id:$notificationId")
        }
    }
}
