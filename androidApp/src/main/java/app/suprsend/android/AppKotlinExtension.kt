package app.suprsend.android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.Date
import org.json.JSONArray

fun View.layoutInflater(): LayoutInflater {
    return LayoutInflater.from(context)
}

fun setVisibleOrGone(isVisible: Boolean): Int {
    return if (isVisible)
        View.VISIBLE
    else
        View.GONE
}

fun <T> List<T>.isLast(index: Int): Boolean {
    return index == size - 1
}

fun logInfo(message: String) {
    Log.i(AppConstants.TAG, message)
}

fun isNull(item: Any?): Boolean {
    return item == null || (item.toString().toIntOrNull() == 0)
}

fun Boolean?.isTrue(): Boolean {
    return this == true
}

@SuppressLint("SimpleDateFormat")
fun getReadableDate(date: Date = Date()): String {
    return SimpleDateFormat("dd-MM-yyyy").format(date)
}

fun Context.readStringFromAsset(fileName: String): String {
    val inputStream = assets.open(fileName)
    return inputStream.bufferedReader().use(BufferedReader::readText)
}

fun Activity.myToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Fragment.myToast(message: String) {
    Toast.makeText(this.context, message, Toast.LENGTH_SHORT).show()
}

internal fun safeDrawable(resources: Resources, drawableId: Int, theme: Resources.Theme? = null): Drawable? {
    try {
        return ResourcesCompat.getDrawable(resources, drawableId, theme)
    } catch (e: Exception) {
    }
    return null
}

internal fun Parcel.safeString(): String {
    return readString() ?: ""
}

internal fun TabLayout.forEachTab(callMe: (TabLayout.Tab) -> Unit) {
    for (i in 0 until tabCount) {
        getTabAt(i)?.let { tab ->
            callMe(tab)
        }
    }
}

@SuppressLint("MissingPermission")
fun Context.isConnected(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetworkInfo
    return activeNetwork != null && activeNetwork.isConnectedOrConnecting
}

fun View.setVisible(isVisible: Boolean) {
    if (isVisible) {
        this.visibility = View.VISIBLE
    } else {
        this.visibility = View.GONE
    }
}

fun Context.safeStartActivity(intent: Intent?) {
    intent ?: return
    try {
        startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(this, "Unable to open", Toast.LENGTH_SHORT).show()
    }
}

fun safeHtml(htmlText: String?): Spanned? {
    htmlText ?: return null
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
    } else {
        Html.fromHtml(htmlText)
    }
}

fun String.getIntent(): Intent {
    return Intent(Intent.ACTION_VIEW, Uri.parse(this))
}

fun setVisibleOrInvisible(isVisible: Boolean): Int {
    return if (isVisible)
        View.VISIBLE
    else
        View.INVISIBLE
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

fun isNullOrBlank(text: String?): Boolean {
    return text.isNullOrBlank()
}

fun <T> isEmpty(list: List<T>?): Boolean {
    return list?.isEmpty() == true
}
