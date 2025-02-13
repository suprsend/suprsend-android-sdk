package app.suprsend.inbox

import app.suprsend.base.Response
import org.json.JSONArray
import org.json.JSONObject

data class InboxStore(
    val storeId: String = DEFAULT_STORE,
    val label: String? = null,
    val query: InboxQuery? = null
) {

    //Data
    val inboxMessagesList: ArrayList<InboxNotification> = arrayListOf()
    var isLoading: Boolean = false
    var unseenCount: Int = 0
    var inStoreMeta = InStoreMeta(
        totalCount = -1,
        currentPageNo = 0,
        totalPages = -1
    )
    var initialFetchTime: Long = -1

    constructor(jsonObject: JSONObject) : this(
        storeId = jsonObject.getString("storeId"),
        label = jsonObject.getString("label"),
        query = jsonObject.optJSONObject("query")?.let { InboxQuery(it) },
    )

    fun toJSONObject(): JSONObject? {
        if (storeId == DEFAULT_STORE || storeId.isBlank()) {
            return null
        }
        val safeQuery = query ?: InboxQuery()
        val jo = JSONObject().apply {
            put("store_id", storeId)
            put("query", safeQuery.toJSONObject())
        }
        return jo
    }

    fun load(): Response<InboxStore> {
        return SSInboxInternal.fetchNotificationsAndNotify(inboxStore = this)
    }

    fun hasNextPage(): Boolean = inStoreMeta.totalPages == -1 || inStoreMeta.currentPageNo <= inStoreMeta.totalPages

    fun getCurrentPageNo(): Int = inStoreMeta.currentPageNo

    fun getTotalPages(): Int = inStoreMeta.totalPages

    internal fun setUnseenCount(unseenCount: Int) {
        this.unseenCount = unseenCount
    }

    fun reset() {
        inboxMessagesList.clear()
        isLoading = false
        inStoreMeta = InStoreMeta(
            totalCount = -1,
            currentPageNo = 0,
            totalPages = -1
        )
        setUnseenCount(0)
        initialFetchTime = -1
    }

    companion object {
        const val DEFAULT_STORE = "default"
        fun from(jsonArray: JSONArray?): List<InboxStore> {
            if (jsonArray == null) {
                return listOf()
            }
            val inboxStoreList = mutableListOf<InboxStore>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(i)
                if (item != null) {
                    inboxStoreList.add(InboxStore(item))
                }
            }
            return inboxStoreList
        }
    }
}


