package app.suprsend.inbox

import android.content.Context
import app.suprsend.SuprSend
import app.suprsend.base.RefreshUserTokenCallbackImpl
import app.suprsend.base.TestConstants
import app.suprsend.base.assertIsSuccess

/**
 * [SuprSend.reset] clears inbox config (subscriber id, base URL, stores).
 * Tests must re-apply inbox settings after identify, then call [SuprsendInbox.getInstance].
 */
internal object InboxTestHelper {

    fun identifiedInbox(
        context: Context,
        distinctId: String,
        configure: (() -> Unit)? = null
    ): SuprsendInbox {
        val refreshUserToken = RefreshUserTokenCallbackImpl()
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            host = TestConstants.SS_BASE_URL
        )
        SuprSend.setRefreshUserToken(refreshUserToken)
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        suprsend.identify(distinctId).assertIsSuccess()
        SuprsendInbox.setBaseUrl(baseUrl = TestConstants.SS_INBOX_BASE_URL)
        SuprsendInbox.setSubscriberId(TestConstants.SUBSCRIBER_ID)
        configure?.invoke()
        return SuprsendInbox.getInstance()
    }
}
