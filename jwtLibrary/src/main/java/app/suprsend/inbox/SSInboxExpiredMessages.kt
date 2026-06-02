package app.suprsend.inbox

import app.suprsend.base.PeriodicJob
import app.suprsend.base.SSConstants
import app.suprsend.log.Logger
import app.suprsend.utils.isTrue
//TODO - write tests for expired
object SSInboxExpiredMessages {
    private var periodicJob: PeriodicJob? = null

    fun start() {
        if (periodicJob != null)
            return
        periodicJob = PeriodicJob(
            periodInSec = 20,
            jobName = "InboxExpiryMessages"
        ) {
            checkExpiredMessages()
        }
        periodicJob?.start()
    }

    fun stop() {
        if (periodicJob?.isScheduled.isTrue())
            periodicJob?.stop()
        periodicJob = null
    }

    private fun checkExpiredMessages() {
        Logger.v(SSConstants.TAG_SUPRSEND_INBOX, "Checking expiry messages")
        val now = System.currentTimeMillis()
        val expiredIds = arrayListOf<String>()
        SSInboxInternal.inboxData
            .storesMap.map { it.value }
            .forEach { store ->
                var hasExpired = false
                val validNotifications = store.inboxMessagesList.filter { notification ->
                    val expiry = notification.expiry
                    if (expiry != null && now > expiry) {
                        expiredIds.add(notification.id)
                        hasExpired = true
                        false
                    } else true
                }
                if (hasExpired) {
                    store.inboxMessagesList.clear()
                    store.inboxMessagesList.addAll(validNotifications)
                    SSInboxInternal.notifyListeners(store)
                }
            }
        if (expiredIds.isNotEmpty()) {
            Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Expired message ids : $expiredIds")
            SSInboxInternal.fetchAndNotifyNotificationsCount()
        }
    }
}