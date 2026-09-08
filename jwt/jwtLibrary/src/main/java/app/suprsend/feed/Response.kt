package app.suprsend.feed

import app.suprsend.model.ResponseError
import app.suprsend.model.ResponseStatus

/** A type alias for a HTTP status code. */
typealias StatusCode = Int

/** A type alias for a JSON response body. */
typealias ResponseBody = Map<String, String>

/**
 * The [Response] interface defines the structure of a feed API response.
 *
 * It encapsulates the status, status code, body and error information of every
 * call made by [Feed].
 */
interface Response<T> {

    /** The status of the response (e.g. success or error). */
    val status: ResponseStatus

    /** The HTTP status code associated with the response. */
    val statusCode: StatusCode?

    /** The JSON response body. */
    val body: T?

    /** Any error that occurred during the request. */
    val error: ResponseError?

    fun isSuccess(): Boolean = status == ResponseStatus.SUCCESS
}

/**
 * Response of the notification actions (mark as read, seen, archived etc.).
 */
class APIResponse(
    override val status: ResponseStatus,
    override val statusCode: StatusCode? = null,
    override val body: ResponseBody? = null,
    override val error: ResponseError? = null
) : Response<ResponseBody> {

    internal companion object {

        fun success(statusCode: StatusCode? = null, body: ResponseBody? = null): APIResponse {
            return APIResponse(
                status = ResponseStatus.SUCCESS,
                statusCode = statusCode,
                body = body,
                error = null
            )
        }

        fun error(error: ResponseError?, statusCode: StatusCode? = null): APIResponse {
            return APIResponse(
                status = ResponseStatus.ERROR,
                statusCode = statusCode,
                body = null,
                error = error
            )
        }
    }
}
