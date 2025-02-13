package app.suprsend.inbox

import app.suprsend.base.SSConstants

internal data class InboxData(
    var baseUrl: String = SSConstants.DEFAULT_INBOX_BASE_API_URL,
    var socketUrl: String = SSConstants.DEFAULT_INBOX_SOCKET_API_URL,

    var subscriberId: String? = null,
    var tenantId: String? = null,


    var bellCount: Int = 0,

    var storesMap: Map<String, InboxStore> = mapOf(InboxStore.DEFAULT_STORE to InboxStore(storeId = InboxStore.DEFAULT_STORE))
) {
    fun getCountHash(): String {
        return "$bellCount${storesMap.map { it.value }.map { it.unseenCount }}"
    }
}