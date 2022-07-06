package app.suprsend.inbox

import android.util.Log
import app.suprsend.BuildConfig
import app.suprsend.base.Logger
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

private typealias UpdateInboxUi = (isConnected: Boolean) -> Unit

internal object InboxHelper {


    fun fetchApiCall(distinctId: String, subscriberId: String, messagesSeen: Boolean = false, updateUI: UpdateInboxUi? = null) {
        if (!SdkAndroidCreator.networkInfo.isConnected()) {
            updateUI?.invoke(false)
            return
        }
        appExecutorService.execute {
            if (messagesSeen)
                bellClicked(distinctId = distinctId, subscriberId = subscriberId)
            var fetchNext = false
            val dayTime: Long = 1000 * 60 * 60 * 24
            //Start from 30 days earlier
            var after = System.currentTimeMillis() - (30 * dayTime)
            try {
                do {
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
                    fetchNext = if (httpResponse.statusCode == 200) {
                        after = System.currentTimeMillis()
                        val responseJO = httpResponse.response?.let { JSONObject(it) } ?: JSONObject()
                        val latestJA = responseJO.optJSONArray("results")
                        val unReadCount = responseJO.optInt("unread")
                        Logger.i(SSInboxActivity.TAG, "Latest items received : ${latestJA?.length()} unReadCount : $unReadCount")
                        if (latestJA != null && latestJA.length() > 0) {
                            storeResponse(latestJA)
                            updateUI?.invoke(true)
                            ConfigHelper.addOrUpdate(SSConstants.INBOX_MESSAGE_UNREAD_COUNT, unReadCount)
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                    if (BuildConfig.DEBUG) {
                        Log.i(
                            SSInboxActivity.TAG, "Fetch Inbox Messages :\n$route" +
                                "\ndistinctId:$distinctId" +
                                "\nsubscriberId:$subscriberId" +
                                "\n${httpResponse.response}"
                        )
                    }
                } while (fetchNext)
            } catch (e: Exception) {
                Log.e(SSInboxActivity.TAG, "fetchApiCall", e)
            }

        }
    }


    fun getUnReadMessagesCount(): Int {
        return ConfigHelper.get(SSConstants.INBOX_MESSAGE_UNREAD_COUNT)?.toInt() ?: 0
    }

    private fun bellClicked(distinctId: String, subscriberId: String) {
        val baseUrl = ConfigHelper.get(SSConstants.CONFIG_API_BASE_URL) ?: SSConstants.DEFAULT_BASE_API_URL
        val route = "/inbox/bell-clicked/"
        val envKey = ConfigHelper.get(SSConstants.CONFIG_API_KEY) ?: ""
        val date = Date()
        val body = JSONObject().apply {
            put("time", date.time)
            put("distinct_id", distinctId)
            put("subscriber_id", subscriberId)
        }.toString()
        val signature = generateSignature(body = body, method = "POST", route = route, date = date.toString())

        val httpResponse = makeHttpRequest(
            method = "POST",
            urL = "$baseUrl$route",
            authorization = "$envKey:$signature",
            body = body,
            date = date.toString()
        )

        if (BuildConfig.DEBUG) {
            Log.i(
                SSInboxActivity.TAG, "Bell Clicked Response :\n$route" +
                    "\ndistinctId:$distinctId" +
                    "\nsubscriberId:$subscriberId" +
                    "\n${httpResponse.response}"
            )
        }
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