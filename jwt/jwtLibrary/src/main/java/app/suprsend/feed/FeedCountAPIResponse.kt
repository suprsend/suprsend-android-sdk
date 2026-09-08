package app.suprsend.feed

import app.suprsend.model.ResponseError
import app.suprsend.model.ResponseStatus
import org.json.JSONObject

class FeedCountAPIResponse(
    override val status: ResponseStatus,
    override val statusCode: StatusCode? = null,
    override val body: FeedCountData? = null,
    override val error: ResponseError? = null
) : Response<FeedCountData> {

    internal companion object {

        fun error(error: ResponseError?, statusCode: StatusCode? = null): FeedCountAPIResponse {
            return FeedCountAPIResponse(
                status = ResponseStatus.ERROR,
                statusCode = statusCode,
                body = null,
                error = error
            )
        }
    }
}

/**
 * Badge count plus the per store unseen counts. The backend returns the store counts as
 * dynamic keys alongside `badge`, so anything numeric that isn't `badge` is a store count.
 */
class FeedCountData(
    val badge: Int? = null,
    val storeCounts: Map<String, Int> = mapOf()
) {
    internal companion object {
        fun fromJson(jsonObject: JSONObject): FeedCountData {
            var badge: Int? = null
            val storeCounts = mutableMapOf<String, Int>()
            jsonObject.keys().forEach { key ->
                if (key == "badge") {
                    badge = jsonObject.safeInt(key)
                } else {
                    val count = jsonObject.opt(key)
                    if (count is Number) {
                        storeCounts[key] = count.toInt()
                    }
                }
            }
            return FeedCountData(badge = badge, storeCounts = storeCounts)
        }
    }
}
