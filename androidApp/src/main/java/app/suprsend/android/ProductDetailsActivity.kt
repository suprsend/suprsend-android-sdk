package app.suprsend.android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import app.suprsend.android.databinding.ActivityProductDetailsBinding
import org.json.JSONObject

class ProductDetailsActivity : AppCompatActivity() {

    lateinit var binding: ActivityProductDetailsBinding

    lateinit var productVo: ProductVo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppCreator.getEmail(this).isBlank()) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finishAffinity()
            return
        }

        binding = ActivityProductDetailsBinding.inflate(layoutInflater)

        setContentView(binding.root)

        var productId = intent?.getStringExtra("productId") ?: "P1"

        if (intent.action == Intent.ACTION_VIEW) {
            productId = intent.data?.getQueryParameter("productId") ?: "P1"
        }

        loadProduct(productId)

        binding.buyNow.setOnClickListener {
            val intent = Intent(this, PlaceOrderActivity::class.java)
            intent.putExtra("productId", productVo.id)
            startActivity(intent)
        }
        binding.thumbUp.setOnClickListener {
            myToast("Called : append : choices")
            CommonAnalyticsHandler.append("choices", productVo.title)
        }
        binding.thumbDown.setOnClickListener {
            myToast("Called : remove : choices")
            CommonAnalyticsHandler.remove("choices", productVo.title)
        }
    }

    override fun onNewIntent(newIntent: Intent?) {
        super.onNewIntent(newIntent)
        var productId = newIntent?.getStringExtra("productId") ?: "P1"

        if (newIntent?.action == Intent.ACTION_VIEW) {
            productId = newIntent.data?.getQueryParameter("productId") ?: "P1"
        }

        loadProduct(productId)
    }

    private fun loadProduct(productId: String) {
        productVo = AppCreator.homeItemsList.find { item -> item.getItemId() == productId } as ProductVo
        title = productVo.title
        binding.obj = productVo

        AppCreator.loadUrl(this, productVo.url, binding.imageIV)

        binding.executePendingBindings()

        CommonAnalyticsHandler.track("product_viewed", JSONObject().apply {
            put("Product ID", productVo.id)
            put("Product Name", productVo.title)
            put("Amount", productVo.amount)
        })
    }
}
