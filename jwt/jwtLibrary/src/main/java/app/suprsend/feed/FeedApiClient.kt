package app.suprsend.feed

import app.suprsend.SSInternal
import app.suprsend.base.SSConstants
import app.suprsend.log.Logger
import app.suprsend.model.ApiResponse
import app.suprsend.model.ErrorType
import app.suprsend.model.ResponseError
import app.suprsend.model.ResponseStatus
import app.suprsend.utils.mapToEnum
import app.suprsend.utils.toKotlinJsonObject
import org.json.JSONObject

internal enum class RequestType(val value: String) {
    GET("GET"),
    POST("POST"),
    PATCH("PATCH")
}

/**
 * HTTP layer of the feed. Every call refreshes an expired user token first and rides along the
 * public key, user agent and `x-ss-signature` headers added by [SSInternal.addSSSignature].
 */
internal class FeedApiClient {

    fun request(url: String, type: RequestType): ApiResponse {
        return request(url = url, type = type, payload = null)
    }

    fun request(url: String, type: RequestType, payload: JSONObject?): ApiResponse {
        val distinctId = SSInternal.suprSendData.distinctId
        if (distinctId.isNullOrBlank()) {
            return ApiResponse(
                status = ResponseStatus.ERROR,
                errorType = ErrorType.VALIDATION_ERROR,
                message = "User isn't authenticated. Call identify method before performing any action"
            )
        }

        val refreshStatus = SSInternal.refreshTokenIfRequired(distinctId = distinctId)
        if (!refreshStatus.isSuccess()) {
            // Mirrors the web and iOS SDKs: a failed refresh doesn't block the call, the API
            // rejects the stale token and checkStatusCodeAndRemoveLocalToken recovers from it.
            Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Feed : ${refreshStatus.message}")
        }

        val response = SSInternal.networkClient.httpCall(
            url = url,
            requestMethod = type.value,
            authorization = SSInternal.suprSendData.publicApiKey ?: "",
            requestJson = if (type == RequestType.GET) null else (payload ?: JSONObject()).toString(),
            headers = SSInternal.addSSSignature()
        )

        if (!response.isSuccess()) {
            SSInternal.checkStatusCodeAndRemoveLocalToken(response.body)
        }

        return response
    }
}

private class ParsedResponse(
    val status: ResponseStatus,
    val statusCode: StatusCode?,
    val json: JSONObject?,
    val error: ResponseError?
)

private fun ApiResponse.parse(): ParsedResponse {
    val json = if (body.isNullOrBlank()) null else body.toKotlinJsonObject()
    val errorJO = json?.optJSONObject("error")
    val bodyError = if (errorJO == null) {
        null
    } else {
        ResponseError(
            type = errorJO.optString("type").toUpperCase().mapToEnum(ErrorType.UNKNOWN_ERROR),
            message = errorJO.optString("message")
        )
    }

    val bodyStatus = json?.optString("status")?.toUpperCase()?.mapToEnum<ResponseStatus>()
    val resolvedStatus = when {
        !isSuccess() -> ResponseStatus.ERROR
        bodyStatus != null -> bodyStatus
        else -> ResponseStatus.SUCCESS
    }

    val resolvedError = when {
        bodyError != null -> bodyError
        resolvedStatus == ResponseStatus.ERROR -> ResponseError(
            type = errorType ?: ErrorType.UNKNOWN_ERROR,
            message = message ?: body
        )
        else -> null
    }

    if (resolvedStatus == ResponseStatus.ERROR) {
        Logger.e(SSConstants.TAG_SUPRSEND_INBOX, "Feed : $statusCode : ${resolvedError?.message}")
    }

    return ParsedResponse(
        status = resolvedStatus,
        statusCode = statusCode,
        json = json,
        error = resolvedError
    )
}

internal fun ApiResponse.toFeedAPIResponse(): FeedAPIResponse {
    val parsed = parse()
    return FeedAPIResponse(
        status = parsed.status,
        statusCode = parsed.statusCode,
        body = if (parsed.status == ResponseStatus.SUCCESS && parsed.json != null) FeedData.fromJson(parsed.json) else null,
        error = parsed.error
    )
}

internal fun ApiResponse.toFeedCountAPIResponse(): FeedCountAPIResponse {
    val parsed = parse()
    return FeedCountAPIResponse(
        status = parsed.status,
        statusCode = parsed.statusCode,
        body = if (parsed.status == ResponseStatus.SUCCESS && parsed.json != null) FeedCountData.fromJson(parsed.json) else null,
        error = parsed.error
    )
}

internal fun ApiResponse.toFeedDetailAPIResponse(): FeedDetailAPIResponse {
    val parsed = parse()
    return FeedDetailAPIResponse(
        status = parsed.status,
        statusCode = parsed.statusCode,
        body = if (parsed.status == ResponseStatus.SUCCESS && parsed.json != null) IRemoteNotification.fromJson(parsed.json) else null,
        error = parsed.error
    )
}

internal fun ApiResponse.toAPIResponse(): APIResponse {
    val parsed = parse()
    val bodyJO = parsed.json?.optJSONObject("body")
    val responseBody = if (bodyJO == null) {
        null
    } else {
        val values = mutableMapOf<String, String>()
        bodyJO.keys().forEach { key -> values[key] = bodyJO.optString(key) }
        values
    }
    return APIResponse(
        status = parsed.status,
        statusCode = parsed.statusCode,
        body = responseBody,
        error = parsed.error
    )
}
