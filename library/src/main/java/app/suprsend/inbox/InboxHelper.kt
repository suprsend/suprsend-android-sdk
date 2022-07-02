package app.suprsend.inbox

import android.util.Log
import app.suprsend.BuildConfig
import app.suprsend.base.SSConstants
import app.suprsend.base.SdkAndroidCreator
import app.suprsend.base.appExecutorService
import app.suprsend.base.generateSignature
import app.suprsend.base.makeHttpRequest
import app.suprsend.base.safeJSONObject
import app.suprsend.base.safeString
import app.suprsend.config.ConfigHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

internal object InboxHelper {

    fun fetchApiCall(distinctId: String, subscriberId: String, updateUI: (isConnected: Boolean) -> Unit) {
        if (!SdkAndroidCreator.networkInfo.isConnected()) {
            updateUI(false)
            return
        }
        appExecutorService.execute {
            try {
                var after = ConfigHelper.get(SSConstants.INBOX_FETCH_TIME)?.toLong()
                if (after == null && after != -1L) {
                    val dayTime: Long = 1000 * 60 * 60 * 24
                    after = System.currentTimeMillis() - (30 * dayTime)
                }
                val baseUrl = ConfigHelper.get(SSConstants.CONFIG_API_BASE_URL) ?: SSConstants.DEFAULT_BASE_API_URL
                val route = "/inbox/fetch/?after=$after&distinct_id=$distinctId&subscriber_id=$subscriberId"
                val envKey = ConfigHelper.get(SSConstants.CONFIG_API_KEY) ?: ""
                val date = Date().toString()
                val signature = generateSignature(method = "GET", route = route, date = date)

                val httpResponse = makeHttpRequest(
                    method = "GET",
                    urL = "$baseUrl$route",
                    authorization = "$envKey:$signature",
                    date = date
                )
                if (httpResponse.statusCode == 200) {
                    ConfigHelper.addOrUpdate(SSConstants.INBOX_FETCH_TIME, System.currentTimeMillis().toString())
                    val latestJA = httpResponse.response?.let { JSONObject(it).optJSONArray("results") } ?: JSONArray()
                    Log.i(SSInboxActivity.TAG, "Latest items received : ${latestJA.length()}")
                    if (latestJA.length() > 0) {
                        storeResponse(latestJA)
                        updateUI(true)
                    }

                }
                if (BuildConfig.DEBUG) {
                    Log.i(
                        SSInboxActivity.TAG, "Response : $route" +
                            "\ndistinctId:$distinctId" +
                            "\nsubscriberId:$subscriberId" +
                            "\n${httpResponse.response}"
                    )
                }

            } catch (e: Exception) {
                Log.e(SSInboxActivity.TAG, "fetchApiCall", e)
            }
        }
    }

    fun getUnReadMessagesCount(): Int {
        val response = ConfigHelper.get(SSConstants.INBOX_RESPONSE) ?: "[]"
        return parseInboxItems(JSONArray(response)).count { item -> item.seenOn == null }
    }

    private fun storeResponse(latestJA: JSONArray) {
        val prevResponse = ConfigHelper.get(SSConstants.INBOX_RESPONSE) ?: "[]"
        val prevJA = JSONArray(prevResponse)
        val ids = arrayListOf<String>()
        for (i in 0 until prevJA.length()) {
            val id = prevJA.getJSONObject(i).safeString("n_id") ?: ""
            ids.add(id)
        }
        for (i in 0 until latestJA.length()) {
            val id = latestJA.getJSONObject(i).safeString("n_id") ?: ""
            if (ids.contains(id))
                continue
            prevJA.put(latestJA.getJSONObject(i))
        }
        ConfigHelper.addOrUpdate(SSConstants.INBOX_RESPONSE, prevJA.toString())
        Log.i(SSInboxActivity.TAG, "Merged items Total : ${prevJA.length()}")
    }

    fun parseInboxItems(jsonArray: JSONArray?): List<SSInboxItemVo> {
        jsonArray ?: return listOf()
        val inboxItems = arrayListOf<SSInboxItemVo>()
        for (i in 0 until jsonArray.length()) {
            val jo = jsonArray.getJSONObject(i)
            val messageJO = jo.safeJSONObject("message")
            val id = jo.safeString("n_id") ?: ""
            inboxItems.add(
                SSInboxItemVo(
                    nID = id,
                    createdOn = jo.safeString("created_on")?.toLong(),
                    seenOn = jo.safeString("seen_on")?.toLong(),

                    //Message
                    header = messageJO?.safeString("header"),
                    text = messageJO?.safeString("text"),
                    url = messageJO?.safeString("url"),
                    imageUrl = messageJO?.safeString("image")
                )
            )
        }
        return inboxItems.sortedByDescending { item -> item.createdOn ?: 0 }
    }
}