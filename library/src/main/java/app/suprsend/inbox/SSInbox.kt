package app.suprsend.inbox

import app.suprsend.BuildConfig
import app.suprsend.SSApiInternal
import app.suprsend.base.*
import app.suprsend.base.Logger
import app.suprsend.base.SSConstants
import app.suprsend.base.SdkAndroidCreator
import app.suprsend.base.appExecutorService
import app.suprsend.base.safeJSONObject
import app.suprsend.base.safeString
import app.suprsend.config.ConfigHelper
import java.util.Date
import org.json.JSONArray
import org.json.JSONObject

typealias UpdateInboxUi = (isConnected: Boolean, showNewUpdatesAvailable: Boolean) -> Unit

object SSInbox {

    var isFetching = false

    fun fetchFromRemote(distinctId: String, subscriberId: String, messagesSeen: Boolean = false, updateUI: UpdateInboxUi? = null) {
        if (!SdkAndroidCreator.networkInfo.isConnected()) {
            updateUI?.invoke(false, false)
            return
        }
        if (isFetching) {
            Logger.i(SSInboxActivity.TAG, "Fetch inbox messages already in progress")
            return
        }
        appExecutorService.execute {
            isFetching = true
            if (messagesSeen)
                bellClicked(distinctId = distinctId, subscriberId = subscriberId)
            var fetchNext = false
            val dayTime: Long = 1000 * 60 * 60 * 24
            // Start from 30 days earlier
            var after = System.currentTimeMillis() - (30 * dayTime)
            try {
                do {
                    val baseUrl = SSApiInternal.getBaseUrl()
                    val route = "/inbox/fetch/?after=$after&distinct_id=$distinctId&subscriber_id=$subscriberId"
                    val envKey = ConfigHelper.get(SSConstants.CONFIG_API_KEY) ?: ""
                    val date = Date().toString()
                    val signature = createAuthorization(requestMethod = "GET", requestURI = route, date = date)

                    val httpResponse = httpCall(
                        requestMethod = "GET",
                        urL = "$baseUrl$route",
                        authorization = "$envKey:$signature",
                        date = date
                    )
                    fetchNext = if (httpResponse.statusCode == 200) {
                        after = System.currentTimeMillis()
                        val responseJO = httpResponse.response?.let { JSONObject(it) } ?: JSONObject()
                        val latestJA = responseJO.optJSONArray("results")
                        val unReadCount = responseJO.optInt("unread")
                        val prevUnReadCount = ConfigHelper.get(SSConstants.INBOX_MESSAGE_UNREAD_COUNT)?.toInt() ?: 0
                        val showNewUpdatesAvailable = (unReadCount - prevUnReadCount > 0)
                        Logger.i(SSInboxActivity.TAG, "Latest items received : ${latestJA?.length()} unReadCount : $unReadCount showNewUpdatesAvailable : $showNewUpdatesAvailable")
                        if (latestJA != null && latestJA.length() > 0) {
                            storeResponse(latestJA)
                            ConfigHelper.addOrUpdate(SSConstants.INBOX_MESSAGE_UNREAD_COUNT, unReadCount)
                            updateUI?.invoke(true, showNewUpdatesAvailable)
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                    if (BuildConfig.DEBUG) {
                        Logger.i(
                            SSInboxActivity.TAG, "Fetch Inbox Messages :\n$route" +
                                "\ndistinctId:$distinctId" +
                                "\nsubscriberId:$subscriberId" +
                                "\n${httpResponse.response}"
                        )
                    }
                } while (fetchNext)
            } catch (e: Exception) {
                Logger.e(SSInboxActivity.TAG, "fetchApiCall", e)
            }
            isFetching = false
        }
    }

    fun getUnReadMessagesCount(): Int?  =  ConfigHelper.get(SSConstants.INBOX_MESSAGE_UNREAD_COUNT)?.toInt()

    private fun bellClicked(distinctId: String, subscriberId: String) {
        val baseUrl =  SSApiInternal.getBaseUrl()
        val route = "/inbox/bell-clicked/"
        val envKey = ConfigHelper.get(SSConstants.CONFIG_API_KEY) ?: ""
        val date = Date()
        val body = JSONObject().apply {
            put("time", date.time)
            put("distinct_id", distinctId)
            put("subscriber_id", subscriberId)
        }.toString()
        val signature = createAuthorization(requestJson = body, requestMethod = "POST", requestURI = route, date = date.toString())

        val httpResponse = httpCall(
            requestMethod = "POST",
            urL = "$baseUrl$route",
            authorization = "$envKey:$signature",
            requestJson = body,
            date = date.toString()
        )

        if (BuildConfig.DEBUG) {
            Logger.i(
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
        Logger.i(SSInboxActivity.TAG, "Merged items Total : ${prevJA.length()}")
    }

    fun getInboxItems(): List<SSInboxItemVo> {
        val response = ConfigHelper.get(SSConstants.INBOX_RESPONSE) ?: "[]"
        val jsonArray = JSONArray(response)
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

                    // Message
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
