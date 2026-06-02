package app.suprsend.android.preference

import androidx.recyclerview.widget.DiffUtil

class UserPreferenceDiffUtilCallback(
    private val oldList: List<RecyclerViewItem>,
    private val newList: List<RecyclerViewItem>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return when {
            oldList[oldItemPosition] == newList[newItemPosition] -> true
            else -> false
        }
    }
}
