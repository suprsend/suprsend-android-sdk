package app.suprsend.android

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.suprsend.android.databinding.InboxMessageFragmentBinding
import app.suprsend.feed.APIResponseStatus
import app.suprsend.feed.IStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class SSInboxMessageListFragment : Fragment() {

    lateinit var binding: InboxMessageFragmentBinding

    private lateinit var adapter: SSInboxMessageAdapter

    private var unsubscribeInbox: (() -> Unit)? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    private val inboxViewModel = InboxViewModel.shared

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = InboxMessageFragmentBinding.inflate(inflater, container, false)
        try {
            binding.inboxLL.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.inbox_screen_bg))
            initializeRecyclerView()
        } catch (e: Exception) {
            Log.e(AppConstants.TAG, "App: onCreateView", e)
        }
        return binding.root
    }

    private fun initializeRecyclerView() {
        binding.socketStatusIv.setVisible(false)
        binding.inboxRv.layoutManager = LinearLayoutManager(activity)
        adapter = SSInboxMessageAdapter(
            inflater = layoutInflater,
            viewModel = inboxViewModel
        )
        adapter.onLoadMoreClick = {
            coroutineScope.launch { inboxViewModel.loadMore() }
        }
        binding.inboxRv.adapter = adapter
        val spacing = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            12f,
            resources.displayMetrics
        ).toInt()
        binding.inboxRv.addItemDecoration(SSInboxMessageAdapter.ItemSpacingDecoration(spacing))
        binding.inboxRv.addOnScrollListener(nextPageScrollListener())

        unsubscribeInbox = inboxViewModel.subscribe {
            refreshStoreChips()
            updateList()
        }

        if (inboxViewModel.stores.isEmpty()) {
            binding.storeTabsScroll.visibility = View.GONE
        } else {
            binding.storeTabsScroll.visibility = View.VISIBLE
            initStoreChips()
        }

        refreshStoreChips()
        updateList()
    }

    private fun nextPageScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                coroutineScope.launch { inboxViewModel.loadMore() }
            }
        }
    }

    private fun initStoreChips() {
        binding.storeChipsLL.removeAllViews()
        inboxViewModel.stores.forEach { store ->
            val chip = layoutInflater.inflate(R.layout.inbox_store_chip, binding.storeChipsLL, false)
            chip.tag = store.storeId
            chip.setOnClickListener {
                inboxViewModel.changeStore(store.storeId)
                refreshStoreChips()
            }
            binding.storeChipsLL.addView(chip)
        }
        refreshStoreChips()
    }

    private fun refreshStoreChips() {
        val activeStoreId = inboxViewModel.activeStoreId
        for (i in 0 until binding.storeChipsLL.childCount) {
            val chip = binding.storeChipsLL.getChildAt(i)
            val storeId = chip.tag as? String ?: continue
            val store = inboxViewModel.stores.firstOrNull { it.storeId == storeId } ?: continue
            bindStoreChip(
                chip = chip,
                store = store,
                count = inboxViewModel.storeBadges[storeId] ?: 0,
                isActive = storeId == activeStoreId
            )
        }
    }

    private fun bindStoreChip(chip: View, store: IStore, count: Int, isActive: Boolean) {
        val labelTv = chip.findViewById<TextView>(R.id.chipLabelTv)
        val badgeTv = chip.findViewById<TextView>(R.id.chipBadgeTv)
        labelTv.text = store.label

        chip.setBackgroundResource(
            if (isActive) R.drawable.inbox_chip_selected else R.drawable.inbox_chip_unselected
        )
        labelTv.setTextColor(
            if (isActive) Color.WHITE else Color.BLACK
        )

        if (count > 0) {
            badgeTv.visibility = View.VISIBLE
            badgeTv.text = count.toString()
            if (isActive) {
                badgeTv.setBackgroundResource(R.drawable.inbox_badge_on_selected)
                badgeTv.setTextColor(ContextCompat.getColor(chip.context, R.color.inbox_accent))
            } else {
                badgeTv.setBackgroundResource(R.drawable.inbox_badge_on_unselected)
                badgeTv.setTextColor(Color.WHITE)
            }
        } else {
            badgeTv.visibility = View.GONE
        }

        // Last chip should not reserve trailing 8dp gap beyond scroll padding.
        val params = chip.layoutParams as LinearLayout.LayoutParams
        val isLast = store.storeId == inboxViewModel.stores.lastOrNull()?.storeId
        params.marginEnd = if (isLast) 0 else resources.getDimensionPixelSize(R.dimen.inbox_chip_spacing)
        chip.layoutParams = params
    }

    private fun updateList() {
        try {
            val isInitialLoading =
                inboxViewModel.apiStatus == APIResponseStatus.LOADING && inboxViewModel.notifications.isEmpty()
            showLoading(isInitialLoading)
            if (isInitialLoading) {
                return
            }
            if (inboxViewModel.notifications.isEmpty()) {
                if (binding.emptyMessageTv.context.isConnected()) {
                    showEmptyScreen()
                } else {
                    binding.emptyTitleTv.text = getString(R.string.no_internet)
                    binding.emptySubtitleTv.setVisible(false)
                    binding.emptyMessageTv.setVisible(true)
                    binding.inboxRv.setVisible(false)
                    binding.progressBar.setVisible(false)
                }
            } else {
                showDataScreen()
                adapter.newList(
                    message = inboxViewModel.notifications,
                    hasMore = inboxViewModel.hasMore,
                    apiStatus = inboxViewModel.apiStatus
                )
            }
        } catch (e: Exception) {
            Log.e(AppConstants.TAG, "App: setRecyclerViewData", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unsubscribeInbox?.invoke()
        unsubscribeInbox = null
        coroutineScope.coroutineContext[Job]?.cancel()
    }

    private fun showEmptyScreen() {
        binding.emptyTitleTv.setText(R.string.no_notifications_yet)
        binding.emptySubtitleTv.setText(R.string.new_messages_will_appear_here)
        binding.emptySubtitleTv.setVisible(true)
        binding.emptyMessageTv.setVisible(true)
        binding.inboxRv.setVisible(false)
        binding.progressBar.setVisible(false)
    }

    private fun showLoading(isLoading: Boolean) {
        if (!isLoading) {
            binding.progressBar.setVisible(false)
            return
        }
        binding.progressBar.setVisible(true)
        binding.emptyMessageTv.setVisible(false)
        binding.inboxRv.setVisible(false)
    }

    fun showDataScreen() {
        binding.inboxRv.setVisible(true)
        binding.progressBar.setVisible(false)
        binding.emptyMessageTv.setVisible(false)
    }
}
