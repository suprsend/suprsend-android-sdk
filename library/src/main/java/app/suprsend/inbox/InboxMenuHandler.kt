package app.suprsend.inbox

import android.view.MenuItem
import android.view.View
import app.suprsend.R
import app.suprsend.base.Logger

class InboxMenuHandler(
    item: MenuItem,
    distinctId: String,
    subscriberId: String,
    ssInboxConfig: SSInboxConfig? = null,
    onClickListener: View.OnClickListener? = null
) {

    private var inboxBellView: InboxBellView? = null

    init {
        try {
            inboxBellView = item.actionView?.findViewById(R.id.inboxBellView)
            inboxBellView
                ?.initialize(
                    distinctId = distinctId,
                    subscriberId = subscriberId,
                    ssInboxConfig = ssInboxConfig
                )
            inboxBellView?.setOnClickListener(onClickListener)
        } catch (e: Exception) {
            Logger.e(SSInboxActivity.TAG, "", e)
        }
    }

    fun onStart() {
        try {
            inboxBellView?.onStart()
        } catch (e: Exception) {
            Logger.e(SSInboxActivity.TAG, "", e)
        }
    }

    fun onStop() {
        try {
            inboxBellView?.onStop()
        } catch (e: Exception) {
            Logger.e(SSInboxActivity.TAG, "", e)
        }
    }
}
