package com.jmnbehar.anyx.Fragments.Main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jmnbehar.anyx.Activities.MainActivity
import com.jmnbehar.anyx.Adapters.ProductListViewAdapter
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_market.view.*
import org.jetbrains.anko.support.v4.toast

/**
 * Created by jmnbehar on 11/5/2017.
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

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_market, container, false)
        listView = rootView.list_products
        this.inflater = inflater

        setupSwipeRefresh(rootView)

        val selectGroup = lambda@ { product: Product ->
            currentProduct = product
            (activity as MainActivity).goToChartFragment(product.currency)
        }

        listView.adapter = ProductListViewAdapter(inflater, selectGroup)
        listView.setHeightBasedOnChildren()

        return rootView
    }

    override fun onResume() {
        super.onResume()
        autoRefresh = Runnable {
            refresh({ })
            handler.postDelayed(autoRefresh, (TimeInSeconds.halfMinute * 1000).toLong())
        }
        handler.postDelayed(autoRefresh, (TimeInSeconds.halfMinute * 1000).toLong())
    }

    override fun onPause() {
        handler.removeCallbacks(autoRefresh)
        super.onPause()
    }


    override fun refresh(onComplete: () -> Unit) {
        var productsUpdated = 0
        var accountListSize = Account.list.size
        val time = TimeInSeconds.oneDay
        val onFailure = { result: Result.Failure<String, FuelError> ->  println("Error!: ${result.error}") }
        //TODO: check in about refreshing product list
        for (account in Account.list) {
            account.updateCandles(time, { _ -> toast("error!") }, { didUpdate ->
                if (didUpdate) {
                    productsUpdated++
                    if (productsUpdated == accountListSize) {
                        (listView.adapter as ProductListViewAdapter).notifyDataSetChanged()
                        onComplete()
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
                            onComplete()
                        }
                    }
                }
            })
        }
    }
}
