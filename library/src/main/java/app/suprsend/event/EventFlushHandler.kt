package app.suprsend.event

import app.suprsend.base.Logger
import app.suprsend.base.SSConstants
import app.suprsend.base.SdkAndroidCreator
import app.suprsend.base.makeHttpPost
import app.suprsend.base.toKotlinJsonObject
import app.suprsend.config.ConfigHelper
import app.suprsend.database.Event_Model
import org.json.JSONArray
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Date

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
            val requestJson = jsonArray.toString()
            val httpVerb = "POST"
            val contentMd5 = requestJson.toMD5()
            val contentType = "application/json"
            val date = Date().toString()
            val requestURI = "/event/"
            val envKey = ConfigHelper.get(SSConstants.CONFIG_API_KEY) ?: ""
            val secret = ConfigHelper.get(SSConstants.CONFIG_API_SECRET) ?: ""

            val stringToSign = httpVerb + "\n" +
                contentMd5 + "\n" +
                contentType + "\n" +
                date + "\n" +
                requestURI

            val signature = Algo.base64(Algo.generateHashWithHmac256(secret, stringToSign))

            val httpResponse = makeHttpPost(
                urL = "$baseUrl$requestURI",
                authorization = "$envKey:$signature",
                body = requestJson,
                date = date
            )

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
