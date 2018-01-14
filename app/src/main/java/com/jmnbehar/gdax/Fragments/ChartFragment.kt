package com.jmnbehar.gdax.Fragments

import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.github.kittinunf.result.Result
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jmnbehar.gdax.Activities.MainActivity
import com.jmnbehar.gdax.Adapters.HistoryListViewAdapter
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_chart.view.*

/**
 * Created by jmnbehar on 11/5/2017.
 */
class ChartFragment : RefreshFragment() {
    private lateinit var inflater: LayoutInflater

    private lateinit var historyList: ListView

    companion object {
        lateinit var account: Account
        fun newInstance(account: Account): ChartFragment {
            this.account = account
            return ChartFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_chart, container, false)

        this.inflater = inflater

        val candles = account.product.candles
        var lineChart = rootView.chart
        lineChart.addCandles(candles)

        rootView.txt_chart_name.text = account.currency.toString()
        rootView.txt_chart_ticker.text = account.currency.toString()
        rootView.txt_chart_account_balance.text = "${account.balance}"
        rootView.txt_chart_account_value.text = "${account.value}"

        val buyButton = rootView.btn_chart_buy
        val sellButton = rootView.btn_chart_sell

        //TODO: send over more info
        buyButton.setOnClickListener {
            MainActivity.goToFragment(TradeFragment.newInstance(account, TradeType.BUY), "Trade: Buy")
        }

        sellButton.setOnClickListener {
            MainActivity.goToFragment(TradeFragment.newInstance(account, TradeType.SELL), "Trade: Sell")
        }
        historyList = rootView.list_history

        refresh {  }

        return rootView
    }

    override fun refresh(onComplete: () -> Unit) {
        GdaxApi.listOrders(productId = account.product.id).executeRequest { result ->
            when (result) {
                is Result.Failure -> {
                    //error
                    println("Error!: ${result.error}")
                }
                is Result.Success -> {
                    val gson = Gson()

                    val apiOrderList: List<ApiOrder> = gson.fromJson(result.value, object : TypeToken<List<ApiOrder>>() {}.type)
                    val filteredOrders = apiOrderList.filter { it.product_id == account.product.id }
                    //TODO: instead of filtering these, fix the api requests, this shit is wasteful

                    GdaxApi.fills(productId = account.product.id).executeRequest { result ->
                        when (result) {
                            is Result.Failure -> {
                                //error
                                println("Error!: ${result.error}")
                            }
                            is Result.Success -> {
                                val apiFillList: List<ApiFill> = gson.fromJson(result.value, object : TypeToken<List<ApiFill>>() {}.type)
                                val filteredFills = apiFillList.filter { it.product_id == account.product.id }
                                historyList.adapter = HistoryListViewAdapter(inflater, filteredOrders, filteredFills, { })
                                historyList.setHeightBasedOnChildren()
                                onComplete()
                            }
                        }
                    }
                }
            }
        }
    }
}
