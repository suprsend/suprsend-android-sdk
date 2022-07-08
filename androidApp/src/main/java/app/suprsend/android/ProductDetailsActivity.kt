package app.suprsend.android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import app.suprsend.android.databinding.ActivityProductDetailsBinding
import app.suprsend.inbox.SSInboxActivity
import app.suprsend.inbox.SSInboxConfig
import org.json.JSONObject

class ProductDetailsActivity : AppCompatActivity() {

    lateinit var binding: ActivityProductDetailsBinding
    lateinit var productVo: ProductVo
    lateinit var ssInboxConfig: SSInboxConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ssInboxConfig = getThemeJson()?.let { SSInboxConfig(it) } ?: SSInboxConfig()
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

        binding
            .inboxBellView
            .initialize(
                distinctId = AppCreator.getEmail(this),
                // This is generated here just for testing purpose we do not recommend doing this in mobile app instead this should be generated on server
                // We should not keep inbox secret at mobile end
                subscriberId = HmacGeneratation()
                    .hmacRawURLSafeBase64String(
                        AppCreator.getEmail(this),
                        BuildConfig.INBOX_SECRET
                    ),
                ssInboxConfig = ssInboxConfig
            )
        binding
            .inboxBellView
            .setOnClickListener {
                val intent = Intent(this, SSInboxActivity::class.java)
                intent.putExtra(SSInboxActivity.DISTINCT_ID, AppCreator.getEmail(this))
                intent.putExtra(
                    SSInboxActivity.SUBSCRIBER_ID,
                    HmacGeneratation()
                        .hmacRawURLSafeBase64String(
                            AppCreator.getEmail(this),
                            BuildConfig.INBOX_SECRET
                        )
                )
                intent.putExtra(SSInboxActivity.CONFIG, ssInboxConfig)
                startActivity(intent)
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

    override fun onStart() {
        super.onStart()
        binding.inboxBellView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.inboxBellView.onStop()
    }

    private fun loadProduct(productId: String) {
        productVo = AppCreator.homeItemsList.find { item -> item.getItemId() == productId } as ProductVo

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
