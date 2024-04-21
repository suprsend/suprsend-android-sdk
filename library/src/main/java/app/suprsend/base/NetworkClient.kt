package app.suprsend.base

import app.suprsend.BuildConfig
import app.suprsend.config.ConfigHelper
import app.suprsend.event.Algo
import app.suprsend.event.EventFlushHandler
import app.suprsend.event.HttPResponse
import app.suprsend.event.toMD5
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date

internal fun httpCall(
    urL: String,
    authorization: String,
    date: String,
    requestJson: String? = null,
    requestMethod: String = "POST"
): HttPResponse {
    Logger.i(SSConstants.TAG_SUPRSEND, "Requesting : $urL requestJson:$requestJson")
    var connection: HttpURLConnection? = null
    var inputStream: InputStream? = null
    var httpResponse = HttPResponse(400)
    try {
        val url = URL(urL)
        connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = requestMethod
        connection.setRequestProperty("Content-Type", "application/json")
        connection.addRequestProperty("Authorization", authorization)
        connection.addRequestProperty("Date", date)
        connection.useCaches = false
        if (requestMethod.equals("POST", true))
            connection.doOutput = true
        connection.doInput = true

        //Send request
        if (requestJson != null) {
            val wr = DataOutputStream(connection.outputStream)
            wr.writeBytes(requestJson)
            wr.close()
        }

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

        httpResponse = HttPResponse(connection.responseCode, response.toString())
    } catch (e: Exception) {
        Logger.e(EventFlushHandler.TAG, "", e)
    } finally {
        connection?.disconnect()
    }
    Logger.i(SSConstants.TAG_SUPRSEND, "Response Received : $urL \nCode : ${httpResponse.statusCode}")
    if (BuildConfig.DEBUG) {
        Logger.i(SSConstants.TAG_SUPRSEND, "Response : ${httpResponse.response}")
    }
    return httpResponse
}

internal fun createAuthorization(
    requestJson: String? = null,
    requestURI: String,
    date: String,
    requestMethod: String = "POST",
    contentType: String = "application/json"
): String {
    val contentMd5 = requestJson?.toMD5() ?: ""

    val secret = ConfigHelper.get(SSConstants.CONFIG_API_SECRET) ?: ""
    val envKey = ConfigHelper.get(SSConstants.CONFIG_API_KEY) ?: ""

    val stringToSign = requestMethod + "\n" +
            contentMd5 + "\n" +
            contentType + "\n" +
            date + "\n" +
            requestURI

    val signature = Algo.base64(Algo.generateHashWithHmac256(secret, stringToSign))

    return "$envKey:$signature"
}

fun getDate(): String {
    return Date().toString()
}