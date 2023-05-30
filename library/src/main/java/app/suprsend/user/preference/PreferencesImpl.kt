package app.suprsend.user.preference

import app.suprsend.base.Response
import org.json.JSONObject

class PreferencesImpl : Preferences {

    override fun fetchUserPreference(brandId: String?): Response<PreferenceData> {
        return SSInternalUserPreference.fetchAndSavePreferenceData(brandId)
    }

    override fun fetchCategories(brandId: String?, limit: Int?, offset: Int?): Response<JSONObject> {
        return SSInternalUserPreference.fetchCategories(brandId, limit, offset)
    }

    override fun fetchCategory(category: String, brandId: String?): Response<JSONObject> {
        return SSInternalUserPreference.fetchCategory(category, brandId)
    }

    override fun fetchOverallChannelPreferences(): Response<JSONObject> {
        return SSInternalUserPreference.fetchOverallChannelPreferences()
    }

    override fun updateCategoryPreference(category: String, brandId: String?, preference: PreferenceOptions): Response<JSONObject> {
        return SSInternalUserPreference.updateCategoryPreference(category, brandId, preference)
    }

    override fun updateChannelPreferenceInCategory(category: String, channel: String, preference: PreferenceOptions, brandId: String?): Response<JSONObject> {
        return SSInternalUserPreference.updateChannelPreferenceInCategory(category, channel, preference, brandId)
    }

    override fun updateOverallChannelPreference(channel: String, channelPreference: ChannelPreferenceOptions): Response<JSONObject> {
        return SSInternalUserPreference.updateOverallChannelPreference(channel, channelPreference)
    }

}