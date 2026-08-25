package app.suprsend.base

import android.util.Log
import app.suprsend.RefreshUserTokenCallback
import org.json.JSONObject

class RefreshUserTokenCallbackImpl : RefreshUserTokenCallback {
    private val networkClient = NetworkClient()

    override fun getToken(distinctId: String): String {
        return try {
            val response = networkClient.httpCall(
                requestMethod = "GET",
                url = "${TestConstants.SS_BASE_URL}/authentication-token/$distinctId"
            )
            val responseJo = JSONObject(response.body ?: "{}")
            val token = responseJo.optString("token")
            Log.i("suprsend", "Token Received $token")
            token
        } catch (e: Exception) {
            ""
        }
    }
}
