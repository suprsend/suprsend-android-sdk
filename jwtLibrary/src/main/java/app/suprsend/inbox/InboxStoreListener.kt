package app.suprsend.inbox

import app.suprsend.inbox.socket.ConnectionState

interface InboxStoreListener {
    /**
     * The Bell count to be shown
     */
    fun bellCount(bellCount: Int)

    /**
     * Loading state of store
     */
    fun loading(storeId: String, isLoading: Boolean)

    /**
     * If store has any new notifications update will be posted
     */
    fun onUpdate(inboxStore: InboxStore)

    /**
     * If any failure happens
     * We suggest to shown full screen error if inbox messages are not loaded in the requested store_id
     * on the screen else show toast error message
     */
    fun onError(id: String, errorType: InBoxErrorType,message:String, e: Exception?)

    /**
     * The current state of socket connection
     */
    fun socket(connectionState: ConnectionState)

    /**
     * If an new notification has received through socket connection
     */
    fun newNotification(notificationModel: InboxNotification)
}

enum class InBoxErrorType{
    NOTIFICATION,
    STORE
}