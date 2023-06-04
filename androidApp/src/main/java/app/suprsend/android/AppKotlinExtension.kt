package app.suprsend.android

import android.util.Log
import android.view.LayoutInflater
import android.view.View

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
