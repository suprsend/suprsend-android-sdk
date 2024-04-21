package app.suprsend.android

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import app.suprsend.inbox.model.NotificationModel
import org.json.JSONArray
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

fun View.layoutInflater(): LayoutInflater {
    return LayoutInflater.from(context)
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

fun TextView.prepend(content:String){
    text = "$content$text"
}

fun<T> JSONArray.convertToList(): MutableList<T> {
    val items= mutableListOf<T>()
    for(i in 0 until length()){
        items.add(get(i) as T)
    }
    return items
}

//fun NotificationModel.getReadableExpiry(): String {
//    return expiry.toString()
//}

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

fun getReadableTimestamp(timeStamp:Long): String {
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
    return  "Expires in $timeDuration"
}
fun NotificationModel.isUnreadNotification(): Boolean {
    return seenOn == null
}