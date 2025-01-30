package app.suprsend.user.preference

import app.suprsend.base.Response
import org.json.JSONObject

interface PreferenceCallback {
    fun onUpdate()
    fun onError(response :Response<JSONObject>)
}