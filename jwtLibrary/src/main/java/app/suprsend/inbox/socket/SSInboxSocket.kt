package app.suprsend.inbox.socket

import app.suprsend.SSInternal
import app.suprsend.base.SSConstants
import app.suprsend.base.inboxExecutorService
import app.suprsend.inbox.SSInboxInternal
import app.suprsend.log.Logger
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket

object SSInboxSocket {

    private var socketConnectionState: ConnectionState = ConnectionState.DISCONNECTED

    private var socket: Socket? = null

    fun getSocketConnectionState(): ConnectionState {
        return socketConnectionState
    }

    fun connect() {
        if (socket != null) {
            Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Socket: Already connected : Connect requested ignored")
            return
        }
        Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Socket Connect Requested")
        inboxExecutorService.execute {
            if (socketConnectionState == ConnectionState.FAILED || socketConnectionState == ConnectionState.DISCONNECTED) {
                val authMap = hashMapOf(
                    "authorization" to "${SSInternal.suprSendData.publicApiKey}",
                    "x-ss-signature" to SSInternal.getToken(),
                    "distinct_id" to (SSInternal.suprSendData.distinctId ?: ""),
                    "tenant_id" to (SSInternal.suprSendData.tenantId ?: ""),
                    "schema" to "1"
                )

                val tenantId = SSInboxInternal.inboxData.tenantId
                if (!tenantId.isNullOrBlank()) {
                    authMap["tenant_id"] = tenantId
                }
                val options = IO.Options().apply {
                    transports = arrayOf(WebSocket.NAME)
                    auth = authMap
                    reconnectionAttempts = 25
                    reconnectionDelay = 5000
                    reconnectionDelayMax = 10000
                }
                socket = IO.socket(SSInboxInternal.inboxData.socketUrl ?: "", options)
                subscribeSocketListeners()
                socket?.connect()
            } else {
                Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Socket connect ignored")
            }
        }
    }

    private fun subscribeSocketListeners() {
        socketConnectionState = ConnectionState.CONNECTING
        subscribeNewNotification()
        subscribeNotificationUpdate()
        subscribeBulkNotificationUpdate()
        subscribeResetBadge()
        socket?.on(Socket.EVENT_CONNECT) {
            Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Socket connected")
            socketConnectionState = ConnectionState.CONNECTED
            SSInboxInternal.notifySocketStatus(socketConnectionState)
        }
        socket?.on(Socket.EVENT_DISCONNECT) {
            socketConnectionState = ConnectionState.DISCONNECTED
            Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Socket disconnected")
            SSInboxInternal.notifySocketStatus(socketConnectionState)
        }
        socket?.on(Socket.EVENT_CONNECT_ERROR) {
            socketConnectionState = ConnectionState.FAILED
            Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Socket connect error")
            SSInboxInternal.notifySocketStatus(socketConnectionState)
        }
        socket?.connect()
    }

    private fun subscribeResetBadge() {
        //Called on clicking bell icon
        socket?.on("reset_badge") { data ->
            inboxExecutorService.execute {
                Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "reset_badge : $data")
                SSInboxInternal.onSocketResetBadge(data)
            }
        }
    }

    private fun subscribeBulkNotificationUpdate() {
        socket?.on("bulk_notification_update") { data ->
            inboxExecutorService.execute {
                Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "bulk_notification_update : $data")
                SSInboxInternal.onSocketBulkNotificationUpdate(data)
            }
        }
    }

    private fun subscribeNotificationUpdate() {
        socket?.on("notification_update") { data ->
            inboxExecutorService.execute {
                Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "notification_update : $data")
                SSInboxInternal.onSocketNotificationUpdate(data)
            }
        }
    }

    private fun subscribeNewNotification() {
        socket?.on("new_notification") { data ->
            inboxExecutorService.execute {
                Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "new_notification : $data")
                SSInboxInternal.onSocketNewNotification(data)
            }
        }
    }

    fun disconnect() {
        Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Socket Disconnect Requested")
        inboxExecutorService.execute {
            if (socket?.connected() == true)
                socket?.disconnect()
            socket = null
        }
    }

}