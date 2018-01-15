package com.jmnbehar.gdax.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import com.github.kittinunf.result.Result
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jmnbehar.gdax.Activities.MainActivity
import com.jmnbehar.gdax.Adapters.HistoryListViewAdapter
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_chart.view.*
import org.jetbrains.anko.support.v4.toast
import android.view.MotionEvent
import com.github.mikephil.charting.listener.ChartTouchListener


/**
 * Created by jmnbehar on 11/5/2017.
 */
class ChartFragment : RefreshFragment(), OnChartValueSelectedListener, OnChartGestureListener {
    private lateinit var inflater: LayoutInflater

    private lateinit var historyList: ListView

    private lateinit var priceText: TextView

    private lateinit var balanceText: TextView
    private lateinit var valueText: TextView
    private lateinit var lineChart: LineChart
    private var chartLength = TimeInSeconds.oneDay

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
        lineChart = rootView.chart
        lineChart.configure(candles, account.currency, true, true)
        lineChart.setOnChartValueSelectedListener(this)
        lineChart.onChartGestureListener = this

        rootView.txt_chart_name.text = account.currency.toString()
        rootView.txt_chart_ticker.text = account.currency.toString()

        balanceText = rootView.txt_chart_account_balance
        valueText = rootView.txt_chart_account_value
        priceText = rootView.txt_chart_price

        priceText.text = "${account.product.price}"
        balanceText.text = "${account.balance}"
        valueText.text = "${account.value}"

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

        GdaxApi.listOrders(productId = account.product.id).executeRequest { result ->
            when (result) {
                is Result.Failure -> println("Error!: ${result.error}")
                is Result.Success -> {
                    val gson = Gson()
                    val apiOrderList: List<ApiOrder> = gson.fromJson(result.value, object : TypeToken<List<ApiOrder>>() {}.type)
                    val filteredOrders = apiOrderList.filter { it.product_id == account.product.id }
                    //TODO: instead of filtering these, fix the api requests, this shit is wasteful
                    GdaxApi.fills(productId = account.product.id).executeRequest { result ->
                        when (result) {
                            is Result.Failure -> println("Error!: ${result.error}")
                            is Result.Success -> {
                                val apiFillList: List<ApiFill> = gson.fromJson(result.value, object : TypeToken<List<ApiFill>>() {}.type)
                                val filteredFills = apiFillList.filter { it.product_id == account.product.id }
                                historyList.adapter = HistoryListViewAdapter(inflater, filteredOrders, filteredFills, { })
                                historyList.setHeightBasedOnChildren()
                            }
                        }
                    }
                }
            }
        }


        return rootView
    }

    override fun onValueSelected(entry: Entry, h: Highlight) {
        priceText.text = entry.y.toString()
    }

    override fun onNothingSelected() {
        priceText.text = account.product.price.fiatFormat()
        lineChart.highlightValues(arrayOf<Highlight>())
    }

    override fun onChartGestureStart(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {
        toast("gesture started")
    }
    override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {
        onNothingSelected()
    }
    override fun onChartLongPressed(me: MotionEvent) { }
    override fun onChartDoubleTapped(me: MotionEvent) { }
    override fun onChartSingleTapped(me: MotionEvent) { }
    override fun onChartFling(me1: MotionEvent, me2: MotionEvent, velocityX: Float, velocityY: Float) { }
    override fun onChartScale(me: MotionEvent, scaleX: Float, scaleY: Float) { }
    override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) { }

    override fun refresh(onComplete: () -> Unit) {
        GdaxApi.listOrders(productId = account.product.id).executeRequest { result ->
            when (result) {
                is Result.Failure -> {
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
                                println("Error!: ${result.error}")
                            }
                            is Result.Success -> {
                                val apiFillList: List<ApiFill> = gson.fromJson(result.value, object : TypeToken<List<ApiFill>>() {}.type)
                                val filteredFills = apiFillList.filter { it.product_id == account.product.id }
                                Candle.getCandles(account.product.id, chartLength, { candleList ->
                                    account.product.candles = candleList
                                    GdaxApi.account(account.id).executeRequest {  result ->
                                        when (result) {
                                            is Result.Failure -> {
                                                println("Error!: ${result.error}")
                                            }
                                            is Result.Success -> {
                                                val apiAccount: ApiAccount = gson.fromJson(result.value, object : TypeToken<ApiAccount>() {}.type)
                                                val newBalance = apiAccount.balance.toDoubleOrZero()

                                                GdaxApi.ticker(account.product.id).executeRequest { result ->
                                                    when (result) {
                                                        is Result.Failure -> {
                                                            toast("Error!: ${result.error}")
                                                        }
                                                        is Result.Success -> {
                                                            val ticker: ApiTicker = gson.fromJson(result.value, object : TypeToken<ApiTicker>() {}.type)
                                                            val newPrice = ticker.price.toDoubleOrZero()

                                                            account.updateAccount(newBalance, newPrice)
                                                            historyList.adapter = HistoryListViewAdapter(inflater, filteredOrders, filteredFills, { })
                                                            historyList.setHeightBasedOnChildren()

                                                            priceText.text = "${account.product.price}"

                                                            balanceText.text = "${account.balance}"
                                                            valueText.text = "${account.value}"

                                                            onComplete()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}
