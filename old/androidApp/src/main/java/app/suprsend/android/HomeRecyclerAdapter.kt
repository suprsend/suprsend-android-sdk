package app.suprsend.android

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import app.suprsend.android.databinding.BannerItemBinding
import app.suprsend.android.databinding.BannerListBinding
import app.suprsend.android.databinding.GridProductImageBinding

class HomeRecyclerAdapter(private val list: List<BaseItem>) : RecyclerView.Adapter<BaseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            1 -> BaseViewHolder(GridProductImageBinding.inflate(LayoutInflater.from(parent.context)))
            2 -> BaseViewHolder(BannerListBinding.inflate(LayoutInflater.from(parent.context)))
            else -> throw IllegalStateException("not handled")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (list[position]) {
            is ProductVo -> 1
            is BannerListVo -> 2
            else -> return super.getItemViewType(position)
        }
    }

    override fun onBindViewHolder(baseViewHolder: BaseViewHolder, position: Int) {
        val item = list[position]

        baseViewHolder.bind(item)

        (baseViewHolder.binding as? GridProductImageBinding)?.apply {
            item as ProductVo
            AppCreator.loadUrl(root.context, item.url, imageIV)
            root.setOnClickListener {
                val context = it.context
                val productDetailIntent = Intent(context, ProductDetailsActivity::class.java)
                productDetailIntent.putExtra("productId", item.id)
                context.startActivity(productDetailIntent)
            }
        }

        (baseViewHolder.binding as? BannerListBinding)?.apply {
            item as BannerListVo
            container.removeAllViews()
            item.list.forEach { bannerVo ->
                val itemBinding = BannerItemBinding.inflate(LayoutInflater.from(root.context))
                AppCreator.loadUrl(root.context, bannerVo.url, itemBinding.imageView)
                itemBinding.root.setOnClickListener {
                    CommonAnalyticsHandler.track("banner_clicked")
                    Toast.makeText(container.context, "Banner Clicked ${bannerVo.id}", Toast.LENGTH_SHORT).show()
                }
                container.addView(itemBinding.root)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}
