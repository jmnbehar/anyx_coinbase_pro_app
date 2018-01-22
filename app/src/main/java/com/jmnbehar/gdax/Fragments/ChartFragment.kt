package com.jmnbehar.gdax.Fragments

import android.os.Bundle
import android.view.*
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
import android.widget.Button
import android.widget.LinearLayout
import com.github.kittinunf.fuel.core.FuelError
import com.github.mikephil.charting.listener.ChartTouchListener
import org.jetbrains.anko.*
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast


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
        //TODO: investigate autoscroll
        //TODO: add autorefresh

        val candles = account.product.candles
        val timeRange = TimeInSeconds.oneDay
        val currency = account.currency

        lineChart = rootView.chart
        lineChart.configure(candles, currency, true, timeRange, true)
        lineChart.setOnChartValueSelectedListener(this)
        lineChart.onChartGestureListener = this

        rootView.txt_chart_name.text = currency.fullName
        rootView.txt_chart_ticker.text = "$currency wallet"
        rootView.img_chart_account_icon.setImageResource(currency.iconId)

        balanceText = rootView.txt_chart_account_balance
        valueText = rootView.txt_chart_account_value
        priceText = rootView.txt_chart_price

        priceText.text = "${account.product.price}"

        balanceText.text = "${account.balance.btcFormat()} ${currency}"
        valueText.text = "$${account.value.fiatFormat()}"

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

        val onFailure = { result: Result.Failure<String, FuelError> ->  println("Error!: ${result.error}") }
        GdaxApi.listOrders(productId = account.product.id).executeRequest(onFailure) { result ->
            val gson = Gson()
            val apiOrderList: List<ApiOrder> = gson.fromJson(result.value, object : TypeToken<List<ApiOrder>>() {}.type)
            val filteredOrders = apiOrderList.filter { it.product_id == account.product.id }
            //TODO: instead of filtering these, fix the api requests, this shit is wasteful
            GdaxApi.fills(productId = account.product.id).executeRequest(onFailure) { result ->
                val apiFillList: List<ApiFill> = gson.fromJson(result.value, object : TypeToken<List<ApiFill>>() {}.type)
                val filteredFills = apiFillList.filter { it.product_id == account.product.id }
                historyList.adapter = HistoryListViewAdapter(inflater, filteredOrders, filteredFills,
                        { order -> orderOnClick(order)}, { fill -> fillOnClick(fill) })
                historyList.setHeightBasedOnChildren()
            }
        }

        return rootView
    }

    fun orderOnClick(order: ApiOrder) {
        alert {
            title = "Order"

            customView {
                linearLayout {
                    verticalLayout {
                        horizontalLayout("Side:  ", order.side).lparams(width = matchParent) {}
                        horizontalLayout("Size:  ", order.size ?: "0").lparams(width = matchParent) {}
                        horizontalLayout("Filled Size:  ", order.filled_size).lparams(width = matchParent) {}
                        horizontalLayout("Price:  ", order.price).lparams(width = matchParent) {}
                        horizontalLayout("Status:  ", order.status).lparams(width = matchParent) {}
                        horizontalLayout("Fill fees:  ", order.fill_fees).lparams(width = matchParent) {}
                        horizontalLayout("Time:  ", order.created_at).lparams(width = matchParent) {}
                    }.lparams(width = matchParent) {leftMargin = dip(10) }
                }
            }
            positiveButton("Close") {  }
            negativeButton("Delete") {
                GdaxApi.cancelOrder(order.id).executeRequest({ }) {
                    var orders = (historyList.adapter as HistoryListViewAdapter).orders.toMutableList()
                    orders.removeIf { o -> o.id == order.id }
                    (historyList.adapter as HistoryListViewAdapter).orders = orders
                    (historyList.adapter as HistoryListViewAdapter).notifyDataSetChanged()
                    toast("order cancelled")
                }
            }
        }.show()
    }

    fun fillOnClick(fill: ApiFill) {
        alert {
            title = "Order"

            customView {
                linearLayout {
                    verticalLayout {
                        horizontalLayout("Side:  ", fill.side).lparams(width = matchParent) {}
                        horizontalLayout("Size:  ", fill.size).lparams(width = matchParent) {}
                        horizontalLayout("Price:  ", fill.price).lparams(width = matchParent) {}
                        horizontalLayout("Fee:  ", fill.fee).lparams(width = matchParent) {}
                        horizontalLayout("Time:  ", fill.created_at).lparams(width = matchParent) {}
                    }.lparams(width = matchParent) {leftMargin = dip(10) }
                }
            }
            positiveButton("Close") {  }
        }.show()
    }

    override fun onValueSelected(entry: Entry, h: Highlight) {
        priceText.text = entry.y.toDouble().fiatFormat()
    }

    override fun onNothingSelected() {
        priceText.text = account.product.price.fiatFormat()
        lineChart.highlightValues(arrayOf<Highlight>())
    }

    override fun onChartGestureStart(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {
        //TODO: don't lock if its a swipe down
        MainActivity.swipeRefreshLayout.isEnabled = false
        LockableScrollView.scrollLocked = true
    }
    override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {
        MainActivity.swipeRefreshLayout.isEnabled = true
        LockableScrollView.scrollLocked = false
        onNothingSelected()
    }
    override fun onChartLongPressed(me: MotionEvent) { }
    override fun onChartDoubleTapped(me: MotionEvent) { }
    override fun onChartSingleTapped(me: MotionEvent) { }
    override fun onChartFling(me1: MotionEvent, me2: MotionEvent, velocityX: Float, velocityY: Float) { }
    override fun onChartScale(me: MotionEvent, scaleX: Float, scaleY: Float) { }
    override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) { }

    override fun refresh(onComplete: () -> Unit) {
        val onFailure = { result: Result.Failure<String, FuelError> ->  println("Error!: ${result.error}") }
        GdaxApi.listOrders(productId = account.product.id).executeRequest(onFailure) { result ->
    val gson = Gson()

    val apiOrderList: List<ApiOrder> = gson.fromJson(result.value, object : TypeToken<List<ApiOrder>>() {}.type)
    val filteredOrders = apiOrderList.filter { it.product_id == account.product.id }
    //TODO: instead of filtering these, fix the api requests, this shit is wasteful

    GdaxApi.fills(productId = account.product.id).executeRequest(onFailure) { result ->
                val apiFillList: List<ApiFill> = gson.fromJson(result.value, object : TypeToken<List<ApiFill>>() {}.type)
                val filteredFills = apiFillList.filter { it.product_id == account.product.id }
                val onFailure = { result: Result.Failure<String, FuelError> ->  println("Error!: ${result.error}") }

                Candle.getCandles(account.product.id, chartLength, { candleList ->
                    account.product.candles = candleList
                    GdaxApi.account(account.id).executeRequest(onFailure) {  result ->
                        val apiAccount: ApiAccount = gson.fromJson(result.value, object : TypeToken<ApiAccount>() {}.type)
                        val newBalance = apiAccount.balance.toDoubleOrZero()

                        GdaxApi.ticker(account.product.id).executeRequest(onFailure) { result ->
                            val ticker: ApiTicker = gson.fromJson(result.value, object : TypeToken<ApiTicker>() {}.type)
                            val newPrice = ticker.price.toDoubleOrZero()

                            account.updateAccount(newBalance, newPrice)
                            historyList.adapter = HistoryListViewAdapter(inflater, filteredOrders, filteredFills,
                                    { order -> orderOnClick(order)}, { fill -> fillOnClick(fill) })
                            historyList.setHeightBasedOnChildren()

                            priceText.text = account.product.price.fiatFormat()

                            balanceText.text = account.balance.fiatFormat()
                            valueText.text = account.value.fiatFormat()
//                                                            val now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
//                                                            val newCandle = Candle(now.toDouble(), newPrice, newPrice, newPrice, newPrice, 0.0)
//                                                            val mutableCandles = candleList.toMutableList()
//                                                            mutableCandles.add(newCandle)

                            lineChart.addCandles(candleList, account.currency, TimeInSeconds.oneDay)

                            onComplete()
                        }
                    }
                })
            }
        }
    }
}
