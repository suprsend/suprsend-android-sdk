package app.suprsend.android.preference

import app.suprsend.user.preference.ChannelPreference
import app.suprsend.user.preference.SubCategory

sealed class RecyclerViewItem(val viewType: Int, val id: String) {
    data class SectionVo(
        val idd: String,
        val title: String,
        val description: String = ""
    ) : RecyclerViewItem(VIEW_TYPE, idd + "SectionVo") {
        companion object {
            const val VIEW_TYPE = 1
        }
    }

    data class SubCategoryVo(
        val subCategory: SubCategory,
        val isLast: Boolean
    ) : RecyclerViewItem(VIEW_TYPE, subCategory.name + "SubCategoryVo") {
        companion object {
            const val VIEW_TYPE = 2
        }
    }

    data class ChannelPreferenceVo(
        val channelPreference: ChannelPreference,
        var isExpanded: Boolean
    ) : RecyclerViewItem(VIEW_TYPE, channelPreference.channel + "ChannelPreferenceVo") {
        companion object {
            const val VIEW_TYPE = 3
        }
    }
}
