package app.suprsend.user.preference

import app.suprsend.base.Logger
import app.suprsend.base.Response
import app.suprsend.base.SSConstants
import app.suprsend.base.SdkAndroidCreator
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

    override fun fetchUserPreference(tenantId: String?, fetchRemote: Boolean): Response<PreferenceData> {
        val response = SSInternalUserPreference.fetchAndSavePreferenceData(tenantId, fetchRemote)
        if (fetchRemote)
            preferenceCallback?.onUpdate()
        return response
    }

    override fun fetchCategories(tenantId: String?, limit: Int?, offset: Int?): Response<JSONObject> {
        val response = SSInternalUserPreference.fetchCategories(tenantId, limit, offset)
        return response
    }

    override fun fetchCategory(category: String, tenantId: String?): Response<JSONObject> {
        val response = SSInternalUserPreference.fetchCategory(category, tenantId)
        preferenceCallback?.onUpdate()
        return response
    }

    override fun fetchOverallChannelPreferences(): Response<JSONObject> {
        val response = SSInternalUserPreference.fetchOverallChannelPreferences()
        preferenceCallback?.onUpdate()
        return response
    }

    override fun updateCategoryPreference(category: String, preference: PreferenceOptions, tenantId: String?): Response<JSONObject> {
        if (!SdkAndroidCreator.networkInfo.isConnected()) {
            preferenceCallback?.onUpdate()
            return Response.Error(NoInternetException())
        }
        val response = SSInternalUserPreference.updateCategoryPreference(category, tenantId, preference)
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
        tenantId: String?
    ): Response<JSONObject> {
        if (!SdkAndroidCreator.networkInfo.isConnected()) {
            preferenceCallback?.onUpdate()
            return Response.Error(NoInternetException())
        }
        val response = SSInternalUserPreference.updateChannelPreferenceInCategory(category, channel, preference, tenantId)
        if (!response.isSuccess()) {
            Logger.e(SSConstants.TAG_SUPRSEND, response.getException()?.message ?: "Something went wrong")
        }
        return response
    }

    override fun updateOverallChannelPreference(
        channel: String,
        channelPreferenceOptions: ChannelPreferenceOptions
    ): Response<JSONObject> {
        if (!SdkAndroidCreator.networkInfo.isConnected()) {
            preferenceCallback?.onUpdate()
            return Response.Error(NoInternetException())
        }
        val response = SSInternalUserPreference.updateOverallChannelPreference(channel, channelPreferenceOptions)
        if (!response.isSuccess()) {
            Logger.e(SSConstants.TAG_SUPRSEND, response.getException()?.message ?: "Something went wrong")
        }
        preferenceCallback?.onUpdate()
        return response
    }

}