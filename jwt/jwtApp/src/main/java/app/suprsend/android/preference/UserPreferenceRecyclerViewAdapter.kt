package app.suprsend.android.preference

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import app.suprsend.android.R
import app.suprsend.android.databinding.ChannelItemBinding
import app.suprsend.android.databinding.ChannelPreferenceItemBinding
import app.suprsend.android.databinding.PreferenceTitleItemBinding
import app.suprsend.android.databinding.SectionItemBinding
import app.suprsend.android.databinding.SubCategoryItemBinding
import app.suprsend.android.layoutInflater
import app.suprsend.android.setVisibleOrGone
import app.suprsend.user.preference.Category
import app.suprsend.user.preference.CategoryChannel
import app.suprsend.user.preference.ChannelLevelPreferenceOptions
import app.suprsend.user.preference.ChannelPreference
import app.suprsend.user.preference.PreferenceOptions

typealias ChannelItemClick = (category: String, channel: String, checked: Boolean) -> Unit
typealias CategoryItemClick = (category: String, checked: Boolean) -> Unit
typealias ChannelPreferenceArrowClick = (category: String, expanded: Boolean) -> Unit
typealias ChannelPreferenceChangeClick = (channel: String, preference: ChannelLevelPreferenceOptions) -> Unit

class UserPreferenceRecyclerViewAdapter(
    private val categoryItemClick: CategoryItemClick,
    private val channelItemClick: ChannelItemClick,
    private val channelPreferenceArrowClick: ChannelPreferenceArrowClick,
    private val channelPreferenceChangeClick: ChannelPreferenceChangeClick
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val itemList = arrayListOf<RecyclerViewItem>()

    override fun getItemCount(): Int = itemList.size

    override fun getItemViewType(position: Int): Int {
        return itemList[position].viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            RecyclerViewItem.TitleVo.VIEW_TYPE -> {
                val binding = PreferenceTitleItemBinding.inflate(parent.layoutInflater(), parent, false)
                TitleHolder(binding)
            }
            RecyclerViewItem.SectionVo.VIEW_TYPE -> {
                val binding = SectionItemBinding.inflate(parent.layoutInflater(), parent, false)
                SectionHolder(binding)
            }
            RecyclerViewItem.CategoryVo.VIEW_TYPE -> {
                val binding = SubCategoryItemBinding.inflate(parent.layoutInflater(), parent, false)
                SubCategoryHolder(binding)
            }
            RecyclerViewItem.ChannelPreferenceVo.VIEW_TYPE -> {
                val binding = ChannelPreferenceItemBinding.inflate(parent.layoutInflater(), parent, false)
                ChannelPreferenceHolder(binding)
            }
            else -> throw IllegalStateException("Not found : onCreateViewHolder")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = itemList[position]) {
            is RecyclerViewItem.TitleVo -> Unit
            is RecyclerViewItem.SectionVo -> {
                (holder as SectionHolder).bind(item)
            }
            is RecyclerViewItem.CategoryVo -> {
                (holder as SubCategoryHolder).bind(item, categoryItemClick = categoryItemClick, channelItemClick = channelItemClick)
            }
            is RecyclerViewItem.ChannelPreferenceVo -> {
                (holder as ChannelPreferenceHolder).bind(
                    item,
                    channelPreferenceArrowClick = channelPreferenceArrowClick,
                    channelPreferenceChangeClick = channelPreferenceChangeClick
                )
            }
        }
    }

    fun setItems(items: List<RecyclerViewItem>) {
        val diffResult = DiffUtil.calculateDiff(UserPreferenceDiffUtilCallback(itemList, items))
        itemList.clear()
        itemList.addAll(items)
        diffResult.dispatchUpdatesTo(this)
    }
}

private class TitleHolder(
    binding: PreferenceTitleItemBinding
) : RecyclerView.ViewHolder(binding.root)

private class SectionHolder(
    val binding: SectionItemBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: RecyclerViewItem.SectionVo) {
        binding.obj = item
        binding.sectionDescTv.visibility = setVisibleOrGone(item.description.isNotBlank())
        val bottom = if (item.tightBottom) 0 else binding.root.resources.getDimensionPixelSize(R.dimen.pref_section_bottom)
        (binding.root.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = bottom
        binding.root.requestLayout()
    }
}

private class SubCategoryHolder(
    val binding: SubCategoryItemBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(
        item: RecyclerViewItem.CategoryVo,
        categoryItemClick: CategoryItemClick,
        channelItemClick: ChannelItemClick
    ) {
        val subCategory = item.subCategory
        binding.obj = subCategory
        binding.subCategoryDescTv.visibility = setVisibleOrGone(!subCategory.description.isNullOrBlank())

        binding.subCategoryCheckbox.setOnCheckedChangeListener(null)
        binding.subCategoryCheckbox.isEnabled = subCategory.isEditable
        binding.subCategoryCheckbox.isChecked = subCategory.preference == PreferenceOptions.optIn
        binding.subCategoryCheckbox.alpha = if (subCategory.isEditable) 1f else 0.5f
        binding.subCategoryCheckbox.setOnCheckedChangeListener { _, isChecked ->
            categoryItemClick.invoke(subCategory.category, isChecked)
        }

        val channels = subCategory.channels
        binding.channelScroll.visibility = setVisibleOrGone(!channels.isNullOrEmpty())
        binding.channelChipGroup.removeAllViews()
        channels?.forEach { channel ->
            addChannel(channel, binding.channelChipGroup, subCategory, channelItemClick)
        }

        // iOS draws a divider under every subcategory row
        binding.subCategoryDivider.visibility = View.VISIBLE
        val bottomExtra = if (item.isLastInSection) {
            binding.root.resources.getDimensionPixelSize(R.dimen.pref_section_group_bottom)
        } else {
            0
        }
        (binding.root.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = bottomExtra
        binding.root.requestLayout()
    }

    private fun addChannel(
        channel: CategoryChannel,
        channelGroup: LinearLayout,
        subCategory: Category,
        channelItemClick: ChannelItemClick
    ) {
        val isChecked = channel.preference == PreferenceOptions.optIn
        val channelBinding = ChannelItemBinding.inflate(channelGroup.layoutInflater(), channelGroup, false)
        channelBinding.channelNameTv.text = channel.channel
        channelBinding.channelCircle.setBackgroundResource(circleDrawable(isChecked, channel.isEditable))
        channelBinding.root.alpha = if (channel.isEditable) 1f else 0.6f
        channelBinding.root.isEnabled = channel.isEditable
        channelBinding.root.setOnClickListener {
            if (!channel.isEditable) return@setOnClickListener
            channelItemClick(
                subCategory.category,
                channel.channel,
                !isChecked
            )
        }
        channelGroup.addView(channelBinding.root)
    }

    private fun circleDrawable(selected: Boolean, editable: Boolean): Int {
        return when {
            selected && editable -> R.drawable.pref_channel_circle_selected
            selected && !editable -> R.drawable.pref_channel_circle_selected_disabled
            !selected && editable -> R.drawable.pref_channel_circle_unselected
            else -> R.drawable.pref_channel_circle_unselected_disabled
        }
    }
}

private class ChannelPreferenceHolder(
    val binding: ChannelPreferenceItemBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(
        item: RecyclerViewItem.ChannelPreferenceVo,
        channelPreferenceArrowClick: ChannelPreferenceArrowClick,
        channelPreferenceChangeClick: ChannelPreferenceChangeClick
    ) {
        val channelPreference = item.channelPreference
        binding.obj = channelPreference
        binding.prefDescTv.setText(
            if (channelPreference.isRestricted) {
                R.string.allow_required_notifications_only
            } else {
                R.string.allow_all_notifications
            }
        )
        binding.expandedTitleTv.text = binding.root.context.getString(
            R.string.channel_preferences_title,
            channelPreference.channel
        )

        binding.expandedContainer.visibility = setVisibleOrGone(item.isExpanded)
        updateRadioSelection(channelPreference.isRestricted)

        binding.prefHeader.setOnClickListener {
            val next = !item.isExpanded
            item.isExpanded = next
            channelPreferenceArrowClick(channelPreference.channel, next)
            binding.expandedContainer.visibility = setVisibleOrGone(next)
        }

        binding.allRow.setOnClickListener {
            if (channelPreference.isRestricted) {
                channelPreferenceChangeClick.invoke(
                    channelPreference.channel,
                    ChannelLevelPreferenceOptions.all
                )
            }
        }
        binding.requiredRow.setOnClickListener {
            if (!channelPreference.isRestricted) {
                channelPreferenceChangeClick.invoke(
                    channelPreference.channel,
                    ChannelLevelPreferenceOptions.required
                )
            }
        }
    }

    private fun updateRadioSelection(isRestricted: Boolean) {
        binding.allRadioInner.visibility = setVisibleOrGone(!isRestricted)
        binding.requiredRadioInner.visibility = setVisibleOrGone(isRestricted)
    }
}
