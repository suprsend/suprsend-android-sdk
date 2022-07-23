package app.suprsend.event

import app.suprsend.BuildConfig
import app.suprsend.base.Logger
import app.suprsend.base.SSConstants
import app.suprsend.base.SdkAndroidCreator
import app.suprsend.base.generateSignature
import app.suprsend.base.makeHttpRequest
import app.suprsend.base.toKotlinJsonObject
import app.suprsend.config.ConfigHelper
import app.suprsend.database.Event_Model
import java.security.MessageDigest
import java.util.Date
import org.json.JSONArray

internal object EventFlushHandler {
    const val TAG = "flush"

    fun flushEvents() {
        val eventLocalDatasource = SdkAndroidCreator.eventLocalDatasource
        var eventModelList: List<Event_Model> = eventLocalDatasource.getEvents(SSConstants.FLUSH_EVENT_PAYLOAD_SIZE)
        if (eventModelList.isEmpty()) {
            Logger.i(TAG, "No events found")
            return
        }
        val baseUrl = ConfigHelper.get(SSConstants.CONFIG_API_BASE_URL) ?: SSConstants.DEFAULT_BASE_API_URL
        while (eventModelList.isNotEmpty()) {

            val jsonArray = JSONArray()
            eventModelList.forEach { eventModel ->
                jsonArray.put(eventModel.value.toKotlinJsonObject())
            }
            val route = "/event/"
            val envKey = ConfigHelper.get(SSConstants.CONFIG_API_KEY) ?: ""
            val body = jsonArray.toString()
            val date = Date().toString()
            val signature = generateSignature(body = body, method = "POST", route = route, date = date)

            val httpResponse = makeHttpRequest(
                method = "POST",
                urL = "$baseUrl$route",
                authorization = "$envKey:$signature",
                body = body,
                date = date
            )
            if (httpResponse.statusCode != 202 || BuildConfig.DEBUG) {
                Logger.i(TAG, "${httpResponse.statusCode} \n$body \n${httpResponse.response}")
            } else {
                Logger.i(TAG, "statusCode:${httpResponse.statusCode}")
            }
            if (httpResponse.statusCode == 202) {
                eventLocalDatasource.delete(eventModelList.map { event -> event.id!! }.joinToString())
                eventModelList = eventLocalDatasource.getEvents(SSConstants.FLUSH_EVENT_PAYLOAD_SIZE)
            } else {
                eventModelList = emptyList()
                break
            }
        }
    }
}

internal fun String.toMD5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray(Charsets.UTF_8))
    return bytes.toHex()
}

private fun ByteArray.toHex(): String {
    return joinToString("") { "%02x".format(it) }
}
