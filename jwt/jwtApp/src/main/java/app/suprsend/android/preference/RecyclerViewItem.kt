package app.suprsend.android.preference

import app.suprsend.user.preference.Category
import app.suprsend.user.preference.ChannelPreference
import app.suprsend.user.preference.PreferenceOptions

sealed class RecyclerViewItem(val viewType: Int, val id: String) {
    data class SectionVo(
        val title: String,
        val description: String = ""
    ) : RecyclerViewItem(VIEW_TYPE,  "SectionVo:${title.hashCode()}") {
        companion object {
            const val VIEW_TYPE = 1
        }
    }

    data class CategoryVo(
        val subCategory: Category,
        val isLast: Boolean
    ) : RecyclerViewItem(VIEW_TYPE, subCategory.name + "CategoryVo") {
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
