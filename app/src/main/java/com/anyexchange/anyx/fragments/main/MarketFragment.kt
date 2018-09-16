package com.anyexchange.anyx.fragments.main

import android.arch.lifecycle.LifecycleOwner
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.anyexchange.anyx.adapters.ProductListViewAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.fragment_market.view.*

/**
 * Created by anyexchange on 11/5/2017.
 */
class MarketFragment : RefreshFragment(), LifecycleOwner {
    private var currentProduct: Product? = null
    private var listView: ListView? = null

    lateinit var inflater: LayoutInflater

    var updateAccountsFragment = { }

    companion object {
        fun newInstance(): MarketFragment
        {
            return MarketFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_market, container, false)
        listView = rootView.list_products
        this.inflater = inflater

        setupSwipeRefresh(rootView.swipe_refresh_layout as SwipeRefreshLayout)

        val selectGroup = lambda@ { product: Product ->
            currentProduct = product
            (activity as com.anyexchange.anyx.activities.MainActivity).goToChartFragment(product.currency)
        }

        listView?.adapter = ProductListViewAdapter(inflater, selectGroup)
        listView?.setHeightBasedOnChildren()

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


    override fun refresh(onComplete: (Boolean) -> Unit) {
        var productsUpdated = 0
        val accountListSize = Account.cryptoAccounts.size
        val time = Timespan.DAY
        skipNextRefresh = true

        //TODO: add this back occasionally
//        Product.updateAllProducts({ }, {})

        val onFailure: (result: Result.Failure<String, FuelError>) -> Unit = { result ->  toast("Error!: ${result.errorMessage}") }
        //TODO: check in about refreshing product list
        //TODO: use Account's updateAllCandles
        for (account in Account.cryptoAccounts) {
            account.product.updateCandles(time, null, apiInitData, {//OnFailure
                if (context != null) {
                    toast(R.string.error_message)
                }
                onComplete(false)
            }) { didUpdate ->   //OnSuccess
                if (lifecycle.isCreatedOrResumed) {
                    if (didUpdate) {
                        productsUpdated++
                        if (productsUpdated == accountListSize) {
                            context?.let {
                                Prefs(it).stashedCryptoAccountList = Account.cryptoAccounts
                            }
                            (listView?.adapter as ProductListViewAdapter).notifyDataSetChanged()
                            updateAccountsFragment()
                            onComplete(true)
                        }
                    } else {
                        CBProApi.ticker(apiInitData, account.product.id).get(onFailure) {
                            productsUpdated++
                            if (productsUpdated == accountListSize) {
                                (listView?.adapter as ProductListViewAdapter).notifyDataSetChanged()
                                updateAccountsFragment()
                                onComplete(true)
                            }
                        }
                    }
                }
            }
        }
    }
}
