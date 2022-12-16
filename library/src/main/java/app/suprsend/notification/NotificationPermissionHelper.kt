package app.suprsend.notification

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar

object NotificationPermissionHelper {


    fun Activity.requestNotificationPermission(
        requestCode: Int,
        snackBarDescription: String = "Notification is blocked",
        snackBarActionTitle: String = "Settings"
    ) {
        val notificationManagerCompat = NotificationManagerCompat.from(this)
        Log.i("permission", "areNotificationsEnabled : ${notificationManagerCompat.areNotificationsEnabled()}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    Log.e("permission", "Permission is granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Snackbar
                        .make(findViewById(android.R.id.content), snackBarDescription, Snackbar.LENGTH_LONG)
                        .setAction(snackBarActionTitle) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            val uri: Uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                        .show()
                }
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), requestCode)
                    }
                }
            }
        }
    }
}