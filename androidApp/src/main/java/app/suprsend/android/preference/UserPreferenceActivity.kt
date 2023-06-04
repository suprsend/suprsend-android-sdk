package app.suprsend.android.preference

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import app.suprsend.SSApi
import app.suprsend.android.databinding.UserPreferenceActivityBinding
import app.suprsend.android.isLast
import app.suprsend.android.myToast
import app.suprsend.exception.NoInternetException
import app.suprsend.user.preference.PreferenceCallback
import app.suprsend.user.preference.PreferenceData
import app.suprsend.user.preference.PreferenceOptions
import app.suprsend.user.preference.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserPreferenceActivity : AppCompatActivity() {

    lateinit var binding: UserPreferenceActivityBinding

    lateinit var adapter: UserPreferenceRecyclerViewAdapter

    lateinit var preferences: Preferences
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Preferences"
        binding = UserPreferenceActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.categoriesRV.layoutManager = LinearLayoutManager(this@UserPreferenceActivity)
        adapter = UserPreferenceRecyclerViewAdapter({ category, checked ->
            coroutineScope.launch {
                val response = SSApi.getInstance().getUser().getPreferences().updateCategoryPreference(
                    category = category,
                    preference = PreferenceOptions.from(checked)
                )
                if (response.getException() is NoInternetException) {
                    withContext(Dispatchers.Main) {
                        myToast("Please check internet connection")
                    }
                }
            }
        }, { category, channel, checked ->
            coroutineScope.launch {
                val response = SSApi.getInstance().getUser().getPreferences().updateChannelPreferenceInCategory(
                    category = category,
                    channel = channel,
                    preference = PreferenceOptions.from(checked)
                )
                if (response.getException() is NoInternetException) {
                    withContext(Dispatchers.Main) {
                        myToast("Please check internet connection")
                    }
                }
            }
        })
        binding.categoriesRV.adapter = adapter

        preferences = SSApi.getInstance().getUser().getPreferences()

        coroutineScope.launch {
            val data = preferences.fetchUserPreference().getData() ?: return@launch
            showData(data)
        }
    }

    override fun onResume() {
        super.onResume()
        preferences.registerCallback(object : PreferenceCallback {
            override fun onUpdate() {
                coroutineScope.launch {
                    val data = preferences.fetchUserPreference(fetchRemote = false).getData() ?: return@launch
                    showData(data)
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        preferences.unRegisterCallback()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    private suspend fun showData(preferenceData: PreferenceData) {
        withContext(Dispatchers.Main) {
            adapter.setItems(preferenceData.toUIItems())
        }
    }

    private fun PreferenceData?.toUIItems(): List<RecyclerViewItem> {
        if (this == null) return listOf()
        val itemsList = arrayListOf<RecyclerViewItem>()
        sections.forEachIndexed { sIndex, section ->
            if (section.name.isNotBlank())
                itemsList.add(RecyclerViewItem.SectionVo(section))
            section.subCategories.forEachIndexed { scIndex, subCategory ->
                itemsList.add(RecyclerViewItem.SubCategoryVo(subCategory, section.subCategories.isLast(scIndex)))
            }
        }
        return itemsList
    }
}
