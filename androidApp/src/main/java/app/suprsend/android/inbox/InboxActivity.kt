package app.suprsend.android.inbox

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import app.suprsend.SSApi
import app.suprsend.android.databinding.ActivityInboxBinding
import app.suprsend.inbox.Inbox
import app.suprsend.inbox.model.NotificationStore
import app.suprsend.inbox.model.NotificationStoreQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InboxActivity : AppCompatActivity() {

    lateinit var binding: ActivityInboxBinding

    lateinit var inbox: Inbox

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Inbox"
        binding = ActivityInboxBinding.inflate(layoutInflater)
        setContentView(binding.root)
        inbox = Inbox(
            workspaceKey = intent.getStringExtra("workspaceKey") ?: "",
            subscriberId = intent.getStringExtra("subscriberId") ?: "",
            distinctId = SSApi.getInstance().getDistinctId()
        )
        coroutineScope.launch(Dispatchers.IO) {
//            val notificationListModel = inbox.getNotificationsList(
//                1,
//                10,
//                System.currentTimeMillis()
//            )
            val notificationListModel = inbox.getNotificationsList(
                1,
                10,
                System.currentTimeMillis(),
                NotificationStore(
                    "Tab1",
                    "Tab 1&2",
                    NotificationStoreQuery(
                        tags = listOf("tab1"),
                        categories = listOf("transactional"),
                        read = false
                    )
                )
            )

            val responseText = StringBuilder()
            if (notificationListModel != null) {
//                responseText.append("Meta:\n")
//                responseText.append("currentPage: ${notificationListModel.meta.currentPage}\n")
//                responseText.append("totalPages: ${notificationListModel.meta.totalPages}\n")
//                responseText.append("Total: ${notificationListModel.total}\n")
//                responseText.append("Unseen: ${notificationListModel.unseen}\n")

//                responseText.append("Results:\n")
                for (notificationModel in notificationListModel.results) {
//                    responseText.append("Tenant ID: ${notificationModel.tenantId}\n")
//                    responseText.append("Is Expiry Visible: ${notificationModel.isExpiryVisible}\n")
//                    responseText.append("Importance: ${notificationModel.importance}\n")
//                    responseText.append("Message:\n")
//                    responseText.append("\tSchema: ${notificationModel.message.schema}\n")
                    responseText.append("\tHeader: ${notificationModel.message.header}\n")
                    responseText.append("\tText: ${notificationModel.message.text}\n")
//                    responseText.append("Is Pinned: ${notificationModel.isPinned}\n")
//                    responseText.append("Archived: ${notificationModel.archived}\n")
//                    responseText.append("Created On: ${notificationModel.createdOn}\n")
//                    responseText.append("Category: ${notificationModel.category}\n")
//                    responseText.append("Can User Unpin: ${notificationModel.canUserUnpin}\n")
//                    responseText.append("ID: ${notificationModel.id}\n")
                    responseText.append("\n")
                }
                responseText.append("\n")
            } else {
                responseText.append("None")
            }
            withContext(Dispatchers.Main) {
                binding.textTv.text = responseText
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
