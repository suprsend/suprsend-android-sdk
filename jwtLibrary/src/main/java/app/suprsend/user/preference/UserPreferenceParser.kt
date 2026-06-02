package app.suprsend.user.preference

import app.suprsend.utils.map
import app.suprsend.utils.safeBooleanDefault
import app.suprsend.utils.safeJsonArray
import app.suprsend.utils.safeStringDefault
import org.json.JSONArray
import org.json.JSONObject

object UserPreferenceParser {

    fun parse(jsonObject: JSONObject): PreferenceData {
        return PreferenceData(
            sections = getSections(jsonObject.safeJsonArray("sections")),
            channelPreferences = getChannelPreferences(jsonObject.safeJsonArray("channel_preferences"))
        )
    }

    private fun getSections(sectionJA: JSONArray?): List<Section> {
        return parseJA(sectionJA) { sectionJO ->
            Section(
                name = sectionJO.safeStringDefault("name"),
                description = sectionJO.safeStringDefault("description"),
                subCategories = getSubCategories(sectionJO.safeJsonArray("subcategories"))
            )
        }
    }

    private fun getSubCategories(subCategoriesJA: JSONArray?): List<SubCategory> {
        return parseJA(subCategoriesJA) { subCategoryJO ->
            SubCategory(
                name = subCategoryJO.safeStringDefault("name"),
                category = subCategoryJO.safeStringDefault("category"),
                description = subCategoryJO.safeStringDefault("description"),
                preferenceOptions = SSPreferenceInternal.getPreference(subCategoryJO.safeStringDefault("preference")),
                isEditable = subCategoryJO.safeBooleanDefault("is_editable"),
                channels = getChannels(subCategoryJO.safeJsonArray("channels"))
            )
        }
    }

    private fun getChannels(channelJA: JSONArray?): List<Channel> {
        return parseJA(channelJA) { channelJO ->
            Channel(
                channel = channelJO.safeStringDefault("channel"),
                preferenceOptions = SSPreferenceInternal.getPreference(channelJO.safeStringDefault("preference")),
                isEditable = channelJO.safeBooleanDefault("is_editable")
            )
        }
    }

    private fun getChannelPreferences(channelPreferenceJA: JSONArray?): List<ChannelPreference> {
        return parseJA(channelPreferenceJA) { channelPreferenceJO ->
            ChannelPreference(
                channel = channelPreferenceJO.safeStringDefault("channel"),
                isRestricted = channelPreferenceJO.safeBooleanDefault("is_restricted")
            )
        }
    }

    private fun <T> parseJA(jsonArray: JSONArray?, createObject: (jo: JSONObject) -> T): List<T> {
        if (jsonArray == null)
            return listOf()
        return jsonArray.map { jo -> createObject(jo) }
    }
}