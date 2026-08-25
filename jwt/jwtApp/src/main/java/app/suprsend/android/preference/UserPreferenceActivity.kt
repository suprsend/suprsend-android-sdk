package app.suprsend.android.preference

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import app.suprsend.Emitter
import app.suprsend.SuprSend
import app.suprsend.android.AppConstants
import app.suprsend.android.AppCreator
import app.suprsend.android.databinding.UserPreferenceActivityBinding
import app.suprsend.android.isLast
import app.suprsend.android.logInfo
import app.suprsend.android.myToast
import app.suprsend.user.preference.ChannelLevelPreferenceOptions
import app.suprsend.user.preference.PreferenceAPIResponse
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

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val expandedIds = hashMapOf<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Preferences"
        binding = UserPreferenceActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val preferenceArgs = Preferences.Args(
            tenantId = AppCreator.getTenantId(),
            showOptOutChannels = intent.extras?.get("showOptOutChannels").toString().toBoolean()
        )
        binding.categoriesRV.layoutManager = LinearLayoutManager(this@UserPreferenceActivity)
        adapter = UserPreferenceRecyclerViewAdapter(
            categoryItemClick = { category, checked ->
                applyPreferenceUpdate("Failed to update category preference") {
                    SuprSend.getInstance().preferences.updateCategoryPreference(
                        category = category,
                        preference = if (checked) PreferenceOptions.optIn else PreferenceOptions.optOut,
                        args = preferenceArgs
                    )
                }
            },
            channelItemClick = { category, channel, checked ->
                applyPreferenceUpdate("Failed to update channel preference") {
                    SuprSend.getInstance().preferences.updateChannelPreferenceInCategory(
                        channel = channel,
                        preference = if (checked) PreferenceOptions.optIn else PreferenceOptions.optOut,
                        category = category,
                        args = preferenceArgs
                    )
                }
            },
            channelPreferenceArrowClick = { category, expanded ->
                expandedIds[category] = expanded
            },
            channelPreferenceChangeClick = { channel: String, preference: ChannelLevelPreferenceOptions ->
                logInfo("Updated channelPreferenceOptions channel:$channel channelPreferenceOptions: $preference ")
                applyPreferenceUpdate("Failed to update overall channel preference") {
                    SuprSend.getInstance().preferences.updateOverallChannelPreference(
                        channel = channel,
                        preference = preference,
                        args = preferenceArgs
                    )
                }
            }
        )
        binding.categoriesRV.adapter = adapter

        coroutineScope.launch {
            val data = SuprSend.getInstance().preferences.getPreferences(args = preferenceArgs).body ?: return@launch
            showData(data)
        }

        SuprSend.getInstance().emitter.on(Emitter.Event.preferencesUpdated) { response ->
            val data = response?.body ?: return@on
            coroutineScope.launch { showData(data) }
        }
        SuprSend.getInstance().emitter.on(Emitter.Event.preferencesError) { response ->
            runOnUiThread {
                myToast(response?.error?.message ?: "Preference update failed")
            }
        }

        binding.testButton.setOnClickListener {
            coroutineScope.launch {
                var data = SuprSend.getInstance().preferences.getCategories(args = Preferences.CategoryArgs(
                    tenantId = preferenceArgs.tenantId,
                    showOptOutChannels = preferenceArgs.showOptOutChannels
                )).body ?: return@launch
                Log.i(AppConstants.TAG, data)
                val category = org.json.JSONObject(data).optJSONArray("results")?.optJSONObject(0)?.optString("category") ?: ""
                data = SuprSend.getInstance().preferences.getCategory(category, args = preferenceArgs).body ?: return@launch
                Log.i(AppConstants.TAG, data)
                data = SuprSend.getInstance().preferences.getOverallChannelPreferences(args = preferenceArgs).body ?: return@launch
                Log.i(AppConstants.TAG, data)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    private fun applyPreferenceUpdate(
        fallbackError: String,
        update: () -> PreferenceAPIResponse
    ) {
        coroutineScope.launch {
            val response = update()
            val body = response.body
            if (body != null) {
                showData(body)
            } else if (response.error != null) {
                withContext(Dispatchers.Main) {
                    myToast(response.error?.message ?: fallbackError)
                }
            }
        }
    }

    private suspend fun showData(preferenceData: PreferenceData) {
        withContext(Dispatchers.Main) {
            adapter.setItems(preferenceData.toUIItems())
        }
    }

    private fun PreferenceData?.toUIItems(): List<RecyclerViewItem> {
        if (this == null) return listOf()
        val itemsList = arrayListOf<RecyclerViewItem>()
        sections?.forEachIndexed { _, section ->
            val sectionName = section.name
            if (!sectionName.isNullOrBlank()) {
                itemsList.add(RecyclerViewItem.SectionVo(title = sectionName, description = section.description ?: ""))
            }
            val subcategories = section.subcategories ?: return@forEachIndexed
            subcategories.forEachIndexed { scIndex, subcategory ->
                itemsList.add(RecyclerViewItem.CategoryVo(subcategory, subcategories.isLast(scIndex)))
            }
        }
        itemsList.add(
            RecyclerViewItem.SectionVo(
                title = "What notifications to allow for channel?"
            )
        )
        channelPreferences?.forEach { channelPreference ->
            val isExpanded = expandedIds[channelPreference.channel] ?: false
            itemsList.add(RecyclerViewItem.ChannelPreferenceVo(channelPreference, isExpanded))
        }
        return itemsList
    }
}
