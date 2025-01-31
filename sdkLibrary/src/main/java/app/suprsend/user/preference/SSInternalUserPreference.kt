package app.suprsend.user.preference

import android.annotation.SuppressLint
import android.content.Context
import app.suprsend.SuprSendInternal
import app.suprsend.base.Response
import app.suprsend.base.SSConstants
import app.suprsend.model.toResponse
import app.suprsend.utils.forEach
import app.suprsend.utils.forEachIndexed
import app.suprsend.utils.mapToEnum
import app.suprsend.utils.safeBoolean
import app.suprsend.utils.safeJsonArray
import app.suprsend.utils.safeString
import app.suprsend.utils.safeStringDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

internal object SSInternalUserPreference {

    var tenantId: String? = null
    var showOptOutChannels: Boolean = true
    var preferenceCallback: PreferenceCallback? = null

    fun fetchAndSavePreferenceData(fetchRemote: Boolean): Response<PreferenceData> {
        return try {
            if (fetchRemote) {
                val apiResponse = UserPreferenceRemote.preference(tenantId, showOptOutChannels)
                if (apiResponse.statusCode == 200) {
                    val response = apiResponse.body
                    if (!response.isNullOrBlank()) {
                        savePreferenceData(response)
                    }
                }
            }
            val response = getPreferenceDataJO()
            return if (response == null) {
                Response.Error(IllegalStateException("Something went wrong"))
            } else {
                val preferenceData = UserPreferenceParser.parse(response)
                Response.Success(preferenceData)
            }
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    fun fetchCategories(
        limit: Int? = null,
        offset: Int? = null
    ): Response<JSONObject> {
        return UserPreferenceRemote
            .fetchCategories(tenantId = tenantId, limit = limit, offset = offset, showOptOutChannels = showOptOutChannels)
            .toResponse()
    }

    fun fetchCategory(
        category: String
    ): Response<JSONObject> {
        return UserPreferenceRemote
            .fetchCategory(category = category, tenantId = tenantId, showOptOutChannels = showOptOutChannels)
            .toResponse()
    }

    fun fetchOverallChannelPreferences(): Response<JSONObject> {
        return UserPreferenceRemote
            .fetchOverallChannelPreferences()
            .toResponse()
    }


    fun updateCategoryPreference(
        category: String,
        preference: PreferenceOptions
    ): Response<JSONObject> {

        val preferenceDataJO = getPreferenceDataJO() ?: return Response.Error(
            IllegalStateException("Preference data is not available. Please fetch preference data with fetchPreferenceData() call")
        )

        val updateCategory = category
        val updatePreference = preference

        var updated = false
        var subCategoryIndexInSection: Int? = null
        var sectionJoFound: JSONObject? = null
        var subCategoryJoFound: JSONObject? = null
        var validationResponse: Response<JSONObject>? = null

        if (preferenceDataJO.has("sections")) {
            preferenceDataJO
                .safeJsonArray("sections")
                ?.forEach SectionForEach@{ sectionJo ->
                    sectionJo.safeJsonArray("subcategories")?.forEachIndexed SubCategoryForEach@{ subCategoryIndex, subCategoryJo ->
                        val categoryStr = subCategoryJo.safeString("category")
                        if (categoryStr == updateCategory) {
                            subCategoryIndexInSection = subCategoryIndex
                            sectionJoFound = sectionJo
                            subCategoryJoFound = subCategoryJo
                            if (subCategoryJo.safeBoolean("is_editable") == true) {
                                val preferenceStr = getPreference(subCategoryJo.safeString("preference") ?: "")
                                if (preferenceStr != updatePreference) {
                                    subCategoryJo.put("preference", preference.getNetworkName())
                                    updated = true
                                }
                            } else {
                                validationResponse = Response.Error(IllegalStateException("Category-$updateCategory is not editable"))
                            }
                            return@SubCategoryForEach true
                        }
                        false
                    }
                    updated
                }
        }
        return if (subCategoryJoFound == null ||
            sectionJoFound == null ||
            subCategoryIndexInSection == null
        ) {
            Response.Error(IllegalStateException("Category-$updateCategory is not found"))
        } else {
            if (!updated) {
                return validationResponse ?: Response.Success(preferenceDataJO)
            }

            val nwResponse = updateCategoryPreferenceRemote(subCategoryJoFound, updatePreference, updateCategory, tenantId, showOptOutChannels)
            if (nwResponse.isSuccess()) {
                val responseJo = nwResponse.getData()
                if (responseJo != null) {
                    if (responseJo.has("channels")) {
                        sectionJoFound?.safeJsonArray("subcategories")?.put(
                            subCategoryIndexInSection!!,
                            responseJo
                        )
                    }
                    savePreferenceData(preferenceDataJO.toString())
                }
            } else {
                preferenceCallback?.onError(nwResponse)
            }
            return nwResponse
        }
    }

    fun updateChannelPreferenceInCategory(
        category: String,
        channel: String,
        channelPreference: PreferenceOptions
    ): Response<JSONObject> {

        val preferenceDataJO = getPreferenceDataJO() ?: return Response.Error(
            IllegalStateException("Preference data is not available. Please fetch preference data with fetchPreferenceData() call")
        )

        val updateCategory = category
        val updateChannel = channel
        val updateChannelPreference = channelPreference

        var updated = false
        var subCategoryJoFound: JSONObject? = null
        var chanelJoFound: JSONObject? = null
        var response: Response<JSONObject>? = null

        if (preferenceDataJO.has("sections")) {
            preferenceDataJO
                .safeJsonArray("sections")
                ?.forEach SectionForEach@{ sectionJo ->
                    sectionJo.safeJsonArray("subcategories")?.forEach SubCategoryForEach@{ subCategoryJo ->
                        val jsonCategory = subCategoryJo.safeString("category")
                        if (jsonCategory == updateCategory) {
                            subCategoryJoFound = subCategoryJo
                            //Do not check while updating channel preference
//                            if (subCategoryJo.safeBoolean("is_editable") == true) {
                            subCategoryJo.safeJsonArray("channels")?.forEach ChannelForEach@{ channelJo ->
                                val channelStr = channelJo.safeString("channel") ?: ""
                                if (channelStr == updateChannel) {
                                    chanelJoFound = channelJo
                                    if (channelJo.safeBoolean("is_editable") == true) {
                                        val currentPreference = getPreference(channelJo.safeString("preference") ?: "")
                                        if (currentPreference != updateChannelPreference) {
                                            channelJo.put("preference", updateChannelPreference.getNetworkName())
                                            if (updateChannelPreference == PreferenceOptions.OPT_IN) {
                                                subCategoryJo.put("preference", updateChannelPreference.getNetworkName())
                                            }
                                            updated = true
                                        }
                                    } else {
                                        response = Response.Error(IllegalStateException("Channel-$updateChannel is not editable in category-$updateCategory"))
                                    }
                                    return@ChannelForEach true
                                }
                                false
                            }
//                            } else {
//                                response = Response.Error(IllegalStateException("Category-$updateCategory is not editable"))
//                            }
                            return@SubCategoryForEach true
                        }
                        false
                    }
                    updated
                }
        }

        return if (subCategoryJoFound == null) {
            response ?: Response.Error(IllegalStateException("Category-$updateCategory is not found"))
        } else if (chanelJoFound == null) {
            response ?: Response.Error(IllegalStateException("Channel-$updateChannel is not found in Category-$updateCategory"))
        } else {
            if (!updated) {
                return response ?: Response.Success(preferenceDataJO)
            }
            savePreferenceData(preferenceDataJO.toString())
            val remoteResponse = updateChannelPreferenceInCategoryRemote(
                subCategoryJoFound = subCategoryJoFound,
                updateCategory = updateCategory,
                updateChannelPreference = updateChannelPreference,
                tenantId = tenantId,
                showOptOutChannels = showOptOutChannels
            )
            if (!remoteResponse.isSuccess()) {
                preferenceCallback?.onError(remoteResponse)
            }
            remoteResponse
        }
    }

    fun updateOverallChannelPreference(
        channel: String,
        channelPreferenceOptions: ChannelPreferenceOptions
    ): Response<JSONObject> {

        val preferenceDataJO = getPreferenceDataJO() ?: return Response.Error(
            IllegalStateException("Preference data is not available. Please fetch preference data with fetchPreferenceData() call")
        )
        var channelJoFound: JSONObject? = null
        var updated = false
        val isRestricted = channelPreferenceOptions == ChannelPreferenceOptions.REQUIRED
        preferenceDataJO.safeJsonArray("channel_preferences")?.forEach ChannelForEach@{ channelPrefJo ->
            if (channelPrefJo.safeStringDefault("channel", "") == channel) {
                channelJoFound = channelPrefJo
                if (channelPrefJo.safeBoolean("is_restricted") != isRestricted) {
                    channelPrefJo.put("is_restricted", isRestricted)
                    updated = true
                    return@ChannelForEach true
                }
            }
            false
        }

        return if (channelJoFound == null) {
            Response.Error(IllegalStateException("Channel-$channel is not found"))
        } else {

            if (!updated) {
                return Response.Success(preferenceDataJO)
            }

            savePreferenceData(preferenceDataJO.toString())

            val updateOverallChannelPrefResponse = UserPreferenceRemote
                .updateOverallChannelPreference(channel, isRestricted)
                .toResponse()

            fetchAndSavePreferenceData(fetchRemote = true)

            if (!updateOverallChannelPrefResponse.isSuccess()) {
                preferenceCallback?.onError(updateOverallChannelPrefResponse)
            }

            return updateOverallChannelPrefResponse
        }

    }

    fun getPreference(preferenceString: String): PreferenceOptions {
        return preferenceString.mapToEnum<PreferenceOptions>(ignoreCase = true) ?: PreferenceOptions.OPT_OUT
    }

    @SuppressLint("ApplySharedPref")
    internal fun clearUserPreference() {
        SuprSendInternal.context.getSharedPreferences(SSConstants.SP_USER_PREFERENCES, Context.MODE_PRIVATE).edit().apply {
            putString(SSConstants.SP_USER_PREFERENCES, "")
            commit()
        }
    }

    private fun updateCategoryPreferenceRemote(
        subCategoryJoFound: JSONObject?,
        preference: PreferenceOptions,
        updateCategory: String,
        tenantId: String?,
        showOptOutChannels: Boolean
    ): Response<JSONObject> {
        val optOutChannelsJA = JSONArray()
        subCategoryJoFound?.safeJsonArray("channels")?.forEach { channelJo ->
            val channelPreference = getPreference(channelJo.safeString("preference") ?: "")
            if (channelPreference == PreferenceOptions.OPT_OUT) {
                optOutChannelsJA.put(channelJo.safeString("channel") ?: "")
            }
            false
        }
        val requestBody = JSONObject()
        requestBody.put("preference", preference.getNetworkName())
        val modifiedOptOutChannelsJA = if (showOptOutChannels && preference == PreferenceOptions.OPT_IN) {
            //Backend handles in this case so we have to send null
            null
        } else
            optOutChannelsJA
        requestBody.put("opt_out_channels", modifiedOptOutChannelsJA)
        return UserPreferenceRemote
            .updateCategoryPreferences(category = updateCategory, tenantId = tenantId, body = requestBody.toString(), showOptOutChannels = showOptOutChannels)
            .toResponse()
    }


    private fun updateChannelPreferenceInCategoryRemote(
        subCategoryJoFound: JSONObject?,
        updateCategory: String,
        updateChannelPreference: PreferenceOptions,
        tenantId: String?,
        showOptOutChannels: Boolean
    ): Response<JSONObject> {
        val subCategoryPreference = getPreference(subCategoryJoFound?.safeString("preference") ?: "")
        val optOutChannelsJA = JSONArray()
        subCategoryJoFound?.safeJsonArray("channels")?.forEach { channelJo ->
            val channelPreference = getPreference(channelJo.safeString("preference") ?: "")
            if (channelPreference == PreferenceOptions.OPT_OUT) {
                optOutChannelsJA.put(channelJo.safeString("channel") ?: "")
            }
            false
        }
        val modifiedCategoryPreference = if (showOptOutChannels && subCategoryPreference == PreferenceOptions.OPT_OUT
            && updateChannelPreference == PreferenceOptions.OPT_IN
        )
            PreferenceOptions.OPT_IN
        else
            subCategoryPreference
        val requestBody = JSONObject()
        requestBody.put("preference", modifiedCategoryPreference.getNetworkName())
        requestBody.put("opt_out_channels", optOutChannelsJA)
        return UserPreferenceRemote
            .updateCategoryPreferences(category = updateCategory, tenantId = tenantId, body = requestBody.toString(), showOptOutChannels = showOptOutChannels)
            .toResponse()
    }

    @SuppressLint("ApplySharedPref")
    private fun savePreferenceData(preferenceResponse: String) {
        SuprSendInternal.context.getSharedPreferences(SSConstants.SP_USER_PREFERENCES, Context.MODE_PRIVATE).edit().apply {
            putString(SSConstants.SP_USER_PREFERENCES, preferenceResponse)
            commit()
        }
    }

    private fun getPreferenceDataJO(): JSONObject? {
        val response = SuprSendInternal.context.getSharedPreferences(SSConstants.SP_USER_PREFERENCES, Context.MODE_PRIVATE).getString(SSConstants.SP_USER_PREFERENCES, "")
        return if (response.isNullOrBlank()) {
            null
        } else {
            JSONObject(response)
        }
    }


}

fun CoroutineScope.executeWithThrottleLast(intervalMillis: Long, action: () -> Unit): (Any) -> Unit {
    var job: Job? = null
    var lastTime = 0L

    return { event ->
        val currentTime = System.currentTimeMillis()
        job?.cancel() // Cancel any previously scheduled job

        job = this.launch {
            val timeSinceLast = currentTime - lastTime
            if (timeSinceLast >= intervalMillis) {
                lastTime = currentTime
                action()
            } else {
                delay(intervalMillis - timeSinceLast)
                lastTime = System.currentTimeMillis()
                action()
            }
        }
    }
}