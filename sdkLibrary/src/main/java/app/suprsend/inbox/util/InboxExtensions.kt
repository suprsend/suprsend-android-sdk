package app.suprsend.inbox.util

import kotlin.math.abs
import kotlin.math.roundToInt

fun getReadableTime(timestampMillis: Long, anchorTimeMillis: Long = System.currentTimeMillis()): String {
    val timeDifference = timestampMillis - anchorTimeMillis
    val positiveTimeDifference = abs(timeDifference)

    val seconds = (positiveTimeDifference / 1000).toInt()
    val minutes = (positiveTimeDifference / (1000 * 60)).toInt()
    val hours = (positiveTimeDifference / (1000 * 60 * 60)).toInt()
    val days = (positiveTimeDifference / (1000 * 60 * 60 * 24)).toInt()
    val months = (days / 30.0).roundToInt()
    val years = (days / 365.0).roundToInt()

    val readableTime = when {
        years > 0 -> pluralize(years, "year")
        months > 0 -> pluralize(months, "month")
        days > 0 -> pluralize(days, "day")
        hours > 0 -> pluralize(hours, "hour")
        minutes > 0 -> pluralize(minutes, "min")
        else -> pluralize(seconds, "sec")
    }

    return if (timeDifference >= 0) {
        readableTime
    } else {
        "$readableTime ago"
    }
}

fun todayMidNightMilli(): Long {
    val currentTimeMillis = System.currentTimeMillis()
    return currentTimeMillis - currentTimeMillis % (24 * 60 * 60 * 1000)
}

fun pluralize(value: Int, unit: String): String {
    return "$value ${unit}${if (value != 1) "(s)" else ""}"
}
