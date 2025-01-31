package app.suprsend.android.preference

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import app.suprsend.SuprSend
import app.suprsend.android.BuildConfig
import app.suprsend.android.databinding.UserPreferenceActivityBinding
import app.suprsend.android.isLast
import app.suprsend.android.logInfo
import app.suprsend.android.myToast
import app.suprsend.base.Response
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
import org.json.JSONObject

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
        SuprSend.getInstance().user.getPreferences().setPreferenceConfig(
            tenantId = intent.extras?.get("tenantId").toString() ?: "",
            showOptOutChannels = intent.extras?.get("showOptOutChannels").toString().toBoolean()
        )
        binding.categoriesRV.layoutManager = LinearLayoutManager(this@UserPreferenceActivity)
        adapter = UserPreferenceRecyclerViewAdapter(
            //Category Toggle from right side
            categoryItemClick = { category, checked ->
                coroutineScope.launch(Dispatchers.IO) {
                    val response = SuprSend.getInstance().user.getPreferences().updateCategoryPreference(
                        category = category,
                        preference = PreferenceOptions.from(checked)
                    )
                    if (response.getException() is NoInternetException) {
                        withContext(Dispatchers.Main) {
                            myToast("Please check internet connection")
                        }
                    }
                }
            },
            //Category Channel Item
            channelItemClick = { category, channel, checked ->
                coroutineScope.launch {
                    val response = SuprSend.getInstance().user.getPreferences().updateChannelPreferenceInCategory(
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
            },
            channelPreferenceArrowClick = { category, expanded ->
                expandedIds[category] = expanded
            },
//           //Overall
            channelPreferenceChangeClick = { channel: String, channelPreferenceOptions: ChannelPreferenceOptions ->
                logInfo("Updated channelPreferenceOptions channel:$channel channelPreferenceOptions: $channelPreferenceOptions ")
                coroutineScope.launch {
                    val response = SuprSend.getInstance().user.getPreferences().updateOverallChannelPreference(
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

        preferences = SuprSend.getInstance().user.getPreferences()

        coroutineScope.launch {
            val data = preferences.fetchUserPreference(fetchRemote = true).getData() ?: return@launch
            showData(data)
        }
    }

    override fun onResume() {
        super.onResume()
        preferences.registerCallback(object : PreferenceCallback {
            override fun onUpdate(preferenceData: PreferenceData) {
                coroutineScope.launch {
                    showData(preferenceData)
                }
            }

            override fun onError(response: Response<JSONObject>) {
                Log.e("preference", "Response Json : ${response.getData()}")
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
