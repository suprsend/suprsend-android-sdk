package app.suprsend.feed

import app.suprsend.model.ResponseError
import app.suprsend.model.ResponseStatus

class FeedDetailAPIResponse(
    override val status: ResponseStatus,
    override val statusCode: StatusCode? = null,
    override val body: IRemoteNotification? = null,
    override val error: ResponseError? = null
) : Response<IRemoteNotification> {

    internal companion object {

        fun error(error: ResponseError?, statusCode: StatusCode? = null): FeedDetailAPIResponse {
            return FeedDetailAPIResponse(
                status = ResponseStatus.ERROR,
                statusCode = statusCode,
                body = null,
                error = error
            )
        }
    }
}
