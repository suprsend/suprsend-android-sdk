package app.suprsend.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import app.suprsend.inbox.model.NotificationModel
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import org.json.JSONArray

fun View.layoutInflater(): LayoutInflater {
    return LayoutInflater.from(context)
}

fun Context.safeStartActivity(intent:Intent?){
    intent?:return
    try {
        startActivity(intent)
    }catch (e:Exception){
        Toast.makeText(this,"Unable to open",Toast.LENGTH_SHORT).show()
    }
}

fun String.getIntent(): Intent {
    return Intent(Intent.ACTION_VIEW, Uri.parse(this))
}

fun setVisibleOrGone(isVisible: Boolean): Int {
    return if (isVisible)
        View.VISIBLE
    else
        View.GONE
}

fun setVisibleOrInvisible(isVisible: Boolean): Int {
    return if (isVisible)
        View.VISIBLE
    else
        View.INVISIBLE
}

fun <T> List<T>.isLast(index: Int): Boolean {
    return index == size - 1
}

fun logInfo(message: String) {
    Log.i("yep", message)
}

fun TextView.prepend(content: String) {
    text = "$content$text"
}

fun <T> JSONArray.convertToList(): MutableList<T> {
    val items = mutableListOf<T>()
    for (i in 0 until length()) {
        items.add(get(i) as T)
    }
    return items
}

// fun NotificationModel.getReadableExpiry(): String {
//    return expiry.toString()
// }

fun readableTimePastTime(timestamp: Long): String {
    val currentTimeMillis = System.currentTimeMillis()
    val elapsedTimeMillis = currentTimeMillis - timestamp

    val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTimeMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTimeMillis)
    val hours = TimeUnit.MILLISECONDS.toHours(elapsedTimeMillis)
    val days = TimeUnit.MILLISECONDS.toDays(elapsedTimeMillis)
    val months = days / 30
    val years = days / 365

    return when {
        years > 0 -> "$years year${if (years > 1) "s" else ""} ago"
        months > 0 -> "$months month${if (months > 1) "s" else ""} ago"
        days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
        hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
        minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
        else -> "$seconds second${if (seconds > 1) "s" else ""} ago"
    }
}

fun isNullOrBlank(text: String?): Boolean {
    return text.isNullOrBlank()
}

fun <T> isEmpty(list: List<T>?): Boolean {
    return list?.isEmpty() == true
}
fun isNull(item: Any?): Boolean {
    return item == null
}

fun getReadableTimestamp(timeStamp: Long): String {
    val currentTime = System.currentTimeMillis()
    val timeDifference = timeStamp - currentTime

    val seconds = TimeUnit.MILLISECONDS.toSeconds(timeDifference)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeDifference)
    val hours = TimeUnit.MILLISECONDS.toHours(timeDifference)
    val days = TimeUnit.MILLISECONDS.toDays(timeDifference)
    val months = ceil(days / 30.0).toInt()
    val years = ceil(days / 365.0).toInt()

    val timeDuration = when {
        years > 0 -> "$years year(s)"
        months > 0 -> "$months month(s)"
        days > 0 -> "$days day(s)"
        hours > 0 -> "$hours hour(s)"
        minutes > 0 -> "$minutes minute(s)"
        else -> "$seconds second(s)"
    }
    return "Expires in $timeDuration"
}

