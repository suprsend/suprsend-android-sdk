package app.suprsend.base

import app.suprsend.log.Logger
import app.suprsend.model.ApiResponse
import app.suprsend.model.ErrorType
import app.suprsend.model.ResponseStatus
import app.suprsend.utils.urlEncode
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


fun createSubUrl(keyValues: Map<String, String?>): String {
    var subUrl = ""
    keyValues.forEach { (key, value) ->
        if (value != null)
            subUrl = "$subUrl&$key=${urlEncode(value)}"
    }
    return subUrl
}

class NetworkClient {
    fun httpCall(
        url: String,
        authorization: String = "",
        date: String = "",
        requestJson: String? = null,
        requestMethod: String = "POST",
        headers: Map<String, String>? = null
    ): ApiResponse {
        Logger.i(SSConstants.TAG_SUPRSEND, "Requesting : $url requestJson:$requestJson")
        if (!NetworkInfo.isConnected()) {
            return ApiResponse(
                status = ResponseStatus.ERROR,
                statusCode = 500,
                errorType = ErrorType.NETWORK_ERROR,
                message = "network error"
            )
        }
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var statusCode = 500
        val response = StringBuilder()

        try {
            val url = URL(url)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = requestMethod
            connection.setRequestProperty("Content-Type", "application/json")
            if (authorization.isNotBlank())
                connection.addRequestProperty("Authorization", authorization)
            if (date.isNotBlank())
                connection.addRequestProperty("Date", date)

            headers?.forEach { (key, value) ->
                connection.addRequestProperty(key, value)
            }

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
                statusCode = connection.responseCode
            } catch (ioe: IOException) {
                if (statusCode >= 400) {
                    inputStream = connection.errorStream
                }
            }
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))

            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                response.append(line)
                response.append('\r')
            }
            bufferedReader.close()
        } catch (e: Exception) {
            Logger.e(SSConstants.TAG_SUPRSEND, "", e)
        } finally {
            connection?.disconnect()
        }
        val responseStr = response.toString()
        Logger.i(SSConstants.TAG_SUPRSEND, "Response Received : $url \nCode : $statusCode")
        Logger.i(SSConstants.TAG_SUPRSEND, "Response : $responseStr")

        return ApiResponse(
            status = if (statusCode >= 400) ResponseStatus.ERROR else ResponseStatus.SUCCESS,
            statusCode = statusCode,
            body = responseStr
        )
    }
}
