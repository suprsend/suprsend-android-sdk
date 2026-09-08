package app.suprsend.android

import android.annotation.SuppressLint
import android.graphics.Rect
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.suprsend.android.databinding.InboxMessageItemBinding
import app.suprsend.feed.APIResponseStatus
import app.suprsend.feed.IRemoteNotification

internal class SSInboxMessageAdapter
constructor(
    val inflater: LayoutInflater,
    private val viewModel: InboxViewModel,
    private var message: List<IRemoteNotification> = listOf(),
    private var hasMore: Boolean = false,
    private var apiStatus: APIResponseStatus = APIResponseStatus.INITIAL
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var onLoadMoreClick: (() -> Unit)? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_ITEM -> {
                val binding = InboxMessageItemBinding.inflate(inflater, viewGroup, false)
                BaseViewHolder(binding)
            }
            TYPE_FOOTER -> {
                val view = inflater.inflate(R.layout.inbox_list_footer, viewGroup, false)
                FooterViewHolder(view)
            }
            else -> throw IllegalStateException("View type is unknown $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < message.size) TYPE_ITEM else TYPE_FOOTER
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FooterViewHolder) {
            bindFooter(holder)
            return
        }
        holder as BaseViewHolder
        val binding = holder.binding as InboxMessageItemBinding
        val notification = message[position]
        val isUnread = notification.read_on == null
        val header = notification.message.header.orEmpty()
        val hasHeader = header.isNotEmpty()

        binding.unreadDot.setVisible(isUnread)
        binding.cardRoot.setBackgroundResource(
            if (isUnread) R.drawable.inbox_card_unread_bg else R.drawable.inbox_card_read_bg
        )

        val title = if (hasHeader) header else notification.message.text
        binding.titleTv.text = title
        binding.timeTv.text = formatRelative(notification.created_on)

        if (hasHeader) {
            binding.messageTv.setVisible(true)
            binding.messageTv.text = notification.message.text
        } else {
            binding.messageTv.setVisible(false)
        }

        val subtext = notification.message.subtext?.text.orEmpty()
        if (subtext.isNotEmpty()) {
            binding.subtextTv.setVisible(true)
            binding.subtextTv.text = subtext
        } else {
            binding.subtextTv.setVisible(false)
        }

        binding.cardRoot.setOnClickListener {
            Log.i(AppConstants.TAG,"Inbox item card clicked ${notification.n_id}")
            viewModel.onItemTap(notification)
        }
        binding.menuBtn.setOnClickListener { view ->
            showActionsMenu(view, notification, isUnread)
        }
    }

    private fun bindFooter(holder: FooterViewHolder) {
        val fetchingMore = apiStatus == APIResponseStatus.FETCHING_MORE
        val showLoadMore = hasMore && !fetchingMore
        val showCaughtUp = !hasMore && message.isNotEmpty() && !fetchingMore

        holder.progress.setVisible(fetchingMore)
        holder.loadMore.setVisible(showLoadMore)
        holder.caughtUp.setVisible(showCaughtUp)
        holder.loadMore.setOnClickListener { onLoadMoreClick?.invoke() }
    }

    private fun showActionsMenu(anchor: View, notification: IRemoteNotification, isUnread: Boolean) {
        val popup = PopupMenu(anchor.context, anchor)
        if (isUnread) {
            popup.menu.add(0, MENU_MARK_READ, 0, R.string.mark_as_read)
        } else {
            popup.menu.add(0, MENU_MARK_UNREAD, 0, R.string.mark_as_unread)
        }
        popup.menu.add(0, MENU_ARCHIVE, 0, R.string.archive)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_MARK_READ -> viewModel.markAsRead(notification)
                MENU_MARK_UNREAD -> viewModel.markAsUnread(notification)
                MENU_ARCHIVE -> viewModel.archive(notification)
            }
            true
        }
        popup.show()
    }

    override fun getItemCount(): Int {
        return if (message.isEmpty()) 0 else message.size + 1
    }

    @SuppressLint("NotifyDataSetChanged")
    fun newList(
        message: List<IRemoteNotification>,
        hasMore: Boolean,
        apiStatus: APIResponseStatus
    ) {
        this.message = message
        this.hasMore = hasMore
        this.apiStatus = apiStatus
        notifyDataSetChanged()
    }

    class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val progress: ProgressBar = itemView.findViewById(R.id.footerProgress)
        val loadMore: TextView = itemView.findViewById(R.id.footerLoadMoreTv)
        val caughtUp: TextView = itemView.findViewById(R.id.footerCaughtUpTv)
    }

    /** 12dp spacing between cards, matching iOS LazyVStack(spacing: 12). */
    class ItemSpacingDecoration(private val spacingPx: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION) return
            val adapter = parent.adapter ?: return
            if (adapter.getItemViewType(position) == TYPE_ITEM) {
                outRect.bottom = spacingPx
            }
        }
    }

    companion object {
        const val TYPE_ITEM = 1
        const val TYPE_FOOTER = 2
        private const val MENU_MARK_READ = 1
        private const val MENU_MARK_UNREAD = 2
        private const val MENU_ARCHIVE = 3
    }
}
