package app.suprsend.user.preference

import app.suprsend.SSApiInternal
import app.suprsend.base.SSConstants
import app.suprsend.base.createAuthorization
import app.suprsend.base.getDate
import app.suprsend.base.httpCall
import app.suprsend.config.ConfigHelper
import app.suprsend.event.HttPResponse
import org.json.JSONArray
import org.json.JSONObject

object UserPreferenceRemote {

    fun preference(tenantId: String?): HttPResponse {
        return callApi(
            route = "full_preference",
            queryParams = "tenant_id=${tenantId ?: ""}"
        )
    }

    fun fetchCategories(
        tenantId: String?,
        limit: Int?,
        offset: Int?
    ): HttPResponse {
        return callApi(
            route = "category",
            queryParams = "tenant_id=${tenantId ?: ""}&limit=${limit?.toString() ?: ""}&offset=${offset?.toString() ?: ""}"
        )
    }

    fun fetchCategory(
        category: String,
        tenantId: String?
    ): HttPResponse {
        return callApi(
            route = "category/$category",
            queryParams = "tenant_id=${tenantId ?: ""}"
        )
    }

    fun fetchOverallChannelPreferences(): HttPResponse {
        return callApi(
            route = "channel_preference"
        )
    }

    fun updateCategoryPreferences(category: String, tenantId: String?, body: String): HttPResponse {
        return callApi(
            route = "category/$category",
            queryParams = "tenant_id=${tenantId ?: ""}",
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

        return httpCall(
            urL = url,
            authorization = authorization,
            date = date,
            requestMethod = requestMethod,
            requestJson = requestJson
        )
    }
}