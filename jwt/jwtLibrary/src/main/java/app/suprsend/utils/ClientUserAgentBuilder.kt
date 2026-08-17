package app.suprsend.utils

import app.suprsend.AppInfo
import app.suprsend.ClientInfo
import org.json.JSONObject

/**
 * Android equivalent of the web SDK's `buildClientUserAgent` / `buildUserAgent` pair.
 * - [toJson] is the JSON payload shipped on the `X-Suprsend-Client-User-Agent` header.
 * - [toUserAgentString] is the compact string shipped on the `X-Suprsend-User-Agent` header.
 *
 * SDK defaults are baked into [ClientInfo] itself; only runtime-derived fields
 * (sdk/os version, device model) are filled by the caller before invoking these helpers.
 */
internal object ClientUserAgentBuilder {

    fun toJson(info: ClientInfo): JSONObject {
        val json = JSONObject()
        putIfNotBlank(json, "sdk", info.sdk)
        putIfNotBlank(json, "sdk_version", info.sdkVersion)
        putIfNotBlank(json, "lang", info.lang)
        putIfNotBlank(json, "lang_version", info.langVersion)
        putIfNotBlank(json, "platform", info.platform)
        putIfNotBlank(json, "environment", info.environment)
        putIfNotBlank(json, "os", info.os)
        putIfNotBlank(json, "os_version", info.osVersion)
        putIfNotBlank(json, "device_model", info.deviceModel)
        info.appInfo?.toJson()?.let { appJson ->
            if (appJson.length() > 0) json.put("app_info", appJson)
        }
        return json
    }

    fun toUserAgentString(info: ClientInfo): String {
        val sdk = info.sdk
        val sdkVersion = info.sdkVersion
        //ex - suprsend-android-sdk/1.0.0 (kotlin/disabled; android)  (test/1.0.0)
        return "$sdk/$sdkVersion (${info.lang}/${info.langVersion}; ${info.platform}) ${formatAppInfo(info.appInfo)}"
    }

    private fun AppInfo.toJson(): JSONObject {
        val json = JSONObject()
        putIfNotBlank(json, "name", name)
        putIfNotBlank(json, "version", version)
        return json
    }

    private fun putIfNotBlank(json: JSONObject, key: String, value: String?) {
        if (!value.isNullOrBlank()) json.put(key, value)
    }

    private fun formatAppInfo(appInfo: AppInfo?): String {
        appInfo?:return ""
        return "(${appInfo.name}/${appInfo.version})"
    }
}
