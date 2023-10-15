package app.suprsend.android

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object AppCreator {
    private const val BASE_IMAGE_SERVER_URL = "https://freeappcreator.in/heruku"

    fun loadUrl(context: Context, url: String, imageView: ImageView) {
        Glide.with(context)
            .load(url)
//            .placeholder(R.drawable.placeholder) // any placeholder to load at start
//            .error(R.drawable.imagenotfound)  // any image in case of error
//            .override(200, 200) // resizing
//            .centerCrop()
            .into(imageView)
    }

    fun setEmail(context: Context, email: String) {
        val sp = context.getSharedPreferences("main", Context.MODE_PRIVATE)
        val spedit = sp.edit()
        spedit.putString("email", email)
        spedit.commit()
    }

    fun getEmail(context: Context): String {
        val sp = context.getSharedPreferences("main", Context.MODE_PRIVATE)
        return sp.getString("email", "") ?: ""
    }

    fun getProductImage(): String {
        return when ((0..9).random()) {
            0 -> "https://fakestoreapi.com/img/81fPKd-2AYL._AC_SL1500_.jpg"
            1 -> "https://fakestoreapi.com/img/71-3HjGNDUL._AC_SY879._SX._UX._SY._UY_.jpg"
            2 -> "https://fakestoreapi.com/img/71li-ujtlUL._AC_UX679_.jpg"
            3 -> "https://fakestoreapi.com/img/71YXzeOuslL._AC_UY879_.jpg"
            4 -> "https://fakestoreapi.com/img/71z3kpMAYsL._AC_UY879_.jpg"
            5 -> "https://fakestoreapi.com/img/61sbMiUnoGL._AC_UL640_QL65_ML3_.jpg"
            6 -> "https://fakestoreapi.com/img/81XH0e8fefL._AC_UY879_.jpg"
            7 -> "https://fakestoreapi.com/img/51UDEzMJVpL._AC_UL640_QL65_ML3_.jpg"
            8 -> "https://fakestoreapi.com/img/71HblAHs5xL._AC_UY879_-2.jpg"
            9 -> "https://fakestoreapi.com/img/51Y5NI-I5jL._AC_UX679_.jpg"
            else -> "https://fakestoreapi.com/img/81fPKd-2AYL._AC_SL1500_.jpg"
        }
    }

    fun getBannerImage(index: Int): String {
        return when ((1..2).random()) {
            1 -> "$BASE_IMAGE_SERVER_URL/images/SizeD1200X400.jpg"
            else -> "$BASE_IMAGE_SERVER_URL/images/womens_wear_resized1.jpg"
        }
    }

    val homeItemsList: List<BaseItem> by lazy {
        val list = arrayListOf<BaseItem>()
        list.add(BannerListVo((1..10).map { value ->
            BannerVo("B$value", getBannerImage(value))
        }))
        list.addAll((1..30).map { value ->
            ProductVo(
                id = "P$value",
                url = getProductImage(),
                title = "Product $value",
                amount = (value * 100).toDouble()
            )
        })
        list
    }
}

fun getSpinnerAdapter(context: Context, list: List<String>): ArrayAdapter<String> {
    val spinnerArrayAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
        context,
        android.R.layout.simple_spinner_item,
        list
    )

    spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    return spinnerArrayAdapter
}

fun Activity.myToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

internal val Context.defaultSharedPreferences: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(this)

internal inline fun SharedPreferences.Edit(func: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.func()
    editor.apply()
}

internal fun Activity.storeInSp(key: String, value: String) {
    defaultSharedPreferences.Edit {
        putString(key, value)
    }
}

internal fun Activity.getFromSp(key: String, default: String = ""): String {
    return defaultSharedPreferences.getString(key, default) ?: ""
}

internal val demoAppExecutorService: ExecutorService by lazy { Executors.newFixedThreadPool(1) }

internal fun makeGetCall(urlStr: String): String {
    val url = URL(urlStr)
    val connection: HttpURLConnection = url.openConnection() as HttpURLConnection

    var response = ""
    try {
        val br = BufferedReader(InputStreamReader(connection.inputStream))
        val sb = StringBuilder()
        var line: String?
        while (br.readLine().also { line = it } != null) {
            sb.append(line).append('\n')
        }
        response = sb.toString()
    } finally {
        connection.disconnect()
    }
    return response
}
