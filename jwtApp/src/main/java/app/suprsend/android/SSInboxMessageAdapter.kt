package app.suprsend.android

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import app.suprsend.android.databinding.InboxMessageItemBinding
import app.suprsend.inbox.InboxNotification
import app.suprsend.inbox.SuprsendInbox
import app.suprsend.inbox.util.getReadableTime
import app.suprsend.inbox.util.todayMidNightMilli

internal class SSInboxMessageAdapter
constructor(
    val inflater: LayoutInflater,
    private var message: List<InboxNotification>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> {
                val binding = InboxMessageItemBinding.inflate(inflater, viewGroup, false)
                BaseViewHolder(binding)
            }

            else -> throw IllegalStateException("View type is unknown $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as BaseViewHolder
        val binding = holder.binding as InboxMessageItemBinding
        holder.binding.root.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        val context = binding.titleTv.context
        val notification = message[position]
        binding.obj = notification

        binding.titleTv.setMarkDownText(notification.message.header)
        binding.messageTv.setMarkDownText(notification.message.text)
        binding.timeTv.text = context.getString(R.string.createdOn) + getReadableTime(notification.createdOn)
        val expiry = notification.expiry
        if (expiry != null)
            binding.expiryTimeTv.text = context.getString(R.string.expiry) + " " + getReadableTime(expiry, todayMidNightMilli())

        binding.markUnreadTv.setOnClickListener {
            SuprsendInbox.getInstance().markAsUnreadAsync(
                notificationId = notification.id
            )
        }
        binding.markReadTv.setOnClickListener {
            SuprsendInbox.getInstance().markAsReadAsync(
                notificationId = notification.id
            )
        }
        binding.archiveTv.setOnClickListener {
            SuprsendInbox.getInstance().markAsArchivedAsync(
                notificationId = notification.id
            )
        }
        binding.interactedTv.setOnClickListener {
            SuprsendInbox.getInstance().markAsInteractedAsync(
                notificationId = notification.id
            )
        }
    }

    override fun getItemCount(): Int {
        return message.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun newList(
        message: List<InboxNotification>
    ) {
        this.message = message
        notifyDataSetChanged()
    }
}
