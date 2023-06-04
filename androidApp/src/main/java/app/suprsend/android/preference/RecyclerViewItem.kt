package app.suprsend.android.preference

import app.suprsend.user.preference.Section
import app.suprsend.user.preference.SubCategory

sealed class RecyclerViewItem(val viewType: Int, val id: String) {
    data class SectionVo(
        val section: Section
    ) : RecyclerViewItem(VIEW_TYPE, section.name) {
        companion object {
            const val VIEW_TYPE = 1
        }
    }

    data class SubCategoryVo(
        val subCategory: SubCategory,
        val isLast: Boolean
    ) : RecyclerViewItem(VIEW_TYPE, subCategory.name) {
        companion object {
            const val VIEW_TYPE = 2
        }
    }
}
