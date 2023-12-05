package app.suprsend.base

import app.suprsend.config.ConfigHelper
import app.suprsend.event.Algo
import app.suprsend.event.EventFlushHandler
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Date

internal fun httpCall(
    urL: String,
    authorization: String,
    date: String,
    requestJson: String? = null,
    requestMethod:String = "POST"
): HttPResponse {
    Logger.i(SSConstants.TAG_SUPRSEND,"Url : $urL")
    var connection: HttpURLConnection? = null
    var inputStream: InputStream? = null
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
        if(requestJson!=null) {
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

        return HttPResponse(connection.responseCode, response.toString())
    } catch (e: Exception) {
        Logger.e(EventFlushHandler.TAG, "", e)
    } finally {
        connection?.disconnect()
    }
    return HttPResponse(400)
}

internal fun createAuthorization(
    requestJson: String?=null,
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

internal fun String.toMD5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray(Charsets.UTF_8))
    return bytes.toHex()
}

private fun ByteArray.toHex(): String {
    return joinToString("") { "%02x".format(it) }
}

fun getDate(): String {
    return Date().toString()
}