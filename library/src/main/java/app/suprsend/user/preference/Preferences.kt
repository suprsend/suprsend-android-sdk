package app.suprsend.user.preference

import androidx.annotation.WorkerThread
import app.suprsend.base.Response
import org.json.JSONObject

interface Preferences {

    fun registerCallback(preferenceCallback:PreferenceCallback)

    fun unRegisterCallback()

    @WorkerThread
    fun fetchUserPreference(tenantId: String? = null, fetchRemote: Boolean = true): Response<PreferenceData>

    @WorkerThread
    fun fetchCategories(
        tenantId: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Response<JSONObject>

    @WorkerThread
    fun fetchCategory(
        category: String,
        tenantId: String? = null
    ): Response<JSONObject>

    @WorkerThread
    fun fetchOverallChannelPreferences(): Response<JSONObject>

    @WorkerThread
    fun updateCategoryPreference(
        category: String,
        preference: PreferenceOptions,
        tenantId: String? = null
    ): Response<JSONObject>

    @WorkerThread
    fun updateChannelPreferenceInCategory(
        category: String,
        channel: String,
        preference: PreferenceOptions,
        tenantId: String? = null
    ): Response<JSONObject>

    @WorkerThread
    fun updateOverallChannelPreference(
        channel: String,
        channelPreferenceOptions: ChannelPreferenceOptions
    ): Response<JSONObject>

}