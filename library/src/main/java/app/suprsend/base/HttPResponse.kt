package app.suprsend.base

import org.json.JSONObject

data class HttPResponse(
    val statusCode: Int,
    val response: String? = null
){
    fun ok(): Boolean {
        return statusCode == 200
    }

    fun accepted(): Boolean {
        return statusCode == 202
    }

}

fun HttPResponse.toResponse(): Response<JSONObject> {
    return if (ok()) {
        Response.Success(JSONObject(response ?: ""))
    } else {
        Response.Error(IllegalStateException("Something went wrong $statusCode"))
    }
}
