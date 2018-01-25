package com.jmnbehar.gdax.Fragments

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import com.github.kittinunf.result.Result
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
import com.github.kittinunf.fuel.core.FuelError
import com.github.mikephil.charting.listener.ChartTouchListener
import org.jetbrains.anko.*
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast
import android.view.MotionEvent
import android.widget.HorizontalScrollView

/**
 * Created by jmnbehar on 11/5/2017.
 */
class ChartFragment : RefreshFragment(), OnChartValueSelectedListener, OnChartGestureListener {
    private lateinit var inflater: LayoutInflater

    private lateinit var historyList: ListView

    private lateinit var priceText: TextView

    private lateinit var balanceText: TextView
    private lateinit var valueText: TextView
    private lateinit var lineChart: PriceChart

    private lateinit var nameText: TextView
    private lateinit var tickerText: TextView
    private lateinit var iconView: ImageView
    private lateinit var percentChangeText: TextView

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
        lineChart.configure(candles, currency, true, PriceChart.DefaultDragDirection.Horizontal,  timeRange,true) {
            MainActivity.swipeRefreshLayout.isEnabled = false
            LockableScrollView.scrollLocked = true

        }
        lineChart.setOnChartValueSelectedListener(this)
        lineChart.onChartGestureListener = this

        nameText = rootView.txt_chart_name
        tickerText = rootView.txt_chart_ticker
        iconView = rootView.img_chart_account_icon

        balanceText = rootView.txt_chart_account_balance
        valueText = rootView.txt_chart_account_value
        priceText = rootView.txt_chart_price
        percentChangeText = rootView.txt_chart_change_or_date

        val price = account.product.price
        priceText.text = price.fiatFormat()

        setPercentChangeText(price, candles.first().open)

        nameText.text = currency.fullName
        tickerText.text = "$currency wallet"
        iconView.setImageResource(currency.iconId)

        balanceText.text = "${account.balance.btcFormat()} ${currency}"
        valueText.text = "$${account.value.fiatFormat()}"

        val buyButton = rootView.btn_chart_buy
        val sellButton = rootView.btn_chart_sell

        //TODO: send over more info
        buyButton.setOnClickListener {
            MainActivity.goToFragment(TradeFragment.newInstance(account, TradeSide.BUY), "Trade: Buy")
        }

        sellButton.setOnClickListener {
            MainActivity.goToFragment(TradeFragment.newInstance(account, TradeSide.SELL), "Trade: Sell")
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

    override fun onResume() {
        super.onResume()
        autoRefresh = Runnable {
            miniRefresh({ }, { })
            handler.postDelayed(autoRefresh, (TimeInSeconds.halfMinute * 1000).toLong())
        }
        handler.postDelayed(autoRefresh, (TimeInSeconds.halfMinute * 1000).toLong())
    }

    override fun onPause() {
        handler.removeCallbacks(autoRefresh)
        super.onPause()
    }

    private fun setPercentChangeText(price: Double, open: Double) {
        val change = price - open
        val weightedChange: Double = (change / open)
        val percentChange: Double = weightedChange * 100.0
        percentChangeText.text = percentChange.fiatFormat() + "%"
        percentChangeText.textColor = if (percentChange >= 0) {
            Color.GREEN
        } else {
            Color.RED
        }
    }

    private fun orderOnClick(order: ApiOrder) {
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

    private fun fillOnClick(fill: ApiFill) {
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
        val candle = account.product.candles[entry.x.toInt()]
        percentChangeText.text = candle.time.toStringWithTimeRange(TimeInSeconds.oneDay)
        percentChangeText.textColor = Color.BLACK
    }

    override fun onNothingSelected() {
        val price = account.product.price
        val open = account.product.candles.first().open
        priceText.text = price.fiatFormat()
        setPercentChangeText(price, open)
        lineChart.highlightValues(arrayOf<Highlight>())
    }

    override fun onChartGestureStart(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) { }
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


                GdaxApi.account(account.id).executeRequest(onFailure) { result ->
                    val apiAccount: ApiAccount = gson.fromJson(result.value, object : TypeToken<ApiAccount>() {}.type)
                    val newBalance = apiAccount.balance.toDoubleOrZero()

                    miniRefresh(onFailure) {
                        account.updateAccount(newBalance, account.product.price)
                        historyList.adapter = HistoryListViewAdapter(inflater, filteredOrders, filteredFills,
                                { order -> orderOnClick(order)}, { fill -> fillOnClick(fill) })
                        onComplete()
                    }
                }
            }
        }
    }

    private fun miniRefresh(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        val time = TimeInSeconds.oneDay
        account.updateCandles(time) { _ ->
            GdaxApi.ticker(account.product.id).executeRequest(onFailure) { result ->
                val ticker: ApiTicker = Gson().fromJson(result.value, object : TypeToken<ApiTicker>() {}.type)
                val price = ticker.price.toDoubleOrNull()
                if (price != null) {
                    account.product.price = price
                }
                historyList.setHeightBasedOnChildren()

                priceText.text = account.product.price.fiatFormat()

                balanceText.text = account.balance.fiatFormat()
                valueText.text = account.value.fiatFormat()

                lineChart.addCandles(account.product.candles, account.currency, TimeInSeconds.oneDay)

                onComplete()
            }
        }
    }
}
