package app.suprsend.android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.suprsend.android.databinding.ActivityPlaceOrderBinding
import org.json.JSONObject

class PlaceOrderActivity : AppCompatActivity() {

    lateinit var binding: ActivityPlaceOrderBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlaceOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val productId =  intent?.getStringExtra("productId")?:"P1"
        val productVo: ProductVo = AppCreator.homeItemsList.find { item -> item.getItemId() == productId } as ProductVo

        binding.paymentModeSp.adapter = getSpinnerAdapter(
            this, arrayListOf(
                "COD",
                "NetBanking",
                "Credit Card",
                "Debit Card"
            )
        )

        binding.placeOrderTv.setOnClickListener {

            val pincode = binding.pinCodeEt.text.toString()
            if (pincode.isBlank()) {
                Toast.makeText(this, "Please enter your picode", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CommonAnalyticsHandler.set(JSONObject().apply {
                put("payment_mode", binding.paymentModeSp.selectedItem.toString())
                put("pincode", pincode)
            })
            val intent = Intent(this, OrderSuccessFullActivity::class.java)
            intent.putExtra("productId", productVo.id)
            startActivity(intent)
            finishAffinity()
        }
    }

    override fun onStart() {
        super.onStart()
        CommonAnalyticsHandler.track("place_order_screen_viewed")
    }
}
