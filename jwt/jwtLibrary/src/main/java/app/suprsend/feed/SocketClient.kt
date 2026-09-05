package app.suprsend.feed

import app.suprsend.base.SSConstants
import app.suprsend.base.inboxExecutorService
import app.suprsend.log.Logger
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import org.json.JSONObject

/**
 * Realtime connection of a single [Feed] instance. Auth is sent in the socket.io CONNECT
 * payload using the same JWT credentials as the HTTP calls.
 */
internal class SocketClient(
    private val serverURL: String,
    headers: Map<String, String>
) {

    internal enum class EventType(val rawValue: String) {
        NEW_NOTIFICATION("new_notification"),
        NOTIFICATION_UPDATE("notification_update"),
        BULK_NOTIFICATION_UPDATE("bulk_notification_update"),
        RESET_BADGE("reset_badge")
    }

    var receivedMessage: ((EventType, JSONObject?) -> Unit)? = null

    var connectionLost: (() -> Unit)? = null

    private val auth = HashMap<String, String>(headers)

    @Volatile
    private var socket: Socket? = null

    fun connect() {
        if (socket != null) {
            Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Socket : Already connected, connect request ignored")
            return
        }
        inboxExecutorService.execute {
            try {
                val options = IO.Options().apply {
                    transports = arrayOf(WebSocket.NAME)
                    auth = this@SocketClient.auth
                    reconnectionDelay = 1000
                    reconnectionDelayMax = 10000
                    // socket.io caches a manager (and its default namespace socket) per URL, so
                    // without this every Feed instance on the same host shares one connection.
                    forceNew = true
                }
                val newSocket = IO.socket(serverURL, options)
                socket = newSocket
                subscribeListeners(newSocket)
                newSocket.connect()
            } catch (e: Exception) {
                Logger.e(SSConstants.TAG_SUPRSEND_INBOX, "Socket : Failed to connect $serverURL", e)
            }
        }
    }

    fun disconnect() {
        val currentSocket = socket ?: return
        socket = null
        inboxExecutorService.execute {
            try {
                currentSocket.off()
                currentSocket.disconnect()
            } catch (e: Exception) {
                Logger.e(SSConstants.TAG_SUPRSEND_INBOX, "Socket : Failed to disconnect", e)
            }
        }
    }

    /**
     * Replaces the credentials used by the next reconnect attempt. socket.io reads the auth map
     * on every CONNECT, so mutating it in place is enough.
     */
    fun updateHeaders(headers: Map<String, String>) {
        synchronized(auth) {
            auth.clear()
            auth.putAll(headers)
        }
    }

    private fun subscribeListeners(socket: Socket) {
        EventType.values().forEach { eventType ->
            socket.on(eventType.rawValue) { args ->
                Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Socket : ${eventType.rawValue}")
                receivedMessage?.invoke(eventType, args?.firstOrNull() as? JSONObject)
            }
        }
        socket.on(Socket.EVENT_CONNECT) {
            Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Socket : connected")
        }
        socket.on(Socket.EVENT_DISCONNECT) {
            Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Socket : disconnected")
            connectionLost?.invoke()
        }
        socket.on(Socket.EVENT_CONNECT_ERROR) {
            Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Socket : connect error")
            connectionLost?.invoke()
        }
    }
}
