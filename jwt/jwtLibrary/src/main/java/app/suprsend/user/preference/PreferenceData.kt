package app.suprsend.user.preference

import app.suprsend.model.ApiResponse
import app.suprsend.model.ErrorType
import app.suprsend.model.ResponseError
import app.suprsend.model.ResponseStatus
import org.json.JSONArray
import org.json.JSONObject

class CategoryChannel(
    val channel: String,
    var preference: PreferenceOptions,
    val isEditable: Boolean
)

class Category(
    val name: String,
    val category: String,
    val description: String?,
    var preference: PreferenceOptions,
    val isEditable: Boolean,
    val channels: List<CategoryChannel>?
)

class Section(
    val name: String?,
    val description: String?,
    val subcategories: List<Category>?
)

class ChannelPreference(
    val channel: String,
    var isRestricted: Boolean
)

class PreferenceData(
    val sections: List<Section>? = null,
    val channelPreferences: List<ChannelPreference>? = null
)

data class PreferenceAPIResponse(
    val status: ResponseStatus,
    val statusCode: Int? = null,
    val body: PreferenceData? = null,
    val error: ResponseError? = null
) {
    fun isSuccess(): Boolean = status == ResponseStatus.SUCCESS

    companion object {
        fun success(statusCode: Int? = null, body: PreferenceData? = null): PreferenceAPIResponse {
            return PreferenceAPIResponse(
                status = ResponseStatus.SUCCESS,
                statusCode = statusCode,
                body = body,
                error = null
            )
        }

        fun error(error: ResponseError?, statusCode: Int? = null): PreferenceAPIResponse {
            return PreferenceAPIResponse(
                status = ResponseStatus.ERROR,
                statusCode = statusCode,
                body = null,
                error = error
            )
        }
    }
}

internal class RequestPayload(
    val preference: PreferenceOptions,
    val optOutChannels: List<String>?
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("preference", preference.rawValue)
        if (optOutChannels != null) {
            json.put("opt_out_channels", JSONArray(optOutChannels))
        }
        return json.toString()
    }
}

internal class ChannelRequestPayload(
    val channelPreferences: List<ChannelPreference>
) {
    fun toJson(): String {
        val json = JSONObject()
        val channelPreferencesJa = JSONArray()
        channelPreferences.forEach { channelPreference ->
            val channelJo = JSONObject()
            channelJo.put("channel", channelPreference.channel)
            channelJo.put("is_restricted", channelPreference.isRestricted)
            channelPreferencesJa.put(channelJo)
        }
        json.put("channel_preferences", channelPreferencesJa)
        return json.toString()
    }
}

internal fun ApiResponse.toPreferenceAPIResponse(): PreferenceAPIResponse {
    return if (isSuccess()) {
        val preferenceData = try {
            if (body.isNullOrBlank()) null else UserPreferenceParser.parse(JSONObject(body))
        } catch (ignored: Exception) {
            null
        }
        PreferenceAPIResponse.success(statusCode = statusCode, body = preferenceData)
    } else {
        PreferenceAPIResponse.error(
            error = ResponseError(
                type = errorType ?: ErrorType.UNKNOWN_ERROR,
                message = message
            ),
            statusCode = statusCode
        )
    }
}
