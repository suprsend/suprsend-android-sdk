package app.suprsend.inbox

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.suprsend.R
import app.suprsend.SSApiInternal
import app.suprsend.base.getDrawableIdFromName
import app.suprsend.base.safeIntent
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import java.text.SimpleDateFormat

internal class SSInboxMessageAdapter
constructor(
    val message: List<SSInboxItemVo>,
    val ssInboxConfig: SSInboxConfig
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> {
                val view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.inbox_message_item, viewGroup, false)
                InboxItemViewHolder(view, ssInboxConfig)
            }
            else -> throw IllegalStateException("View type is unknown $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is InboxItemViewHolder) {
            holder.bind(holder, message[position])
        }
    }

    override fun getItemCount(): Int {
        return message.size
    }

    internal class InboxItemViewHolder(
        view: View,
        private val ssInboxConfig: SSInboxConfig
    ) : RecyclerView.ViewHolder(view) {
        private val context: Context = view.context
        private val itemContainer: RelativeLayout = view.findViewById(R.id.itemContainer)
        private val imageView: ImageView = view.findViewById(R.id.imageView)
        private val descriptionTv: TextView = view.findViewById(R.id.descriptionText)
        private val readStatusIv: ImageView = view.findViewById(R.id.readStatusIv)
        private val headerTv: TextView = view.findViewById(R.id.headerTv)
        private val timeTv: TextView = view.findViewById(R.id.timeTv)

        @SuppressLint("SetTextI18n", "SimpleDateFormat")
        fun bind(holder: InboxItemViewHolder, ssInboxItemVo: SSInboxItemVo) {

            try {
                val itemContainerDrawable = holder.itemContainer.background as? GradientDrawable
                itemContainerDrawable?.setColor(Color.parseColor(ssInboxConfig.cardBackgroundColor))
                itemContainerDrawable?.setStroke(
                    context.resources.getDimension(R.dimen.margin_1).toInt(),
                    Color.parseColor(ssInboxConfig.cardBorderColor)
                )
                holder.itemContainer.background = itemContainerDrawable

                val imageUrl = ssInboxItemVo.imageUrl
                val url = ssInboxItemVo.url
                if (!imageUrl.isNullOrBlank()) {
                    holder.imageView.visibility = View.VISIBLE
                    Glide.with(holder.context)
                        .load(imageUrl)
                        .apply(
                            RequestOptions()
                                .transform(RoundedCorners(context.resources.getDimension(R.dimen.margin_10).toInt()))
                                .placeholder(holder.context.getDrawableIdFromName("ic_ss_image") ?: -1)
                                .error(holder.context.getDrawableIdFromName("ic_ss_image") ?: -1)
                        )
                        .into(holder.imageView)
                } else {
                    holder.imageView.visibility = View.GONE
                }

                val text = ssInboxItemVo.text
                if (!text.isNullOrBlank()) {
                    holder.descriptionTv.setTextColor(Color.parseColor(ssInboxConfig.messageTextColor))
                    holder.descriptionTv.text = text
                    holder.descriptionTv.visibility = View.VISIBLE
                } else {
                    holder.descriptionTv.visibility = View.GONE
                }

                holder.itemContainer.setOnClickListener {
                    trackInboxNotificationClick(ssInboxItemVo, holder)
                    if (!url.isNullOrBlank()) {
                        launchUrl(holder, url)
                    }
                }

                holder.readStatusIv.visibility = if (ssInboxItemVo.seenOn == null) View.VISIBLE else View.GONE

                val header = ssInboxItemVo.header
                if (header.isNullOrBlank()) {
                    holder.headerTv.visibility = View.GONE
                } else {
                    holder.headerTv.visibility = View.VISIBLE
                    holder.headerTv.text = header
                    holder.headerTv.setTextColor(Color.parseColor(ssInboxConfig.messageTextColor))
                }
                val createdOn = ssInboxItemVo.createdOn
                if (createdOn == null) {
                    holder.timeTv.visibility = View.GONE
                } else {
                    holder.timeTv.visibility = View.VISIBLE
                    holder.timeTv.text = SimpleDateFormat("dd/MM/yyyy").format(createdOn) +
                        " at " +
                        SimpleDateFormat("hh:mm").format(createdOn)
                    holder.timeTv.setTextColor(Color.parseColor(ssInboxConfig.messageTextColor))
                    holder.timeTv.alpha = 0.7f
                }
            } catch (e: Exception) {
                Log.e(SSInboxActivity.TAG, "", e)
            }
        }

        private fun trackInboxNotificationClick(ssInboxItemVo: SSInboxItemVo, holder: InboxItemViewHolder) {
            SSApiInternal.inBoxNotificationClicked(ssInboxItemVo.nID)
            ssInboxItemVo.seenOn = System.currentTimeMillis()
            holder.readStatusIv.visibility = View.GONE
        }

        private fun launchUrl(holder: InboxItemViewHolder, url: String?) {
            val launchIntent = holder.context.safeIntent(link = url, defaultLauncherIntent = false)
            launchIntent?.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (launchIntent != null)
                holder.context.startActivity(launchIntent)
        }
    }
}
