package app.suprsend.android.preference

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import app.suprsend.android.R
import app.suprsend.android.databinding.ChannelItemBinding
import app.suprsend.android.databinding.ChannelPreferenceItemBinding
import app.suprsend.android.databinding.SectionItemBinding
import app.suprsend.android.databinding.SubCategoryItemBinding
import app.suprsend.android.layoutInflater
import app.suprsend.android.setVisibleOrGone
import app.suprsend.base.Debounce
import app.suprsend.user.preference.Channel
import app.suprsend.user.preference.ChannelPreference
import app.suprsend.user.preference.ChannelPreferenceOptions
import app.suprsend.user.preference.PreferenceOptions
import app.suprsend.user.preference.SubCategory
import com.nex3z.flowlayout.FlowLayout

typealias ChannelItemClick = (category: String, channel: String, checked: Boolean) -> Unit
typealias CategoryItemClick = (category: String, checked: Boolean) -> Unit
typealias ChannelPreferenceArrowClick = (category: String, expanded: Boolean) -> Unit
typealias ChannelPreferenceChangeClick = (channel: String, channelPreferenceOptions: ChannelPreferenceOptions) -> Unit

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
            RecyclerViewItem.SectionVo.VIEW_TYPE -> {
                val binding = SectionItemBinding.inflate(parent.layoutInflater(), parent, false)
                SectionHolder(binding)
            }
            RecyclerViewItem.SubCategoryVo.VIEW_TYPE -> {
                val binding = SubCategoryItemBinding.inflate(parent.layoutInflater(), parent, false)
                SubCategoryHolder(binding)
            }
            RecyclerViewItem.ChannelPreferenceVo.VIEW_TYPE -> {
                val binding =
                    ChannelPreferenceItemBinding.inflate(parent.layoutInflater(), parent, false)
                ChannelPreferenceHolder(binding)
            }
            else -> throw IllegalStateException("Not found : onCreateViewHolder")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = itemList[position]) {
            is RecyclerViewItem.SectionVo -> {
                (holder as SectionHolder).bind(item)
            }
            is RecyclerViewItem.SubCategoryVo -> {
                (holder as SubCategoryHolder).bind(
                    item,
                    categoryItemClick = categoryItemClick,
                    channelItemClick = channelItemClick
                )
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

private data class SectionHolder(
    val binding: SectionItemBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: RecyclerViewItem.SectionVo) {
        binding.obj = item
        binding.sectionDescTv.visibility = setVisibleOrGone(item.description.isNotBlank())
    }
}

private data class SubCategoryHolder(
    val binding: SubCategoryItemBinding
) : RecyclerView.ViewHolder(binding.root) {
    val subCategoryDebounce = Debounce()
    val channelDebounce = Debounce()
    fun bind(
        item: RecyclerViewItem.SubCategoryVo,
        categoryItemClick: CategoryItemClick,
        channelItemClick: ChannelItemClick
    ) {
        val subCategory = item.subCategory
        binding.obj = subCategory
        binding.subCategoryDescTv.visibility =
            setVisibleOrGone(subCategory.description.isNotBlank())
        binding.subCategoryCheckbox.isEnabled = subCategory.isEditable
        binding.subCategoryCheckbox.isOn = subCategory.preferenceOptions == PreferenceOptions.OPT_IN
        binding.subCategoryCheckbox.setOnToggledListener { _, isOn ->

            subCategoryDebounce.debounceLast {
                categoryItemClick.invoke(subCategory.category, isOn)
            }
        }

        binding.channelFlowLayout.removeAllViews()
        if (subCategory.preferenceOptions == PreferenceOptions.OPT_IN) {
            subCategory.channels.forEachIndexed { index, channel ->
                addChannel(
                    index == 0,
                    channel,
                    binding.channelFlowLayout,
                    subCategory,
                    channelDebounce,
                    channelItemClick
                )
            }
        }
        binding.subCategoryDivider.visibility = setVisibleOrGone(!item.isLast)
    }

    private fun addChannel(
        isStart: Boolean,
        channel: Channel,
        flowLayout: FlowLayout,
        subCategory: SubCategory,
        channelDebounce: Debounce,
        channelItemClick: ChannelItemClick
    ) {
        val isChecked = channel.preferenceOptions == PreferenceOptions.OPT_IN
        val channelBinding = ChannelItemBinding.inflate(flowLayout.layoutInflater())
        val chip = channelBinding.root as AppCompatTextView
        chip.text = channel.channel

        val isEnabled = channel.isEditable

        if (isChecked) {
            chip.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_chip_selected, 0, 0, 0)
        } else {
            if (isEnabled) {
                chip.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_chip_unselected, 0, 0, 0)
            } else {
                chip.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_chip_disabled_unselected,
                    0,
                    0,
                    0
                )
            }
        }
        chip.setTag(R.id.item_tag, isChecked.toString())
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.topMargin =
            chip.context?.resources?.getDimension(R.dimen.margin_10)?.toInt() ?: 0
        if (isStart) {
            layoutParams.marginStart = 0
        } else {
            layoutParams.marginStart =
                chip.context?.resources?.getDimension(R.dimen.margin_10)?.toInt() ?: 0
        }
        chip.layoutParams = layoutParams
        flowLayout.addView(chip)
        if (channel.isEditable) {
            chip.setOnClickListener {
                val isOn = !chip.getTag(R.id.item_tag).toString().toBoolean()
                if (isOn) {
                    chip.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_chip_selected,
                        0,
                        0,
                        0
                    )
                } else {
                    chip.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_chip_unselected,
                        0,
                        0,
                        0
                    )
                }
                chip.setTag(R.id.item_tag, isOn.toString())
                channelDebounce.debounceLast {
                    channelItemClick(
                        subCategory.category,
                        channel.channel,
                        isOn
                    )
                }
            }
        }
    }
}

private data class ChannelPreferenceHolder(
    val binding: ChannelPreferenceItemBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(
        item: RecyclerViewItem.ChannelPreferenceVo,
        channelPreferenceArrowClick: ChannelPreferenceArrowClick,
        channelPreferenceChangeClick: ChannelPreferenceChangeClick
    ) {
        val channelPreference = item.channelPreference
        binding.obj = channelPreference
        binding.sectionArrowIV.setOnClickListener {
            expandCollapse(channelPreferenceArrowClick, channelPreference)
        }
        binding.prefNameTv.setOnClickListener {
            expandCollapse(channelPreferenceArrowClick, channelPreference)
        }
        binding.prefDescTv.setOnClickListener {
            expandCollapse(channelPreferenceArrowClick, channelPreference)
        }
        binding.allPrefRG.visibility = setVisibleOrGone(item.isExpanded)
        binding.allPrefRG.setOnCheckedChangeListener(null)
        if (channelPreference.isRestricted) {
            binding.allPrefRG.check(R.id.requiredRb)
        } else {
            binding.allPrefRG.check(R.id.allRb)
        }
        binding.allPrefRG.setOnCheckedChangeListener { _, id ->
            val checkedRb = binding.allPrefRG.findViewById<RadioButton>(id)
            val pref = if (checkedRb == binding.allRb) {
                ChannelPreferenceOptions.ALL
            } else {
                ChannelPreferenceOptions.REQUIRED
            }
            if (item.channelPreference.toChannelPreferenceOptions() != pref)
                channelPreferenceChangeClick.invoke(channelPreference.channel, pref)
        }
    }

    private fun expandCollapse(
        channelPreferenceArrowClick: ChannelPreferenceArrowClick,
        channelPreference: ChannelPreference
    ) {
        val isExpanded = binding.sectionArrowIV.rotation == 0f
        channelPreferenceArrowClick(channelPreference.channel, isExpanded)
        if (isExpanded) {
            binding.sectionArrowIV.animate().rotation(180f).setDuration(300)
        } else {
            binding.sectionArrowIV.animate().rotation(0f).setDuration(300)
        }
        binding.allPrefRG.visibility = setVisibleOrGone(isExpanded)
    }
}
