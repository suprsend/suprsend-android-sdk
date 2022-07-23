package app.suprsend.base

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Parcel
import androidx.core.content.res.ResourcesCompat
import app.suprsend.config.ConfigHelper
import app.suprsend.event.Algo
import app.suprsend.event.EventFlushHandler
import app.suprsend.event.toMD5
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

internal inline fun <reified T : Enum<T>> String?.mapToEnum(defaultValue: T): T {
    return mapToEnum<T>() ?: defaultValue
}

internal inline fun <reified T : Enum<T>> String?.mapToEnum(): T? {
    this ?: return null
    return enumValues<T>().find { value -> value.name == this }
}

internal fun String?.toKotlinJsonObject(): JSONObject {
    return try {
        if (isNullOrBlank())
            return JSONObject()
        return JSONObject(this ?: "")
    } catch (e: Exception) {
        JSONObject()
    }
}

internal fun JSONObject.filterSSReservedKeys(): JSONObject {
    val filteredJson = JSONObject()
    keys().forEach { key ->
        if (key.isInValidKey()) {
            Logger.e("validation", "Key should not contain $ & ss_ : $key")
        } else {
            filteredJson.put(key, get(key))
        }
    }
    return filteredJson
}

internal fun String.isInValidKey(): Boolean {
    return contains("$") || this.toLowerCase().startsWith("ss_")
}

internal fun JSONObject.addUpdateJsoObject(updateJsonObject: JSONObject?): JSONObject {
    updateJsonObject ?: return this
    val main = this
    updateJsonObject.keys().forEach { key ->
        main.put(key, updateJsonObject.get(key))
    }
    return this
}

internal fun JSONObject?.size(): Int {
    this ?: return 0
    var count = 0
    keys().forEach { _ ->
        count++
    }
    return count
}

internal fun JSONObject.safeJsonArray(key: String): JSONArray? {
    return if (!isNull(key))
        getJSONArray(key)
    else null
}

internal fun JSONObject.safeJSONObject(key: String): JSONObject? {
    return if (!isNull(key))
        getJSONObject(key)
    else null
}

internal fun JSONObject.safeString(key: String): String? {
    return if (!isNull(key))
        getString(key)
    else null
}

internal fun JSONObject.safeBoolean(key: String): Boolean? {
    return if (!isNull(key))
        getBoolean(key)
    else null
}

internal fun JSONObject.safeLong(key: String): Long? {
    return if (!isNull(key))
        getLong(key)
    else null
}

internal fun JSONObject.safeDouble(key: String): Double? {
    return if (!isNull(key))
        getDouble(key)
    else null
}

internal fun getRandomString(length: Int): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}

internal fun safeDrawable(resources: Resources, drawableId: Int, theme: Resources.Theme? = null): Drawable? {
    try {
        return ResourcesCompat.getDrawable(resources, drawableId, theme)
    } catch (e: Exception) {
    }
    return null
}

internal fun Context.getDrawableIdFromName(drawableName: String?): Int? {
    drawableName ?: return null
    return try {
        resources.getIdentifier(drawableName, "drawable", packageName)
    } catch (e: Exception) {
        null
    }
}

internal fun Context.safeIntent(link: String? = null, defaultLauncherIntent: Boolean = true): Intent? {
    return if (!link.isNullOrBlank()) {
        Intent(Intent.ACTION_VIEW, Uri.parse(link))
    } else {
        if (defaultLauncherIntent)
            packageManager.getLaunchIntentForPackage(packageName)
        else null
    }
}

internal fun Parcel.safeString(): String {
    return readString() ?: ""
}

internal fun makeHttpRequest(method: String, urL: String, authorization: String, date: String, body: String? = null): HttPResponse {

    var connection: HttpURLConnection? = null
    var inputStream: InputStream? = null
    try {
        val url = URL(urL)
        connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = method.toUpperCase(Locale.getDefault())
        connection.setRequestProperty("Content-Type", "application/json")
        connection.addRequestProperty("Authorization", authorization)
        connection.addRequestProperty("Date", date)
        connection.useCaches = false
        connection.doOutput = method.toUpperCase(Locale.getDefault()) == "POST"
        connection.doInput = true

        // Send request
        if (body != null) {
            val wr = DataOutputStream(connection.outputStream)
            wr.writeBytes(body)
            wr.close()
        }

        // Get Response
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

internal fun generateSignature(
    method: String,
    route: String,
    contentType: String = "application/json",
    body: String = "",
    date: String
): String {
    val contentMd5 = body.toMD5()
    val secret = ConfigHelper.get(SSConstants.CONFIG_API_SECRET) ?: ""
    val stringToSign = method + "\n" +
        contentMd5 + "\n" +
        contentType + "\n" +
        date + "\n" +
        route
    return Algo.base64(Algo.generateHashWithHmac256(secret, stringToSign))
}
