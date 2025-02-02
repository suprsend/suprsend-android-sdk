package app.suprsend.model

import app.suprsend.base.Response
import org.json.JSONObject

data class SuprSendOptions(
    val host: String? = null
)

data class ApiResponse(
    val status: ResponseStatus,
    val statusCode: Int? = null,
    val body: String? = null,
    val errorType: ErrorType? = null,
    val exception: Exception? = null,
    val message: String? = null
) {
    fun isSuccess(): Boolean {
        return status == ResponseStatus.SUCCESS
    }

}

fun ApiResponse.toResponse(): Response<JSONObject> {
    return if (this.isSuccess())
        Response.Success(JSONObject(body ?: ""))
    else Response.Error(ex = IllegalStateException("Something went wrong $statusCode"),message=this.message?:"")
}


enum class ResponseStatus {
    SUCCESS, ERROR
}

enum class ErrorType {
    VALIDATION_ERROR,
    NETWORK_ERROR,
    UNKNOWN_ERROR,
    PERMISSION_DENIED,
    UNSUPPORTED_ACTION,
    NOT_FOUND,
}