package app.suprsend.user.preference

import app.suprsend.SSInternal
import app.suprsend.base.NetworkClient
import app.suprsend.base.SSConstants
import app.suprsend.base.createSubUrl
import app.suprsend.log.Logger
import app.suprsend.model.ApiResponse
import app.suprsend.utils.urlEncode
import org.json.JSONArray
import org.json.JSONObject

object UserPreferenceRemote {

    var networkClient = NetworkClient()

    fun preference(tenantId: String?, showOptOutChannels: Boolean): ApiResponse {
        return refreshTokenIfRequiredAndCallApi(
            route = "full_preference",
            queryParams = createSubUrl(
                mapOf(
                    "tenant_id" to tenantId,
                    "show_opt_out_channels" to showOptOutChannels.toString()
                )
            )
        )
    }

    fun fetchCategories(
        tenantId: String?,
        limit: Int?,
        offset: Int?,
        showOptOutChannels: Boolean
    ): ApiResponse {
        return refreshTokenIfRequiredAndCallApi(
            route = "category",
            queryParams = createSubUrl(
                mapOf(
                    "tenant_id" to tenantId,
                    "show_opt_out_channels" to showOptOutChannels.toString(),
                    "offset" to offset?.toString(),
                    "limit" to limit?.toString()
                )
            )
        )
    }

    fun fetchCategory(
        category: String,
        tenantId: String?,
        showOptOutChannels: Boolean
    ): ApiResponse {
        return refreshTokenIfRequiredAndCallApi(
            route = "category/$category",
            queryParams = createSubUrl(
                mapOf(
                    "tenant_id" to tenantId,
                    "show_opt_out_channels" to showOptOutChannels.toString()
                )
            )
        )
    }

    fun fetchOverallChannelPreferences(): ApiResponse {
        return refreshTokenIfRequiredAndCallApi(
            route = "channel_preference"
        )
    }

    fun updateCategoryPreferences(
        category: String,
        tenantId: String?,
        body: String,
        showOptOutChannels: Boolean
    ): ApiResponse {
        return refreshTokenIfRequiredAndCallApi(
            route = "category/$category",
            requestJson = body,
            requestMethod = "PATCH",
            queryParams = createSubUrl(
                mapOf(
                    "tenant_id" to tenantId,
                    "show_opt_out_channels" to showOptOutChannels.toString()
                )
            )
        )
    }

    fun updateOverallChannelPreference(
        channel: String,
        isRestricted: Boolean
    ): ApiResponse {

        val body = JSONObject()
        val chanelPrefJA = JSONArray()
        val chanelJO = JSONObject()
        chanelJO.put("channel", channel)
        chanelJO.put("is_restricted", isRestricted)
        chanelPrefJA.put(chanelJO)
        body.put("channel_preferences", chanelPrefJA)
        return refreshTokenIfRequiredAndCallApi(
            route = "channel_preference",
            requestJson = body.toString(),
            requestMethod = "PATCH"
        )
    }

    private fun refreshTokenIfRequiredAndCallApi(
        requestJson: String? = null,
        route: String,
        queryParams: String? = null,
        requestMethod: String = if (requestJson == null) "GET" else "POST"
    ): ApiResponse {
        val distinctId = urlEncode(SSInternal.suprSendData.distinctId ?: "")

        val operationStatus = SSInternal.refreshTokenIfRequired(distinctId = distinctId)

        if (!operationStatus.isSuccess()) {
            Logger.e(SSConstants.TAG_SUPRSEND, operationStatus.message ?: "No response", operationStatus.exception)
            return operationStatus
        }

        val requestURI = if (queryParams == null) {
            "v2/subscriber/${distinctId}/$route"
        } else {
            "v2/subscriber/${distinctId}/$route?$queryParams"
        }
        val baseUrl = SSInternal.suprSendData.host
        val url = "$baseUrl/$requestURI"

        return networkClient.httpCall(
            url = url,
            authorization = SSInternal.suprSendData.publicApiKey ?: "",
            requestMethod = requestMethod,
            requestJson = requestJson,
            headers = SSInternal.addSSSignature()
        )
    }
}