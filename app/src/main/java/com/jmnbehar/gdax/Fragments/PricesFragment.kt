package com.jmnbehar.gdax.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jmnbehar.gdax.Activities.MainActivity
import com.jmnbehar.gdax.Adapters.ProductListViewAdapter
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_home.view.*
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Created by jmnbehar on 11/5/2017.
 */
class PricesFragment : RefreshFragment() {
    private var currentProduct: Product? = null
    lateinit var listView: ListView

    lateinit var totalValueText: TextView
    lateinit var totalValueLabelText: TextView

    lateinit var inflater: LayoutInflater

    companion object {
        fun newInstance(): PricesFragment
        {
            return PricesFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_home, container, false)

        listView = rootView.list_products
        this.inflater = inflater
        totalValueText = rootView.txt_home_total_value
        totalValueLabelText = rootView.txt_home_total_value_label

        val selectGroup = lambda@ { product: Product ->
            currentProduct = product
            var equivalentMenuItem = when(product.currency) {
                Currency.BTC -> R.id.nav_btc
                Currency.ETH -> R.id.nav_eth
                Currency.LTC -> R.id.nav_ltc
                Currency.BCH -> R.id.nav_bch
                Currency.USD -> R.id.nav_home
            }
            MainActivity.goToNavigationId(equivalentMenuItem, activity)
        }

        totalValueText.text = "$${Account.totalBalance.fiatFormat()}"
        totalValueLabelText.text = "Account total value"

        listView.adapter = ProductListViewAdapter(inflater, selectGroup )
        listView.setHeightBasedOnChildren()

        return rootView
    }

    override fun refresh(onComplete: () -> Unit) {
        var productsUpdated = 0
        var productListSize = Product.listSize
        val time = TimeInSeconds.oneDay
        for (account in Account.list) {
            if (account.product.lastCandleUpdateTime.isBefore(LocalDateTime.now().minusMinutes(2))) {
                Candle.getCandles(account.product.id, time, { candleList ->
                    account.product.lastCandleUpdateTime = LocalDateTime.now()
                    productsUpdated++
                    account.product.candles = candleList
                    if (productsUpdated == productListSize) {
                        (listView.adapter as ProductListViewAdapter).notifyDataSetChanged()
                        onComplete()
                    }
                })
            } else {
                GdaxApi.ticker(account.product.id).executeRequest { result ->
                    when (result) {
                        is Result.Failure -> {
                            toast("Error!: ${result.error}")
                        }
                        is Result.Success -> {
                            val ticker: ApiTicker = Gson().fromJson(result.value, object : TypeToken<ApiTicker>() {}.type)
                            val price = ticker.price.toDoubleOrNull()
                            if (price != null) {
                                account.product.price = price
//                                val now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
//                                val newCandle = Candle(now.toDouble(), price, price, price, price, 0.0)
//                                val mutableCandles = account.product.candles.toMutableList()
//                                mutableCandles.add(newCandle)
//                                account.product.candles = mutableCandles
                            }
                            productsUpdated++
                            if (productsUpdated == productListSize) {
                                (listView.adapter as ProductListViewAdapter).notifyDataSetChanged()
                                onComplete()
                            }
                        }
                    }
                }
            }
        }
    }

}
