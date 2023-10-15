package app.suprsend.base

internal data class HttPResponse(
    val statusCode: Int,
    val response: String? = null
) {
    fun ok(): Boolean {
        return statusCode == 200
    }

    fun accepted(): Boolean {
        return statusCode == 202
    }
}
