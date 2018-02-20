package com.jmnbehar.gdax.Fragments

import android.graphics.Color
import android.os.Bundle
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
import android.widget.Button

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

    private lateinit var timespanButtonHour: Button
    private lateinit var timespanButtonDay: Button
    private lateinit var timespanButtonWeek: Button
    private lateinit var timespanButtonMonth: Button
    private lateinit var timespanButtonYear: Button
    private lateinit var timespanButtonAll: Button

    private var chartTimeSpan = TimeInSeconds.oneDay

    companion object {
        var account: Account? = null
        fun newInstance(account: Account): ChartFragment {
            this.account = account
            return ChartFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_chart, container, false)

        showDarkMode(rootView)

        this.inflater = inflater

        nameText = rootView.txt_chart_name
        tickerText = rootView.txt_chart_ticker
        iconView = rootView.img_chart_account_icon

        balanceText = rootView.txt_chart_account_balance
        valueText = rootView.txt_chart_account_value
        priceText = rootView.txt_chart_price
        percentChangeText = rootView.txt_chart_change_or_date

        timespanButtonHour = rootView.btn_chart_timespan_hour
        timespanButtonDay = rootView.btn_chart_timespan_day
        timespanButtonWeek = rootView.btn_chart_timespan_week
        timespanButtonMonth = rootView.btn_chart_timespan_month
        timespanButtonYear = rootView.btn_chart_timespan_year
        timespanButtonAll = rootView.btn_chart_timespan_all

        val account = account
        if (account == null) {
            activity.supportFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss();
        } else {
            val prefs = Prefs(activity)

            val candles = account.product.candles
            val timeRange = TimeInSeconds.oneDay
            val currency = account.currency
            setupSwipeRefresh(rootView)

            lineChart = rootView.chart
            lineChart.configure(candles, currency, true, PriceChart.DefaultDragDirection.Horizontal,  timeRange,true) {
                swipeRefreshLayout?.isEnabled = false
                LockableScrollView.scrollLocked = true
            }
            lineChart.setOnChartValueSelectedListener(this)
            lineChart.onChartGestureListener = this

            val price = account.product.price
            priceText.text = price.fiatFormat()

            setPercentChangeText(price, candles.first().open)

            nameText.text = currency.fullName

            if (prefs.isLoggedIn) {
                tickerText.text = "$currency wallet"
                iconView.setImageResource(currency.iconId)
                balanceText.text = "${account.balance.btcFormat()} ${currency}"
                valueText.text = account.value.fiatFormat()
            } else {
                tickerText.visibility = View.GONE
                iconView.visibility = View.GONE
                balanceText.visibility = View.GONE
                valueText.visibility = View.GONE
            }

            val buyButton = rootView.btn_chart_buy
            val sellButton = rootView.btn_chart_sell

            //TODO: send over more info
            buyButton.setOnClickListener {
                if (prefs.isLoggedIn) {
                    MainActivity.goToFragment(TradeFragment.newInstance(account, TradeSide.BUY), "Trade: Buy")
                } else {
                    toast("Log in to buy or sell $currency")
                }
            }

            sellButton.setOnClickListener {
                if (prefs.isLoggedIn) {
                    MainActivity.goToFragment(TradeFragment.newInstance(account, TradeSide.SELL), "Trade: Sell")
                } else {
                    toast("Log in to buy or sell $currency")
                }
            }
            timespanButtonHour.setText("1H")
            timespanButtonHour.setOnClickListener {
                setChartTimespan(TimeInSeconds.oneHour)
            }
            timespanButtonDay.setText("1D")
            timespanButtonDay.setOnClickListener {
                setChartTimespan(TimeInSeconds.oneDay)
            }
            timespanButtonWeek.setText("1W")
            timespanButtonWeek.setOnClickListener {
                setChartTimespan(TimeInSeconds.oneWeek)
            }
            timespanButtonMonth.setText("1M")
            timespanButtonMonth.setOnClickListener {
                setChartTimespan(TimeInSeconds.oneMonth)
            }
            timespanButtonYear.setText("1Y")
            timespanButtonYear.setOnClickListener {
                setChartTimespan(TimeInSeconds.oneYear)
            }
            timespanButtonAll.setText("ALL")
            timespanButtonAll.setOnClickListener {
                //TODO: fix this
                setChartTimespan(currency.lifetimeInSeconds)
            }

            val stashedFills = prefs.getStashedFills(account.product.id)
            val stashedOrders = prefs.getStashedOrders(account.product.id)
            historyList = rootView.list_history
            historyList.adapter = HistoryListViewAdapter(inflater, prefs.isLoggedIn, stashedOrders, stashedFills,
                    { order -> orderOnClick(order)}, { fill -> fillOnClick(fill) })
            historyList.setHeightBasedOnChildren()

            if (prefs.isLoggedIn) {
                val onFailure = { result: Result.Failure<String, FuelError> ->  println("Error!: ${result.error}") }
                GdaxApi.listOrders(productId = account.product.id).executeRequest(onFailure) { orderResult ->
                    prefs.stashOrders(orderResult.value)
                    val gson = Gson()
                    val apiOrderList: List<ApiOrder> = gson.fromJson(orderResult.value, object : TypeToken<List<ApiOrder>>() {}.type)
                    val filteredOrders = apiOrderList.filter { it.product_id == account.product.id }
                    //TODO: instead of filtering these, fix the api requests, this shit is wasteful
                    GdaxApi.fills(productId = account.product.id).executeRequest(onFailure) { fillResult ->
                        prefs.stashFills(fillResult.value)
                        val apiFillList: List<ApiFill> = gson.fromJson(fillResult.value, object : TypeToken<List<ApiFill>>() {}.type)
                        val filteredFills = apiFillList.filter { it.product_id == account.product.id }
                        historyList.adapter = HistoryListViewAdapter(inflater, true, filteredOrders, filteredFills,
                                { order -> orderOnClick(order)}, { fill -> fillOnClick(fill) })
                        historyList.setHeightBasedOnChildren()
                    }
                }
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

    private fun setChartTimespan(timespan: Long) {
        chartTimeSpan = timespan
        MainActivity.progressDialog?.show()
        miniRefresh({
            toast("Error updating chart time")
            MainActivity.progressDialog?.dismiss()
        }, {
            MainActivity.progressDialog?.dismiss()
        })
    }
    private fun setPercentChangeText(price: Double, open: Double) {
        val change = price - open
        val weightedChange: Double = (change / open)
        val percentChange: Double = weightedChange * 100.0
        percentChangeText.text = percentChange.percentFormat()
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
                    var orders = (historyList.adapter as HistoryListViewAdapter).orders
                    orders = orders.filter { o -> o.id != order.id }
                    (historyList.adapter as HistoryListViewAdapter).orders = orders
                    (historyList.adapter as HistoryListViewAdapter).notifyDataSetChanged()
                    toast("order cancelled")
                }
            }
        }.show()
    }

    private fun fillOnClick(fill: ApiFill) {
        alert {
            title = "Fill"

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
        account?. let { account ->
            val candle = account.product.candles[entry.x.toInt()]
            percentChangeText.text = candle.time.toStringWithTimeRange(chartTimeSpan)
            percentChangeText.textColor = Color.BLACK
        }
    }

    override fun onNothingSelected() {
        val account = account
        if (account != null) {
            val price = account.product.price
            val open = account.product.candles.first().open
            priceText.text = price.fiatFormat()
            setPercentChangeText(price, open)
            lineChart.highlightValues(arrayOf<Highlight>())
        }
    }

    override fun onChartGestureStart(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) { }
    override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {
        swipeRefreshLayout?.isEnabled = true
        LockableScrollView.scrollLocked = false
        onNothingSelected()
    }
    override fun onChartLongPressed(me: MotionEvent) {
        swipeRefreshLayout?.isEnabled = false
        LockableScrollView.scrollLocked = true
    }
    override fun onChartDoubleTapped(me: MotionEvent) { }
    override fun onChartSingleTapped(me: MotionEvent) { }
    override fun onChartFling(me1: MotionEvent, me2: MotionEvent, velocityX: Float, velocityY: Float) { }
    override fun onChartScale(me: MotionEvent, scaleX: Float, scaleY: Float) { }
    override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) { }

    override fun refresh(onComplete: () -> Unit) {
        val gson = Gson()
        val onFailure = { result: Result.Failure<String, FuelError> ->
            toast("Error!: ${result.error}")
            println("error!" )}
        val prefs = Prefs(context)
        if (prefs.isLoggedIn) {

        }
        account?. let { account ->
            if (prefs.isLoggedIn) {
                GdaxApi.account(account.id).executeRequest(onFailure) { result ->
                    val apiAccount: ApiAccount = gson.fromJson(result.value, object : TypeToken<ApiAccount>() {}.type)
                    val newBalance = apiAccount.balance.toDoubleOrZero()
                    balanceText.text = account.balance.fiatFormat()
                    valueText.text = account.value.fiatFormat()
                    miniRefresh(onFailure) {
                        account.updateAccount(newBalance, account.product.price)
                        onComplete()
                    }
                }

                var filteredOrders: List<ApiOrder>? = null
                var filteredFills: List<ApiFill>? = null
                GdaxApi.listOrders(productId = account.product.id).executeRequest(onFailure) { result ->
                    prefs.stashOrders(result.value)
                    val apiOrderList: List<ApiOrder> = gson.fromJson(result.value, object : TypeToken<List<ApiOrder>>() {}.type)
                    filteredOrders = apiOrderList.filter { it.product_id == account.product.id }
                    if (filteredOrders != null && filteredFills != null) {
                        updateHistoryListAdapter(filteredOrders!!, filteredFills!!)
                    }
                }
                GdaxApi.fills(productId = account.product.id).executeRequest(onFailure) { result ->
                    prefs.stashFills(result.value)
                    val apiFillList: List<ApiFill> = gson.fromJson(result.value, object : TypeToken<List<ApiFill>>() {}.type)
                    filteredFills = apiFillList.filter { it.product_id == account.product.id }
                    if (filteredOrders != null && filteredFills != null) {
                        updateHistoryListAdapter(filteredOrders!!, filteredFills!!)
                    }
                }
            } else {
                miniRefresh(onFailure) {
                    account.updateAccount(0.0, account.product?.price)
                    onComplete()
                }
            }
        }
    }

    private fun updateHistoryListAdapter(orderList: List<ApiOrder>, fillList: List<ApiFill>) {
        (historyList.adapter as HistoryListViewAdapter).orders = orderList
        (historyList.adapter as HistoryListViewAdapter).fills  = fillList
        (historyList.adapter as HistoryListViewAdapter).notifyDataSetChanged()
        historyList.setHeightBasedOnChildren()
    }

    private fun miniRefresh(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        account?. let { account ->
            account.updateCandles(chartTimeSpan, onFailure, { _ ->
                GdaxApi.ticker(account.product.id).executeRequest(onFailure) { result ->
                    val ticker: ApiTicker = Gson().fromJson(result.value, object : TypeToken<ApiTicker>() {}.type)
                    val price = ticker.price.toDoubleOrNull()
                    if (price != null) {
                        account.product.price = price
                    }

                    priceText.text = account.product.price.fiatFormat()
                    valueText.text = account.value.fiatFormat()

                    setPercentChangeText(account.product.price, account.product.candles.first().open)

                    lineChart.addCandles(account.product.candles, account.currency, chartTimeSpan)
                    onComplete()
                }
            })
        }
    }
}
