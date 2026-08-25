package app.suprsend.android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import io.noties.markwon.Markwon

@SuppressLint("StaticFieldLeak")
object AppCreator {
    lateinit var context: Context
    var inboxThemeConfig: InboxThemeConfig = InboxThemeConfig()

    val markWon: Markwon by lazy { Markwon.create(context) }

    fun getTenantId(): String? {
        return getValue(AppConstants.PREF_TENANT_ID, BuildConfig.SS_TENANT_ID).takeIf { it.isNotBlank() }
    }

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
            0 -> "https://cdn.dummyjson.com/product-images/smartphones/iphone-13-pro/1.webp"
            1 -> "https://cdn.dummyjson.com/product-images/laptops/apple-macbook-pro-14-inch-space-grey/1.webp"
            2 -> "https://cdn.dummyjson.com/product-images/mens-shirts/man-plaid-shirt/1.webp"
            3 -> "https://cdn.dummyjson.com/product-images/mens-shoes/nike-air-jordan-1-red-and-black/1.webp"
            4 -> "https://cdn.dummyjson.com/product-images/sunglasses/classic-sun-glasses/1.webp"
            5 -> "https://cdn.dummyjson.com/product-images/womens-dresses/corset-leather-with-skirt/1.webp"
            6 -> "https://cdn.dummyjson.com/product-images/womens-bags/prada-women-bag/1.webp"
            7 -> "https://cdn.dummyjson.com/product-images/beauty/red-lipstick/1.webp"
            8 -> "https://cdn.dummyjson.com/product-images/mens-watches/brown-leather-belt-watch/1.webp"
            9 -> "https://cdn.dummyjson.com/product-images/womens-jewellery/green-oval-earring/1.webp"
            else -> "https://cdn.dummyjson.com/product-images/smartphones/iphone-13-pro/1.webp"
        }
    }

    fun getBannerImage(index: Int): String {
        return when ((1..2).random()) {
            1 -> "https://picsum.photos/id/1015/1200/400"
            else -> "https://picsum.photos/id/1018/1200/400"
        }
    }

    val homeItemsList: List<BaseItem> by lazy {
        val list = arrayListOf<BaseItem>()
        list.add(BannerListVo((1..10).map { value ->
            BannerVo("B$value", getProductImage())
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

    fun getInboxStoreJson(context: Context): String {
        return context.readStringFromAsset("inbox_stores.json")
    }

    fun getValue(key: String, default: String = ""): String {
        return context.defaultSharedPreferences.getString(key, default) ?: ""
    }

    fun storeValue(key: String, value: String) {
        context.defaultSharedPreferences.Edit {
            putString(key, value)
        }
    }

    fun startInboxActivity(activity: Activity) {

        CommonAnalyticsHandler.initializeInbox()

        val intent = Intent(activity, SSInboxActivity::class.java)
        activity.startActivity(intent)
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

internal val Context.defaultSharedPreferences: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(this)

internal inline fun SharedPreferences.Edit(func: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.func()
    editor.apply()
}

fun TextView.setMarkDownText(markdownText: String?) {
    markdownText ?: return
    AppCreator.markWon.setMarkdown(this, markdownText)
}
