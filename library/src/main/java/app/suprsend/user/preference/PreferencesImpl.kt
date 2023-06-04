package app.suprsend.user.preference

import app.suprsend.base.Logger
import app.suprsend.base.Response
import app.suprsend.base.SSConstants
import app.suprsend.base.SdkAndroidCreator
import app.suprsend.base.executorService
import app.suprsend.exception.NoInternetException
import org.json.JSONObject

class PreferencesImpl : Preferences {
    var preferenceCallback: PreferenceCallback? = null
    override fun registerCallback(preferenceCallback: PreferenceCallback) {
        this.preferenceCallback = preferenceCallback
    }

    override fun unRegisterCallback() {
        preferenceCallback = null
    }

    override fun fetchUserPreference(brandId: String?, fetchRemote: Boolean): Response<PreferenceData> {
        val response = SSInternalUserPreference.fetchAndSavePreferenceData(brandId, fetchRemote)
        if (fetchRemote)
            preferenceCallback?.onUpdate()
        return response
    }

    override fun fetchCategories(brandId: String?, limit: Int?, offset: Int?): Response<JSONObject> {
        val response = SSInternalUserPreference.fetchCategories(brandId, limit, offset)
        return response
    }

    override fun fetchCategory(category: String, brandId: String?): Response<JSONObject> {
        val response = SSInternalUserPreference.fetchCategory(category, brandId)
        preferenceCallback?.onUpdate()
        return response
    }

    override fun fetchOverallChannelPreferences(): Response<JSONObject> {
        val response = SSInternalUserPreference.fetchOverallChannelPreferences()
        preferenceCallback?.onUpdate()
        return response
    }

    override fun updateCategoryPreference(category: String, preference: PreferenceOptions, brandId: String?): Response<JSONObject> {
        if (!SdkAndroidCreator.networkInfo.isConnected())
            return Response.Error(NoInternetException())
        val response = SSInternalUserPreference.updateCategoryPreference(category, brandId, preference)
        if (!response.isSuccess()) {
            Logger.e(SSConstants.TAG_SUPRSEND, response.getException()?.message ?: "Something went wrong")
        }
        preferenceCallback?.onUpdate()
        return response
    }

    override fun updateChannelPreferenceInCategory(
        category: String,
        channel: String,
        preference: PreferenceOptions,
        brandId: String?
    ): Response<JSONObject> {
        if (!SdkAndroidCreator.networkInfo.isConnected())
            return Response.Error(NoInternetException())
        val response = SSInternalUserPreference.updateChannelPreferenceInCategory(category, channel, preference, brandId)
        if (!response.isSuccess()) {
            Logger.e(SSConstants.TAG_SUPRSEND, response.getException()?.message ?: "Something went wrong")
        }
        preferenceCallback?.onUpdate()
        return response
    }

    override fun updateOverallChannelPreference(
        channel: String,
        channelPreference: ChannelPreferenceOptions
    ): Response<JSONObject> {
        if (!SdkAndroidCreator.networkInfo.isConnected())
            return Response.Error(NoInternetException())
        val response = SSInternalUserPreference.updateOverallChannelPreference(channel, channelPreference)
        if (!response.isSuccess()) {
            Logger.e(SSConstants.TAG_SUPRSEND, response.getException()?.message ?: "Something went wrong")
        }
        preferenceCallback?.onUpdate()
        return response
    }

}