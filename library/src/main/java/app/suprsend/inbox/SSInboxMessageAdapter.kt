package app.suprsend.inbox

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import app.suprsend.R
import app.suprsend.base.getDrawableIdFromName
import app.suprsend.base.safeIntent
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import java.lang.IllegalStateException

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
            else -> throw  IllegalStateException("View type is unknown $viewType")
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
        private val imageView: ImageView = view.findViewById(R.id.imageView)
        private val descriptionTv: TextView = view.findViewById(R.id.descriptionText)
        private val button: TextView = view.findViewById(R.id.buttonText)

        fun bind(holder: InboxItemViewHolder, ssInboxItemVo: SSInboxItemVo) {
            val imageUrl = ssInboxItemVo.imageUrl
            val url = ssInboxItemVo.url
            if (imageUrl != null) {
                Glide.with(holder.context)
                    .load(imageUrl)
                    .apply(
                        RequestOptions()
                            .transform(RoundedCorners(context.resources.getDimension(R.dimen.margin_10).toInt()))
                            .placeholder(holder.context.getDrawableIdFromName("ic_ss_image") ?: -1)
                            .error(holder.context.getDrawableIdFromName("ic_ss_image") ?: -1)
                    )
                    .into(holder.imageView)

                holder.imageView.setOnClickListener {
                    launchUrl(holder, url)
                }
            }

            val text = ssInboxItemVo.text
            if (!text.isNullOrBlank()) {
                holder.descriptionTv.setTextColor(Color.parseColor(ssInboxConfig.messageTextColor))
                holder.descriptionTv.text = text
                holder.descriptionTv.visibility = View.VISIBLE
            } else {
                holder.descriptionTv.visibility = View.GONE
            }

            val button = ssInboxItemVo.button

            if (!button.isNullOrBlank()) {
                val buttonDrawable = holder.button.background
                buttonDrawable?.setColorFilter(Color.parseColor(ssInboxConfig.messageActionBgColor), PorterDuff.Mode.SRC_IN)
                holder.button.background = buttonDrawable
                holder.button.setTextColor(Color.parseColor(ssInboxConfig.messageActionTextColor))
                holder.button.text = button
                holder.button.visibility = View.VISIBLE
                if (!url.isNullOrBlank()) {
                    holder.button.setOnClickListener {
                        launchUrl(holder, url)
                    }
                } else {
                    holder.button.setOnClickListener(null)
                }
            } else {
                holder.button.visibility = View.GONE
            }
        }

        private fun launchUrl(holder: InboxItemViewHolder, url: String?) {
            val launchIntent = holder.context.safeIntent(link = url, defaultLauncherIntent = false)
            launchIntent?.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (launchIntent != null)
                holder.context.startActivity(launchIntent)
        }

    }
}

