package app.suprsend.base

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

internal inline fun <reified T : Enum<T>> String?.mapToEnum(defaultValue: T): T {
    return mapToEnum<T>() ?: defaultValue
}

internal inline fun <reified T : Enum<T>> String?.mapToEnum(ignoreCase: Boolean = false): T? {
    this ?: return null
    return enumValues<T>().find { value -> value.name.equals(this,ignoreCase) }
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
