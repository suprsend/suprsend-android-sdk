package app.suprsend.event

import app.suprsend.SSApiInternal
import app.suprsend.base.HttPResponse
import app.suprsend.base.Logger
import app.suprsend.base.SSConstants
import app.suprsend.base.SdkAndroidCreator
import app.suprsend.base.createAuthorization
import app.suprsend.base.getDate
import app.suprsend.base.httpCall
import app.suprsend.base.toKotlinJsonObject
import app.suprsend.database.Event_Model
import org.json.JSONArray
import java.security.MessageDigest

internal object EventFlushHandler {
    const val TAG = "flush"

    fun flush() {

        val eventLocalDatasource = SdkAndroidCreator.eventLocalDatasource
        var eventModelList: List<Event_Model> = eventLocalDatasource.getEvents(SSConstants.FLUSH_EVENT_PAYLOAD_SIZE)
        if (eventModelList.isEmpty()) {
            Logger.i(TAG, "No events found")
            return
        }

        while (eventModelList.isNotEmpty()) {

            val httpResponse = flushEvents(eventModelList)

            if (httpResponse.statusCode == 202) {
                eventLocalDatasource.delete(eventModelList.map { event -> event.id!! }.joinToString())
                eventModelList = eventLocalDatasource.getEvents(SSConstants.FLUSH_EVENT_PAYLOAD_SIZE)
            } else {
                eventModelList = emptyList()
                break
            }
        }
    }

    fun flushEvents(eventModelList: List<Event_Model>): HttPResponse {
        val requestJson = eventModelList.toJsonArray().toString()

        val requestURI = "/event/"
        val date = getDate()

        val authorization = createAuthorization(
            requestJson = requestJson,
            requestURI = requestURI,
            date = date
        )
        val baseUrl = SSApiInternal.getBaseUrl()
        val httpResponse = httpCall(
            urL = "$baseUrl$requestURI",
            authorization = authorization,
            requestJson = requestJson,
            date = date
        )

        Logger.i(TAG, "${httpResponse.statusCode} \n$requestJson \n${httpResponse.response}")
        return httpResponse
    }

    private fun List<Event_Model>.toJsonArray(): JSONArray {
        val jsonArray = JSONArray()
        forEach { eventModel ->
            jsonArray.put(eventModel.value.toKotlinJsonObject())
        }
        return jsonArray
    }


}