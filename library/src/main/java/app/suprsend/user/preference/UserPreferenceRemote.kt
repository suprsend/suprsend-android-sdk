package app.suprsend.user.preference

import app.suprsend.SSApiInternal
import app.suprsend.base.Logger
import app.suprsend.base.SSConstants
import app.suprsend.base.createAuthorization
import app.suprsend.base.getDate
import app.suprsend.base.httpCall
import app.suprsend.base.urlEncode
import app.suprsend.config.ConfigHelper
import app.suprsend.event.EventFlushHandler
import app.suprsend.event.HttPResponse
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

object UserPreferenceRemote {

    fun preference(brandId: String?): HttPResponse {
        return callApi(
            route = "full_preference",
            queryParams = "brandId=${brandId ?: ""}"
        )
    }

    fun fetchCategories(
        brandId: String?,
        limit: Int?,
        offset: Int?
    ): HttPResponse {
        return callApi(
            route = "category",
            queryParams = "brandId=${brandId ?: ""}&limit=${limit?.toString() ?: ""}&offset=${offset?.toString() ?: ""}"
        )
    }

    fun fetchCategory(
        category: String,
        brandId: String?
    ): HttPResponse {
        return callApi(
            route = "category/$category",
            queryParams = "brandId=${brandId ?: ""}"
        )
    }

    fun fetchOverallChannelPreferences(): HttPResponse {
        return callApi(
            route = "channel_preference"
        )
    }

    fun updateCategoryPreferences(category: String, brandId: String?, body: String): HttPResponse {
        return callApi(
            route = "category/$category",
            queryParams = "brandId=${brandId ?: ""}",
            requestJson = body
        )
    }

    fun updateChannelPreference(
        channel: String,
        isRestricted: Boolean
    ): HttPResponse {
        val body = JSONObject()
        val chanelPrefJA = JSONArray()
        val chanelJO = JSONObject()
        chanelJO.put("channel", channel)
        chanelJO.put("is_restricted", isRestricted)
        chanelPrefJA.put(chanelJO)
        body.put("channel_preferences", chanelPrefJA)
        return callApi(
            route = "channel_preference",
            requestJson = body.toString()
        )
    }

    private fun callApi(
        requestJson: String? = null,
        route: String,
        queryParams: String? = null
    ): HttPResponse {
        val distinctId = ConfigHelper.get(SSConstants.CONFIG_USER_ID) ?: ""
        val requestURI = if (queryParams == null) {
            "/v1/subscriber/${distinctId}/$route"
        } else {
            "/v1/subscriber/${distinctId}/$route?$queryParams"
        }

        val date = getDate()
        val requestMethod = if (requestJson == null) "GET" else "POST"

        val authorization = createAuthorization(
            requestURI = requestURI,
            date = date,
            requestMethod = requestMethod,
            requestJson = requestJson
        )
        val baseUrl = SSApiInternal.getBaseUrl()
        val url = "$baseUrl$requestURI"
        Logger.i(SSConstants.TAG_SUPRSEND, "Requesting : $url requestJson:$requestJson")
        val httpResponse = httpCall(
            urL = url,
            authorization = authorization,
            date = date,
            requestMethod = requestMethod,
            requestJson = requestJson
        )

        Logger.i(EventFlushHandler.TAG, "${httpResponse.statusCode} \n${httpResponse.response}")
        return httpResponse
    }
}