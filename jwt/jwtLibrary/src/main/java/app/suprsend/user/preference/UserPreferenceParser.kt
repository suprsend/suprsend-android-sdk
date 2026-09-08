package app.suprsend.user.preference

import app.suprsend.utils.map
import app.suprsend.utils.safeBooleanDefault
import app.suprsend.utils.safeJsonArray
import app.suprsend.utils.safeString
import app.suprsend.utils.safeStringDefault
import org.json.JSONArray
import org.json.JSONObject

object UserPreferenceParser {

    fun parse(jsonObject: JSONObject): PreferenceData {
        return PreferenceData(
            sections = jsonObject.safeJsonArray("sections")?.let { getSections(it) },
            channelPreferences = jsonObject.safeJsonArray("channel_preferences")?.let { getChannelPreferences(it) }
        )
    }

    private fun getSections(sectionJA: JSONArray): List<Section> {
        return parseJA(sectionJA) { sectionJO ->
            Section(
                name = sectionJO.safeString("name"),
                description = sectionJO.safeString("description"),
                subcategories = sectionJO.safeJsonArray("subcategories")?.let { getCategories(it) }
            )
        }
    }

    private fun getCategories(categoriesJA: JSONArray): List<Category> {
        return parseJA(categoriesJA) { categoryJO ->
            Category(
                name = categoryJO.safeStringDefault("name"),
                category = categoryJO.safeStringDefault("category"),
                description = categoryJO.safeString("description"),
                preference = PreferenceOptions.from(categoryJO.safeString("preference")),
                isEditable = categoryJO.safeBooleanDefault("is_editable"),
                channels = categoryJO.safeJsonArray("channels")?.let { getChannels(it) }
            )
        }
    }

    private fun getChannels(channelJA: JSONArray): List<CategoryChannel> {
        return parseJA(channelJA) { channelJO ->
            CategoryChannel(
                channel = channelJO.safeStringDefault("channel"),
                preference = PreferenceOptions.from(channelJO.safeString("preference")),
                isEditable = channelJO.safeBooleanDefault("is_editable")
            )
        }
    }

    private fun getChannelPreferences(channelPreferenceJA: JSONArray): List<ChannelPreference> {
        return parseJA(channelPreferenceJA) { channelPreferenceJO ->
            ChannelPreference(
                channel = channelPreferenceJO.safeStringDefault("channel"),
                isRestricted = channelPreferenceJO.safeBooleanDefault("is_restricted")
            )
        }
    }

    private fun <T> parseJA(jsonArray: JSONArray, createObject: (jo: JSONObject) -> T): List<T> {
        return jsonArray.map { jo -> createObject(jo) }
    }
}