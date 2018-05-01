package com.anyexchange.anyx.Fragments.Main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.anyexchange.anyx.Activities.MainActivity
import com.anyexchange.anyx.Adapters.ProductListViewAdapter
import com.anyexchange.anyx.Classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.fragment_market.view.*
import org.jetbrains.anko.support.v4.toast

/**
 * Created by anyexchange on 11/5/2017.
 */
class MarketFragment : RefreshFragment() {
    private var currentProduct: Product? = null
    private lateinit var listView: ListView

    lateinit var inflater: LayoutInflater

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

        setupSwipeRefresh(rootView)

        val selectGroup = lambda@ { product: Product ->
            currentProduct = product
            (activity as com.anyexchange.anyx.Activities.MainActivity).goToChartFragment(product.currency)
        }

        listView.adapter = ProductListViewAdapter(inflater, selectGroup)
        listView.setHeightBasedOnChildren()

        doneLoading()
        return rootView
    }

    override fun onResume() {
        //be smarter about only showing this when necessary, and maybe only refresh when encesary as well
        swipeRefreshLayout?.isRefreshing = true

        super.onResume()
        autoRefresh = Runnable {
            refresh({ })
            handler.postDelayed(autoRefresh, (TimeInSeconds.halfMinute * 1000))
        }
        handler.postDelayed(autoRefresh, (TimeInSeconds.halfMinute * 1000))
    }

    override fun onPause() {
        handler.removeCallbacks(autoRefresh)
        super.onPause()
    }


    override fun refresh(onComplete: (Boolean) -> Unit) {
        var productsUpdated = 0
        var accountListSize = Account.list.size
        val time = Timespan.DAY
        val onFailure = { result: Result.Failure<String, FuelError> ->  println("Error!: ${result.errorMessage}") }
        //TODO: check in about refreshing product list
        for (account in Account.list) {
            account.product.updateCandles(time, {
                toast("Error")
                onComplete(false)
            }, { didUpdate ->
                if (didUpdate) {
                    productsUpdated++
                    if (productsUpdated == accountListSize) {
                        (listView.adapter as ProductListViewAdapter).notifyDataSetChanged()
                        onComplete(true)
                    }
                } else {
                    GdaxApi.ticker(account.product.id).executeRequest(onFailure) { result ->
                        val ticker: ApiTicker = Gson().fromJson(result.value, object : TypeToken<ApiTicker>() {}.type)
                        val price = ticker.price.toDoubleOrNull()
                        if (price != null) {
                            account.product.price = price
                        }
                        productsUpdated++
                        if (productsUpdated == accountListSize) {
                            (listView.adapter as ProductListViewAdapter).notifyDataSetChanged()
                            onComplete(true)
                        }
                    }
                }
            })
        }
    }
}
