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
import com.anyexchange.anyx.adapters.ProductListViewAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.activities.MainActivity
import com.anyexchange.anyx.classes.api.AnyApi
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import kotlinx.android.synthetic.main.fragment_market.view.*

/**
 * Created by anyexchange on 11/5/2017.
 */
open class MarketFragment : RefreshFragment(), LifecycleOwner {
    private var listView: ListView? = null
    lateinit var inflater: LayoutInflater
    open val onlyShowFavorites = false

    companion object {
        var resetHomeListeners = { }
    }

    private val productList: List<Product>
        get() {
            return if (onlyShowFavorites) {
                Product.map.values.filter { it.isFavorite }.toList().sortProducts()
            } else {
                Product.map.values.toList().alphabeticalProducts()
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

        listView?.adapter = ProductListViewAdapter(inflater, productList, onlyShowFavorites, onClick) { view, product ->
            setIsFavorite(view, product)
        }
//        listView?.setHeightBasedOnChildren()

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
                if (onlyShowFavorites) {
                    completeRefresh()
                } else {
                    favoritesUpdateListener?.favoritesUpdated()
                }
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
        if (onlyShowFavorites) {
            var productsUpdated = 0
            val time = Timespan.DAY
            val favoriteProducts = Product.favorites()
            val count = favoriteProducts.count()
            for (product in favoriteProducts) {
                //always check multiple exchanges?
                product.defaultTradingPair?.let { tradingPair ->
                    product.updateCandles(time, tradingPair, apiInitData, {
                        //OnFailure
                    }) { didUpdate ->
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
        } else {
            AnyApi(apiInitData).updateAllTickers(onFailure) {
                //Complete Market Refresh
                if (fullRefresh) {
                    refreshCompleteListener?.refreshComplete()
                }
                favoritesUpdateListener?.favoritesUpdated()
                completeRefresh()
                onComplete(true)
            }
        }
    }

    fun completeRefresh() {
        endRefresh()
        (listView?.adapter as? ProductListViewAdapter)?.productList = productList
//        (listView?.adapter as ProductListViewAdapter).notifyDataSetChanged()

        context?.let {
            Prefs(it).stashedProducts = Product.map.values.toList()
        }
        (listView?.adapter as? ProductListViewAdapter)?.notifyDataSetChanged()
        listView?.invalidateViews()
        listView?.refreshDrawableState()

//        val run = Runnable {
//            //reload content
//            (listView?.adapter as? ProductListViewAdapter)?.notifyDataSetChanged()
//            listView?.invalidateViews()
//            listView?.refreshDrawableState()
//        }
//        activity?.runOnUiThread(run)
    }

    private var favoritesUpdateListener: FavoritesUpdateListener? = null
    interface FavoritesUpdateListener {
        fun favoritesUpdated()
    }
    fun setFavoritesListener(listener: FavoritesUpdateListener) {
        this.favoritesUpdateListener = listener
    }


    private var refreshCompleteListener: RefreshCompleteListener? = null
    interface RefreshCompleteListener {
        fun refreshComplete()
    }
    fun setRefreshListener(listener: RefreshCompleteListener) {
        this.refreshCompleteListener = listener
    }

    override fun onResume() {
        super.onResume()
        resetHomeListeners()
        if (onlyShowFavorites) {
            autoRefresh = Runnable {
                if (!skipNextRefresh) {
                    refresh {}
                }
                skipNextRefresh = false

                handler.postDelayed(autoRefresh, TimeInMillis.halfMinute)
            }
            handler.postDelayed(autoRefresh, TimeInMillis.halfMinute)
        }
        refresh(false) { endRefresh() }
    }

    override fun onPause() {
        handler.removeCallbacks(autoRefresh)
        super.onPause()
    }
}
