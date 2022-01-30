package app.suprsend.inbox

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import app.suprsend.R
import app.suprsend.base.getDrawableIdFromName
import app.suprsend.base.safeIntent
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.lang.IllegalStateException

//Todo : Change to submit list
//Todo : Implement diff utils adapter to maintain position
internal class SSInboxMessageAdapter
constructor(
    val message: List<SSInboxItemVo>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> {
                val view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.inbox_message_item, viewGroup, false);
                InboxItemViewHolder(view)
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
}

internal class InboxItemViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    val context: Context = view.context
    val imageView: ImageView = view.findViewById(R.id.imageView)
    val descriptionTv: TextView = view.findViewById(R.id.descriptionText)
    val button: AppCompatButton = view.findViewById(R.id.buttonText)

    fun bind(holder: InboxItemViewHolder, ssInboxItemVo: SSInboxItemVo) {
        val imageUrl = ssInboxItemVo.imageUrl
        val url = ssInboxItemVo.url
        if (imageUrl != null) {
            Glide.with(holder.context)
                .load(imageUrl)
                .apply(
                    RequestOptions()
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
            //Todo : Testing color
            holder.descriptionTv.setTextColor(Color.parseColor("#FFFFFF"))
            holder.descriptionTv.text = text
            holder.descriptionTv.visibility = View.VISIBLE
        } else {
            holder.descriptionTv.visibility = View.GONE
        }

        val button = ssInboxItemVo.button

        if (!button.isNullOrBlank()) {
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