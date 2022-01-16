package app.suprsend.oppo

import android.content.Context
import app.suprsend.base.Logger
import app.suprsend.notification.SSNotificationHelper
import com.heytap.msp.push.mode.DataMessage
import com.heytap.msp.push.service.DataMessageCallbackService

class DataMessagePushMessageService : DataMessageCallbackService() {
    override fun processMessage(context: Context?, dataMessage: DataMessage?) {
        super.processMessage(context, dataMessage)
        try {
            Logger.i(TAG, "DataMessagePushMessageService : processMessage : $dataMessage")
            context ?: return
            dataMessage ?: return
            SSNotificationHelper.showOppoNotification(context, dataMessage)
        } catch (e: Exception) {
            Logger.e(TAG, "processMessage exception ", e)
        }
    }

    companion object {
        const val TAG = "push_oppo"
    }
}