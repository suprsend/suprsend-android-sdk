package app.suprsend.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import app.suprsend.android.databinding.ItemImageBinding

class WelcomeAdapter(
    private val layoutInflater: LayoutInflater,
    private val list: List<WelcomeVo>
) : PagerAdapter() {
    override fun getCount(): Int {
        return list.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as View
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val itemView = ItemImageBinding.inflate(layoutInflater)
        AppCreator.loadUrl(container.context, list[position].url, itemView.imageIV)
        container.addView(itemView.root)
        return itemView.root
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}
