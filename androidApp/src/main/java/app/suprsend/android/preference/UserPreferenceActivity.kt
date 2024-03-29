package app.suprsend.android.preference

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import app.suprsend.SSApi
import app.suprsend.android.BuildConfig
import app.suprsend.android.databinding.UserPreferenceActivityBinding
import app.suprsend.android.isLast
import app.suprsend.android.logInfo
import app.suprsend.android.myToast
import app.suprsend.exception.NoInternetException
import app.suprsend.user.preference.ChannelPreferenceOptions
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

    private val expandedIds = hashMapOf<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Preferences"
        binding = UserPreferenceActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.categoriesRV.layoutManager = LinearLayoutManager(this@UserPreferenceActivity)
        adapter = UserPreferenceRecyclerViewAdapter(categoryItemClick = { category, checked ->
            coroutineScope.launch {
                val response = SSApi.getInstance().getUser().getPreferences().updateCategoryPreference(
                    category = category,
                    preference = PreferenceOptions.from(checked),
                    tenantId = BuildConfig.SS_TENANT_ID
                )
                if (response.getException() is NoInternetException) {
                    withContext(Dispatchers.Main) {
                        myToast("Please check internet connection")
                    }
                }
            }
        }, channelItemClick = { category, channel, checked ->
            coroutineScope.launch {
                val response = SSApi.getInstance().getUser().getPreferences().updateChannelPreferenceInCategory(
                    category = category,
                    channel = channel,
                    preference = PreferenceOptions.from(checked),
                    tenantId = BuildConfig.SS_TENANT_ID
                )
                if (response.getException() is NoInternetException) {
                    withContext(Dispatchers.Main) {
                        myToast("Please check internet connection")
                    }
                }
            }
        },
            channelPreferenceArrowClick = { category, expanded ->
                expandedIds[category] = expanded
            },
            channelPreferenceChangeClick = { channel: String, channelPreferenceOptions: ChannelPreferenceOptions ->
                logInfo("Updated channelPreferenceOptions channel:$channel channelPreferenceOptions: $channelPreferenceOptions ")
                coroutineScope.launch {
                    val response = SSApi.getInstance().getUser().getPreferences().updateOverallChannelPreference(
                        channel = channel,
                        channelPreferenceOptions = channelPreferenceOptions
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
            val data = preferences.fetchUserPreference(
                tenantId = BuildConfig.SS_TENANT_ID
            ).getData() ?: return@launch
            showData(data)
        }
    }

    override fun onResume() {
        super.onResume()
        preferences.registerCallback(object : PreferenceCallback {
            override fun onUpdate() {
                coroutineScope.launch {
                    val data = preferences.fetchUserPreference(
                        fetchRemote = false,
                        tenantId = BuildConfig.SS_TENANT_ID
                    ).getData() ?: return@launch
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
                itemsList.add(RecyclerViewItem.SectionVo(idd = section.name, title = section.name, description = section.description))
            section.subCategories.forEachIndexed { scIndex, subCategory ->
                itemsList.add(RecyclerViewItem.SubCategoryVo(subCategory, section.subCategories.isLast(scIndex)))
            }
        }
        itemsList.add(
            RecyclerViewItem.SectionVo(
                "Over all section divider",
                "What notifications to allow for channel?"
            )
        )
        channelPreferences.forEach { channelPreference ->
            val isExpanded = expandedIds[channelPreference.channel] ?: false
            itemsList.add(RecyclerViewItem.ChannelPreferenceVo(channelPreference, isExpanded))
        }
        return itemsList
    }
}
