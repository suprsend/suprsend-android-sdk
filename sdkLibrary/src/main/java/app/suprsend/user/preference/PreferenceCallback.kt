package app.suprsend.user.preference

import app.suprsend.base.Response
import org.json.JSONObject

interface PreferenceCallback {
    fun onUpdate(preferenceData: PreferenceData)
    fun onError(response: Response<JSONObject>)
}