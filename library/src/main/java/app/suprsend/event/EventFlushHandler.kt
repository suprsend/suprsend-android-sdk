package app.suprsend.event

import app.suprsend.BuildConfig
import app.suprsend.base.Logger
import app.suprsend.base.SSConstants
import app.suprsend.base.SdkAndroidCreator
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

            val httpResponse = httpPost(
                urL = "$baseUrl$requestURI",
                authorization = "$envKey:$signature",
                body = requestJson,
                date = date
            )

            Logger.i(TAG, "${httpResponse.statusCode} \n$requestJson \n${httpResponse.response}")

            if (httpResponse.statusCode == 202) {
                eventLocalDatasource.delete(eventModelList.map { event -> event.id!! }.joinToString())
                eventModelList = eventLocalDatasource.getEvents(SSConstants.FLUSH_EVENT_PAYLOAD_SIZE)
            } else {
                eventModelList = emptyList()
                break
            }
        }
    }

    private fun httpPost(urL: String, authorization: String, date: String, body: String): HttPResponse {

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        try {
            val url = URL(urL)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.addRequestProperty("Authorization", authorization)
            connection.addRequestProperty("Date", date)
            connection.useCaches = false
            connection.doOutput = true
            connection.doInput = true

            //Send request
            val wr = DataOutputStream(connection.outputStream)
            wr.writeBytes(body)
            wr.close()

            //Get Response
            try {
                inputStream = connection.inputStream
            } catch (ioe: IOException) {

                val statusCode = connection.responseCode
                if (statusCode >= 400) {
                    inputStream = connection.errorStream
                }
            }
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val response = StringBuilder()
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                response.append(line)
                response.append('\r')
            }
            bufferedReader.close()

            return HttPResponse(connection.responseCode, response.toString())
        } catch (e: Exception) {
            Logger.e(TAG, "", e)
        } finally {
            connection?.disconnect()
        }
        return HttPResponse(400)
    }
}

data class HttPResponse(
    val statusCode: Int,
    val response: String? = null
)

internal fun String.toMD5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray(Charsets.UTF_8))
    return bytes.toHex()
}

private fun ByteArray.toHex(): String {
    return joinToString("") { "%02x".format(it) }
}
