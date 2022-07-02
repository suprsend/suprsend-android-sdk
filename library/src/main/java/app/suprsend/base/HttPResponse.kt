package app.suprsend.base

internal data class HttPResponse(
    val statusCode: Int,
    val response: String? = null
)