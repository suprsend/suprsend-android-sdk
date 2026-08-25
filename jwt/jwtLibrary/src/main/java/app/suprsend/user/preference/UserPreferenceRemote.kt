package app.suprsend.user.preference

import app.suprsend.SSInternal
import app.suprsend.base.NetworkClient
import app.suprsend.base.SSConstants
import app.suprsend.log.Logger
import app.suprsend.model.ApiResponse
import app.suprsend.utils.urlEncode

object UserPreferenceRemote {

    var networkClient = NetworkClient()

    fun request(
        path: String,
        requestJson: String? = null,
        requestMethod: String = if (requestJson == null) "GET" else "POST"
    ): ApiResponse {
        val distinctId = urlEncode(SSInternal.suprSendData.distinctId ?: "")
        val operationStatus = SSInternal.refreshTokenIfRequired(distinctId = distinctId)
        if (!operationStatus.isSuccess()) {
            Logger.e(SSConstants.TAG_SUPRSEND, operationStatus.message ?: "No response", operationStatus.exception)
            return operationStatus
        }

        val host = SSInternal.suprSendData.host
        val url = when {
            path.startsWith("http://") || path.startsWith("https://") -> path
            host.endsWith("/") -> host + path
            else -> "$host/$path"
        }

        return networkClient.httpCall(
            url = url,
            authorization = SSInternal.suprSendData.publicApiKey ?: "",
            requestMethod = requestMethod,
            requestJson = requestJson,
            headers = SSInternal.addSSSignature()
        )
    }
}
