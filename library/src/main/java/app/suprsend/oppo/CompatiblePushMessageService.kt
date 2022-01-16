package app.suprsend.oppo

import android.content.Context
import app.suprsend.base.Logger
import app.suprsend.notification.SSNotificationHelper
import com.heytap.msp.push.mode.DataMessage
import com.heytap.msp.push.service.CompatibleDataMessageCallbackService

class CompatiblePushMessageService : CompatibleDataMessageCallbackService() {
    override fun processMessage(context: Context?, dataMessage: DataMessage?) {
        super.processMessage(context, dataMessage)
        try {
            Logger.i(DataMessagePushMessageService.TAG, "CompatiblePushMessageService : processMessage : $dataMessage")
            context ?: return
            dataMessage ?: return
            SSNotificationHelper.showOppoNotification(context, dataMessage)
        } catch (e: Exception) {
            Logger.e(DataMessagePushMessageService.TAG, "processMessage exception ", e)
        }
    }
}