package app.suprsend.android.preference

import app.suprsend.user.preference.Category
import app.suprsend.user.preference.ChannelPreference

sealed class RecyclerViewItem(val viewType: Int, val id: String) {

    object TitleVo : RecyclerViewItem(0, "TitleVo") {
        const val VIEW_TYPE = 0
    }

    data class SectionVo(
        val title: String,
        val description: String = "",
        /** When true, no extra bottom margin (channel-level header uses VStack spacing via next item). */
        val tightBottom: Boolean = false
    ) : RecyclerViewItem(VIEW_TYPE, "SectionVo:${title.hashCode()}") {
        companion object {
            const val VIEW_TYPE = 1
        }
    }

    data class CategoryVo(
        val subCategory: Category,
        val isLastInSection: Boolean
    ) : RecyclerViewItem(VIEW_TYPE, subCategory.category + "CategoryVo") {
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
