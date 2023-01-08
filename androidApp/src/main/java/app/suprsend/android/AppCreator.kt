package app.suprsend.android

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide

object AppCreator {
    const val BASE_IMAGE_SERVER_URL = "https://freeappcreator.in/heruku/images"

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
        val sp = context.getSharedPreferences("main", 0)
        val spedit = sp.edit()
        spedit.putString("email", email)
        spedit.commit()
    }

    fun getEmail(context: Context): String {
        val sp = context.getSharedPreferences("main", 0)
        return sp.getString("email", "") ?: ""
    }

    fun getProductImage(): String {
        return when ((0..6).random()) {
            0 -> "$BASE_IMAGE_SERVER_URL/ecommerce/11461225530026-Antony-Morato-Men-Tshirts-8781461225528839-1.jpg"
            1 -> "$BASE_IMAGE_SERVER_URL/images/ecommerce/11461225529845-Antony-Morato-Men-Tshirts-8781461225528839-2.jpg"
            2 -> "$BASE_IMAGE_SERVER_URL/images/ecommerce/11461226111809-Antony-Morato-Navy-Polo-T-shirt-7711461226111027-1.jpg"
            3 -> "$BASE_IMAGE_SERVER_URL/images/ecommerce/11467807016267-AKS-Red-Printed-Anarkali-Kurta-1501467807016047-2.jpg"
            4 -> "$BASE_IMAGE_SERVER_URL/images/ecommerce/11467807016203-AKS-Red-Printed-Anarkali-Kurta-1501467807016047-5.jpg"
            5 -> "$BASE_IMAGE_SERVER_URL/images/ecommerce/11467807016293-AKS-Red-Printed-Anarkali-Kurta-1501467807016047-1.jpg"
            else -> "$BASE_IMAGE_SERVER_URL/images/ecommerce/11461226111575-Antony-Morato-Navy-Polo-T-shirt-7711461226111027-3.jpg"
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

val Context.defaultSharedPreferences: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(this)

inline fun SharedPreferences.Edit(func: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.func()
    editor.apply()
}
