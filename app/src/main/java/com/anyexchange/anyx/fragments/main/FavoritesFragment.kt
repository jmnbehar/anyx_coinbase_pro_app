package com.anyexchange.anyx.fragments.main

import android.arch.lifecycle.LifecycleOwner
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.PopupMenu
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.activities.MainActivity
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import kotlinx.android.synthetic.main.fragment_market.view.*
import android.widget.AbsListView
import com.anyexchange.anyx.adapters.FavoritesListViewAdapter
import com.anyexchange.anyx.classes.LazyLoader


/**
 * Created by anyexchange on 11/5/2017.
 */
class FavoritesFragment : RefreshFragment(), LifecycleOwner {
    private var listView: ListView? = null
    lateinit var inflater: LayoutInflater

    companion object {
        var resetHomeListeners = { }
    }

    private val productList: List<Product>
        get() {
            val context = context
            return if (context != null && Prefs(context).sortFavoritesAlphabetical) {
                Product.map.values.filter { it.isFavorite }.toList().sortProductsAlphabetical()
            } else {
                Product.map.values.filter { it.isFavorite }.toList().sortProducts()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_market, container, false)
        listView = rootView.list_products
        this.inflater = inflater

        setupSwipeRefresh(rootView.swipe_refresh_layout as SwipeRefreshLayout)

        val onClick = lambda@ { product: Product ->
            (activity as MainActivity).goToChartFragment(product.currency)
        }

        listView?.adapter = FavoritesListViewAdapter(inflater, productList, onClick) { view, product ->
            setIsFavorite(view, product)
        }

        listView?.setOnScrollListener(object : LazyLoader() {
            override fun loadMore(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                (listView?.adapter as FavoritesListViewAdapter).increaseSize()
            }
        })

        dismissProgressSpinner()
        return rootView
    }

    private fun setIsFavorite(view: View, product: Product) {
        context?.let {
            val popup = PopupMenu(it, view)
            //Inflating the Popup using xml file
            popup.menuInflater.inflate(R.menu.product_popup_menu, popup.menu)
            popup.menu.findItem(R.id.setFavorite).isVisible = !product.isFavorite
            popup.menu.findItem(R.id.removeFavorite).isVisible = product.isFavorite

            popup.setOnMenuItemClickListener { item: MenuItem? ->
                when (item?.itemId) {
                    R.id.setFavorite -> {
                        product.isFavorite = true
                    }
                    R.id.removeFavorite -> {
                        product.isFavorite = false
                    }
                }
                completeRefresh()
                true
            }
            popup.show()
        }
    }

    override fun refresh(onComplete: (Boolean) -> Unit) {
        refresh(true, onComplete)
    }
    fun refresh(fullRefresh: Boolean, onComplete: (Boolean) -> Unit) {
        val onFailure: (result: Result.Failure<String, FuelError>) -> Unit = { result ->
            toast("Error: ${result.errorMessage}")
            onComplete(false)
        }
        swipeRefreshLayout?.isRefreshing = true

        var productsUpdated = 0
        val time = Timespan.DAY
        val favoriteProducts = Product.favorites()
        val count = favoriteProducts.count()
        for (product in favoriteProducts) {
            //always check multiple exchanges?
            product.defaultTradingPair?.let { tradingPair ->
                product.updateCandles(time, tradingPair, apiInitData, {
                    //OnFailure
                }) {
                    //OnSuccess
                    if (lifecycle.isCreatedOrResumed) {
                        productsUpdated++
                        if (productsUpdated == count) {
                            //update Favorites Tab
                            if (fullRefresh) {
                                refreshCompleteListener?.refreshComplete()
                            }
                            completeRefresh()
                            onComplete(true)
                        }
                    }
                }
            } ?: run {
                onFailure(Result.Failure(FuelError(Exception())))
            }
        }
    }

    fun completeRefresh() {
        endRefresh()
        (listView?.adapter as? FavoritesListViewAdapter)?.productList = productList

        context?.let {
            Prefs(it).stashedProducts = Product.map.values.toList()
        }
        (listView?.adapter as? FavoritesListViewAdapter)?.notifyDataSetChanged()
        listView?.invalidateViews()
        listView?.refreshDrawableState()
    }

    private var refreshCompleteListener: MarketFragment.RefreshCompleteListener? = null

    fun setRefreshListener(listener: MarketFragment.RefreshCompleteListener) {
        this.refreshCompleteListener = listener
    }

    override fun onResume() {
        shouldHideSpinner = false
        super.onResume()

        resetHomeListeners()

        autoRefresh = Runnable {
            if (!skipNextRefresh) {
                refresh {}
            }
            skipNextRefresh = false

            handler.postDelayed(autoRefresh, TimeInMillis.halfMinute)
        }
        handler.postDelayed(autoRefresh, TimeInMillis.halfMinute)

        refresh(false) { endRefresh() }
    }

    override fun onPause() {
        handler.removeCallbacks(autoRefresh)
        super.onPause()
    }
}
