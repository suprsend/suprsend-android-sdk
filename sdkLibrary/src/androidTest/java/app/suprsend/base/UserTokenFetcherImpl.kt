package app.suprsend.base

import android.util.Log
import app.suprsend.UserTokenFetcher
import org.json.JSONObject

class UserTokenFetcherImpl : UserTokenFetcher {
    private val networkClient = NetworkClient()

    override fun getToken(distinctId: String): String {
        return try {
            val baseUrl = "https://collector-staging.suprsend.workers.dev"
            val response = networkClient.httpCall(
                requestMethod = "GET",
                url = "$baseUrl/authentication-token/$distinctId"
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