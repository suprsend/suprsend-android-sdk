package app.suprsend.android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import app.suprsend.android.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val layoutManager = GridLayoutManager(this, 2)

        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return getSpanCount(position)
            }
        }
        binding.recyclerView.layoutManager = layoutManager

        binding.recyclerView.adapter = HomeRecyclerAdapter(AppCreator.homeItemsList)
        binding.settingsTv.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        CommonAnalyticsHandler.track("home_screen_viewed")
    }

    private fun getSpanCount(position: Int): Int {
        return if (position == 0)
            2
        else 1
    }
}
