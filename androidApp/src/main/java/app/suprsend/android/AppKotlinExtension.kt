package app.suprsend.android

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import org.json.JSONObject

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


fun Activity.getThemeJson(): JSONObject? {
    return try {
        defaultSharedPreferences.getString(SettingsActivity.APP_INBOX_THEME, "")?.let {
            if (it.isBlank())
                null
            else
                JSONObject(it)
        }
    } catch (e: Exception) {
        null
    }
}
