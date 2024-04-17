package app.suprsend.inbox.model

data class NotificationListModel(
    val meta: NotificationListMeta,
    val total: Int,
    val unseen: Int,
    val results: List<NotificationModel>
)

data class NotificationListMeta(
    val currentPage: Int,
    val totalPages: Int
)

data class NotificationMessage(
    val schema: String,
    val header: String,
    val text: String
)

data class NotificationModel(
    val tenantId: String,
    val isExpiryVisible: Boolean,
    val importance: String,
    val message: NotificationMessage,
    val isPinned: Boolean,
    val archived: Boolean,
    val createdOn: Long,
    val category: String,
    val canUserUnpin: Boolean,
    val id: String
)

data class NotificationStore(
    val storeId: String,
    val label: String,
    val query: NotificationStoreQuery? = null
)

data class NotificationStoreQuery(
    val tags: List<String>? = null,
    val categories: List<String>? = null,
    val read: Boolean? = null
)