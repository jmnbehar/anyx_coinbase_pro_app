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
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.anyexchange.anyx.adapters.ProductListViewAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.activities.MainActivity
import com.anyexchange.anyx.classes.api.AnyApi
import kotlinx.android.synthetic.main.fragment_market.view.*

/**
 * Created by anyexchange on 11/5/2017.
 */
open class MarketFragment : RefreshFragment(), LifecycleOwner {
    private var listView: ListView? = null

    lateinit var inflater: LayoutInflater

    open val onlyShowFavorites = false
    var updateAccountsFragment = { }

    companion object {
        fun newInstance(): MarketFragment
        {
            return MarketFragment()
        }
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

    override fun onResume() {
        //be smarter about only showing this when necessary, and maybe only refresh when necessary as well
        swipeRefreshLayout?.isRefreshing = true

        super.onResume()
        autoRefresh = Runnable {
            if (!skipNextRefresh) {
                refresh {}
            }
            skipNextRefresh = false
            handler.postDelayed(autoRefresh, TimeInMillis.halfMinute)
        }
        handler.postDelayed(autoRefresh, TimeInMillis.halfMinute)

        refresh { endRefresh() }
    }

    override fun onPause() {
        handler.removeCallbacks(autoRefresh)
        super.onPause()
    }

    private fun setIsFavorite(view: View, product: Product) {
        val popup = PopupMenu(activity, view)
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
                (listView?.adapter as ProductListViewAdapter).productList = productList
                (listView?.adapter as ProductListViewAdapter).notifyDataSetChanged()
            } else {
                //refresh other fragments
            }
            true
        }
        popup.show()
    }

    override fun refresh(onComplete: (Boolean) -> Unit) {
        var productsUpdated = 0
        val time = Timespan.DAY
        skipNextRefresh = true

        //TODO: move this entire refresh block up to a homeFragment level refresh that properly refreshes everything in homeFragment
        val onFailure: (result: Result.Failure<String, FuelError>) -> Unit = { result ->  toast("Error!: ${result.errorMessage}") }
        //TODO: check in about refreshing product list
        //TODO: use Account's updateAllCandles
        val favoriteProducts = Product.favorites()
        val count = favoriteProducts.count()
        AnyApi(apiInitData).updateAllTickers({ }) {
            (listView?.adapter as ProductListViewAdapter).productList = productList
            (listView?.adapter as ProductListViewAdapter).notifyDataSetChanged()
        }
        for (product in favoriteProducts) {
            //always check multiple exchanges?
            product.defaultTradingPair?.let { tradingPair ->
                product.updateCandles(time, tradingPair, apiInitData, {
                    //OnFailure
                    if (context != null) {
                        toast(R.string.error_message)
                    }
                    onComplete(false)
                }) { didUpdate ->
                    //OnSuccess
                    if (lifecycle.isCreatedOrResumed) {
                        if (didUpdate) {
                            productsUpdated++
                            if (productsUpdated == count) {
                                context?.let {
                                    Prefs(it).stashedProducts = Product.map.values.toList()
                                }
                                (listView?.adapter as ProductListViewAdapter).productList = productList
                                (listView?.adapter as ProductListViewAdapter).notifyDataSetChanged()
                                updateAccountsFragment()
                                onComplete(true)
                            }
                        } else {
                            AnyApi(apiInitData).ticker(tradingPair, onFailure) {
                                productsUpdated++
                                if (productsUpdated == count) {
                                    (listView?.adapter as ProductListViewAdapter).productList = productList
                                    (listView?.adapter as ProductListViewAdapter).notifyDataSetChanged()
                                    updateAccountsFragment()
                                    onComplete(true)
                                }
                            }
                        }
                    }
                }
            } ?: run {
                onFailure(Result.Failure(FuelError(Exception())))
            }
        }
    }
}
