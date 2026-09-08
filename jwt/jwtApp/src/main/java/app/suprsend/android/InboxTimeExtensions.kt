package app.suprsend.android

import java.text.DateFormat
import java.util.Date
import kotlin.math.max

/** Matches iOS `formatRelative(from:)` in InboxScreen. */
fun formatRelative(timestamp: Double): String {
    val secondsEpoch = if (timestamp > 1_000_000_000_000.0) timestamp / 1000.0 else timestamp
    val dateMillis = (secondsEpoch * 1000.0).toLong()
    val diff = max(1, ((System.currentTimeMillis() - dateMillis) / 1000L).toInt())
    if (diff < 60) return "${diff}s ago"
    val m = diff / 60
    if (m < 60) return "${m}m ago"
    val h = m / 60
    if (h < 24) return "${h}h ago"
    val d = h / 24
    if (d < 7) return "${d}d ago"
    return DateFormat.getDateInstance(DateFormat.SHORT).format(Date(dateMillis))
}
