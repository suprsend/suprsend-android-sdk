package app.suprsend.fcm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import app.suprsend.SSApi

class CustomNotificationClickReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "ACTION_BUTTON_CLICK") {
            val notificationId = intent.getStringExtra("id") ?: ""
            SSApi.getInstance().notificationClicked(notificationId)
            Toast.makeText(context, "Notification Button Clicked ${notificationId}!", Toast.LENGTH_SHORT).show()
        }
    }

}