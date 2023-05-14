package app.suprsend.user.preference

import app.suprsend.base.mapToEnum
import org.json.JSONArray
import org.json.JSONObject

object UserPreferenceParser {

    fun parse(jsonObject: JSONObject): UserPreferences {
        return UserPreferences(
            categories = getCategories(jsonObject.optJSONArray("categories")),
            channelPreferences = getChannelPreferences(jsonObject.optJSONArray("channel_preferences"))
        )
    }

    private fun getCategories(categoriesJA: JSONArray?): List<Category> {
        return parseJA(categoriesJA) { categoryJO ->
            Category(
                rootCategory = categoryJO.optString("root_category"),
                sections = getSections(categoryJO.optJSONArray("sections"))
            )
        }
    }

    private fun getSections(sectionJA: JSONArray?): List<Section> {
        return parseJA(sectionJA) { sectionJO ->
            Section(
                name = sectionJO.optString("name"),
                subCategories = getSubCategories(sectionJO.optJSONArray("subcategories"))
            )
        }
    }

    private fun getSubCategories(subCategoriesJA: JSONArray?): List<SubCategory> {
        return parseJA(subCategoriesJA) { subCategoryJO ->
            SubCategory(
                name = subCategoryJO.optString("name"),
                category = subCategoryJO.optString("category"),
                description = subCategoryJO.optString("description"),
                defaultPreference = subCategoryJO.optString("default_preference"),
                preference = getPreference(subCategoryJO.optString("preference")),
                isEditable = subCategoryJO.optBoolean("is_editable"),
                channels = getChannels(subCategoryJO.optJSONArray("channels"))
            )
        }
    }

    private fun getChannels(channelJA: JSONArray?): List<Channel> {
        return parseJA(channelJA) { channelJO ->
            Channel(
                channel = channelJO.optString("channel"),
                preference = getPreference(channelJO.optString("preference")),
                isEditable = channelJO.optBoolean("is_editable")
            )
        }
    }

    private fun getChannelPreferences(channelPreferenceJA: JSONArray?): List<ChannelPreference> {
        return parseJA(channelPreferenceJA) { channelPreferenceJO ->
            ChannelPreference(
                channel = channelPreferenceJO.optString("channel"),
                isRestricted = channelPreferenceJO.optBoolean("is_restricted")
            )
        }
    }

    fun getPreference(preferenceString: String): Preference {
        return preferenceString.mapToEnum<Preference>(ignoreCase = true) ?: Preference.OPT_OUT
    }

    private fun <T> parseJA(jsonArray: JSONArray?, createObject: (jo: JSONObject) -> T): List<T> {
        if (jsonArray == null)
            return listOf()
        val channels = arrayListOf<T>()
        for (i in 0 until jsonArray.length()) {
            val jo = jsonArray.getJSONObject(i)
            channels.add(
                createObject(jo)
            )
        }
        return channels
    }
}