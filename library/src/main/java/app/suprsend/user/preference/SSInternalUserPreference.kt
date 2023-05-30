package app.suprsend.user.preference

import android.annotation.SuppressLint
import app.suprsend.base.Response
import app.suprsend.base.SSConstants
import app.suprsend.base.SdkAndroidCreator
import app.suprsend.base.forEach
import app.suprsend.base.mapToEnum
import app.suprsend.base.safeBoolean
import app.suprsend.base.safeJsonArray
import app.suprsend.base.safeString
import app.suprsend.base.safeStringDefault
import app.suprsend.event.HttPResponse
import org.json.JSONArray
import org.json.JSONObject

internal object SSInternalUserPreference {

    fun fetchAndSavePreferenceData(brandId: String? = null): Response<PreferenceData> {
        return try {
            val httpResponse = UserPreferenceRemote.preference(brandId)
            if (httpResponse.statusCode == 200) {
                val response = httpResponse.response
                if (!response.isNullOrBlank()) {
                    savePreferenceData(response)
                    val preferenceData = UserPreferenceParser.parse(JSONObject(response))
                    return Response.Success(preferenceData)
                }
            }
            Response.Error(IllegalStateException("Something went wrong status code : ${httpResponse.statusCode}"))
        } catch (e: Exception) {
            Response.Error(e)
        }
    }

    fun fetchCategories(
        brandId: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Response<JSONObject> {
        return UserPreferenceRemote
            .fetchCategories(brandId = brandId, limit = limit, offset = offset)
            .toResponse()
    }

    fun fetchCategory(
        category: String,
        brandId: String? = null
    ): Response<JSONObject> {
        return UserPreferenceRemote
            .fetchCategory(category = category, brandId = brandId)
            .toResponse()
    }

    fun fetchOverallChannelPreferences(): Response<JSONObject> {
        return UserPreferenceRemote
            .fetchOverallChannelPreferences()
            .toResponse()
    }

    fun updateCategoryPreference(
        category: String,
        brandId: String?,
        preference: PreferenceOptions
    ): Response<JSONObject> {

        val preferenceDataJO = getPreferenceDataJO() ?: return Response.Error(
            IllegalStateException("Preference data is not available. Please fetch preference data with fetchPreferenceData() call")
        )

        val updateCategory = category
        val updatePreference = preference

        var updated = false
        var subCategoryJoFound: JSONObject? = null
        var response: Response<JSONObject>? = null

        if (preferenceDataJO.has("sections")) {
            preferenceDataJO
                .safeJsonArray("sections")
                ?.forEach SectionForEach@{ sectionJo ->
                    sectionJo.safeJsonArray("subcategories")?.forEach SubCategoryForEach@{ subCategoryJo ->
                        val categoryStr = subCategoryJo.safeString("category")
                        if (categoryStr == updateCategory) {
                            subCategoryJoFound = subCategoryJo
                            if (subCategoryJo.safeBoolean("is_editable") == true) {
                                val preferenceStr = getPreference(subCategoryJo.safeString("preference") ?: "")
                                if (preferenceStr != updatePreference) {
                                    subCategoryJo.put("preference", preference.getNetworkName())
                                    updated = true
                                }
                            } else {
                                response = Response.Error(IllegalStateException("Category-$updateCategory is not editable"))
                            }
                            return@SubCategoryForEach true
                        }
                        false
                    }
                    updated
                }
        }
        return if (subCategoryJoFound == null) {
            Response.Error(IllegalStateException("Category-$updateCategory is not found"))
        } else {
            if (!updated) {
                return response ?: Response.Success(preferenceDataJO)
            }
            updateCategoryPreferenceRemote(subCategoryJoFound, updatePreference, updateCategory, brandId)
        }
    }

    fun updateChannelPreferenceInCategory(
        category: String,
        channel: String,
        preference: PreferenceOptions,
        brandId: String?
    ): Response<JSONObject> {

        val preferenceDataJO = getPreferenceDataJO() ?: return Response.Error(
            IllegalStateException("Preference data is not available. Please fetch preference data with fetchPreferenceData() call")
        )

        val updateCategory = category
        val updateChannel = channel
        val updatePreference = preference

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
                            if (subCategoryJo.safeBoolean("is_editable") == true) {
                                subCategoryJo.safeJsonArray("channels")?.forEach ChannelForEach@{ channelJo ->
                                    val channelStr = channelJo.safeString("channel") ?: ""
                                    if (channelStr == updateChannel) {
                                        chanelJoFound = channelJo
                                        if (channelJo.safeBoolean("is_editable") == true) {
                                            val currentPreference = getPreference(channelJo.safeString("preference") ?: "")
                                            if (currentPreference != updatePreference) {
                                                channelJo.put("preference", updatePreference.getNetworkName())
                                                if (updatePreference == PreferenceOptions.OPT_IN) {
                                                    subCategoryJo.put("preference", updatePreference.getNetworkName())
                                                }
                                                updated = true
                                            }
                                        } else {
                                            response = Response.Error(IllegalStateException("Channel-$updateChannel is not editable"))
                                        }
                                        return@ChannelForEach true
                                    }
                                    false
                                }
                            } else {
                                response = Response.Error(IllegalStateException("Category-$updateCategory is not editable"))
                            }
                            return@SubCategoryForEach true
                        }
                        false
                    }
                    updated
                }
        }

        return if (subCategoryJoFound == null) {
            Response.Error(IllegalStateException("Category-$updateCategory is not found"))
        } else if (chanelJoFound == null) {
            Response.Error(IllegalStateException("Channel-$updateChannel is not found in Category-$updateCategory"))
        } else {
            if (!updated) {
                return response ?: Response.Success(preferenceDataJO)
            }
            updateCategoryPreferenceRemote(subCategoryJoFound, getPreference(subCategoryJoFound?.safeString("preference") ?: ""), updateCategory, brandId)
        }
    }

    fun updateOverallChannelPreference(
        channel: String,
        channelPreference: ChannelPreferenceOptions
    ): Response<JSONObject> {
        val preferenceDataJO = getPreferenceDataJO() ?: return Response.Error(
            IllegalStateException("Preference data is not available. Please fetch preference data with fetchPreferenceData() call")
        )
        var channelJoFound: JSONObject? = null
        var updated = false
        val isRestricted = channelPreference == ChannelPreferenceOptions.REQUIRED
        preferenceDataJO.safeJsonArray("channel_preferences")?.forEach ChannelForEach@{ channelPrefJo ->
            if (channelPrefJo.safeStringDefault("channel", "") == channel) {
                channelJoFound= channelPrefJo
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
        }  else {
            if (!updated) {
                return  Response.Success(preferenceDataJO)
            }
            UserPreferenceRemote
                .updateChannelPreference(channel,isRestricted)
                .toResponse()
        }

    }

    fun getPreference(preferenceString: String): PreferenceOptions {
        return preferenceString.mapToEnum<PreferenceOptions>(ignoreCase = true) ?: PreferenceOptions.OPT_OUT
    }

    @SuppressLint("ApplySharedPref")
    internal fun clearUserPreference() {
        SdkAndroidCreator.getSharedPreference().edit().apply {
            putString(SSConstants.SP_USER_PREFERENCES, "")
            commit()
        }
    }

    private fun updateCategoryPreferenceRemote(subCategoryJoFound: JSONObject?, preference: PreferenceOptions, updateCategory: String, brandId: String?): Response<JSONObject> {
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
        requestBody.put("opt_out_channels", optOutChannelsJA)
        return UserPreferenceRemote
            .updateCategoryPreferences(category = updateCategory, brandId = brandId, body = requestBody.toString())
            .toResponse()
    }

    @SuppressLint("ApplySharedPref")
    private fun savePreferenceData(preferenceResponse: String) {
        SdkAndroidCreator.getSharedPreference().edit().apply {
            putString(SSConstants.SP_USER_PREFERENCES, preferenceResponse)
            commit()
        }
    }

    private fun getPreferenceDataJO(): JSONObject? {
        val response = SdkAndroidCreator.getSharedPreference().getString(SSConstants.SP_USER_PREFERENCES, "")
        return if (response.isNullOrBlank()) {
            null
        } else {
            JSONObject(response)
        }
    }

    private fun HttPResponse.toResponse(): Response<JSONObject> {
        return if (ok()) {
            Response.Success(JSONObject(response ?: ""))
        } else {
            Response.Error(IllegalStateException("Something went wrong $statusCode"))
        }
    }

}