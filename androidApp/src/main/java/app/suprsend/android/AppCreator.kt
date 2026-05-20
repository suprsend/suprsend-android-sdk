package app.suprsend.android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import io.noties.markwon.Markwon

@SuppressLint("StaticFieldLeak")
object AppCreator {
    private const val BASE_IMAGE_SERVER_URL = "https://freeappcreator.in/heruku"

    lateinit var context: Context

    val markWon: Markwon by lazy { Markwon.create(context) }

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
            else -> "https://images.pexels.com/photos/5240020/pexels-photo-5240020.jpeg"
        }
    }

    fun getBannerImage(index: Int): String {
        return when ((1..2).random()) {
            1 -> "https://images.pexels.com/photos/159045/the-interior-of-the-repair-interior-design-159045.jpeg"
            else -> "https://images.pexels.com/photos/276267/pexels-photo-276267.jpeg"
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

val Context.defaultSharedPreferences: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(this)

inline fun SharedPreferences.Edit(func: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.func()
    editor.apply()
}

fun TextView.setMarkDownText(markdownText: String?) {
    markdownText ?: return
    AppCreator.markWon.setMarkdown(this, markdownText)
}
