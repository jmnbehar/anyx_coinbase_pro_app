package com.anyexchange.anyx.fragments.main

import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.view.*
import com.github.kittinunf.result.Result
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.fragment_chart.view.*
import com.github.kittinunf.fuel.core.FuelError
import com.github.mikephil.charting.listener.ChartTouchListener
import org.jetbrains.anko.*
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast
import android.view.MotionEvent
import android.widget.*
import com.anyexchange.anyx.adapters.HistoryPagerAdapter
import kotlinx.android.synthetic.main.fragment_accounts.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by anyexchange on 11/5/2017.
 */
class ChartFragment : RefreshFragment(), OnChartValueSelectedListener, OnChartGestureListener, View.OnTouchListener {
    private lateinit var inflater: LayoutInflater

    private lateinit var historyPager: ViewPager

    private lateinit var priceText: TextView

    private lateinit var balanceText: TextView
    private lateinit var valueText: TextView
    private lateinit var lineChart: PriceChart

    private lateinit var nameText: TextView
    private lateinit var tickerText: TextView
    private lateinit var iconView: ImageView
    private lateinit var percentChangeText: TextView

    private lateinit var timespanButtonHour: RadioButton
    private lateinit var timespanButtonDay: RadioButton
    private lateinit var timespanButtonWeek: RadioButton
    private lateinit var timespanButtonMonth: RadioButton
    private lateinit var timespanButtonYear: RadioButton
//    private lateinit var timespanButtonAll: RadioButton

    private lateinit var historyTabList: TabLayout

    private lateinit var buyButton: Button
    private lateinit var sellButton: Button

    private var chartTimeSpan = Timespan.DAY
    private var candles = listOf<Candle>()

    private var tradeFragment: TradeFragment? = null

    companion object {
        var account: Account? = null
        fun newInstance(account: Account): ChartFragment {
            Companion.account = account
            return ChartFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_chart, container, false)

        showDarkMode(rootView)

        this.inflater = inflater

        nameText = rootView.txt_chart_name
        tickerText = rootView.txt_chart_ticker
        iconView = rootView.img_chart_account_icon

        balanceText = rootView.txt_chart_account_balance
        valueText = rootView.txt_chart_account_value
        priceText = rootView.txt_chart_price
        percentChangeText = rootView.txt_chart_change_or_date

        timespanButtonHour = rootView.rbtn_chart_timespan_hour
        timespanButtonDay = rootView.rbtn_chart_timespan_day
        timespanButtonWeek = rootView.rbtn_chart_timespan_week
        timespanButtonMonth = rootView.rbtn_chart_timespan_month
        timespanButtonYear = rootView.rbtn_chart_timespan_year
//        timespanButtonAll = rootView.rbtn_chart_timespan_all

        setupSwipeRefresh(rootView)

        val account = account
        val activity = activity!!
        if (account == null) {
            activity.supportFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
        } else {
            val prefs = Prefs(activity)

            checkTimespanButton()
            candles = account.product.candlesForTimespan(chartTimeSpan)
            val currency = account.currency

            historyPager = rootView.history_view_pager
            lineChart = rootView.chart
            lineChart.configure(candles, currency, true, PriceChart.DefaultDragDirection.Horizontal,  chartTimeSpan) {
                swipeRefreshLayout?.isEnabled = false
            }
            lineChart.setOnChartValueSelectedListener(this)
            lineChart.onChartGestureListener = this

            buyButton = rootView.btn_chart_buy
            sellButton = rootView.btn_chart_sell

            //TODO: send over more info
            buyButton.setOnClickListener {
                if (!prefs.isLoggedIn) {
                    toast("Please log in")
                } else if (GdaxApi.credentials?.isValidated == null) { //(GdaxApi.credentials?.isValidated == null) {
                    toast("Please validate your account in Settings")
                } else if (GdaxApi.credentials?.isValidated == false) { // (GdaxApi.credentials?.isValidated == false) {
                    toast("Please use an API Key with all required permissions.")
                } else {
                    if (tradeFragment == null) {
                        tradeFragment = TradeFragment.newInstance(account, TradeSide.BUY)
                    } else {
                        tradeFragment?.tradeSide = TradeSide.BUY
                    }
                    (activity as com.anyexchange.anyx.activities.MainActivity).goToFragment(tradeFragment!!, "Trade: Buy")
                }
            }

            sellButton.setOnClickListener {
                if (!prefs.isLoggedIn) {
                    toast("Please log in")
                } else if (GdaxApi.credentials?.isValidated == null) { //(GdaxApi.credentials?.isValidated == null) {
                    toast("Please validate your account in Settings")
                } else if (GdaxApi.credentials?.isValidated == false) { // (GdaxApi.credentials?.isValidated == false) {
                    toast("Please use an API Key with all required permissions")
                } else {
                    if (tradeFragment == null) {
                        tradeFragment = TradeFragment.newInstance(account, TradeSide.SELL)
                    } else {
                        tradeFragment?.tradeSide = TradeSide.SELL
                    }
                    (activity as com.anyexchange.anyx.activities.MainActivity).goToFragment(tradeFragment!!, "Trade: Sell")
                }
            }

            timespanButtonHour.text = "1H"
            timespanButtonHour.setOnClickListener {
                setChartTimespan(Timespan.HOUR)
            }
            timespanButtonDay.text = "1D"
            timespanButtonDay.setOnClickListener {
                setChartTimespan(Timespan.DAY)
            }
            timespanButtonWeek.text = "1W"
            timespanButtonWeek.setOnClickListener {
                setChartTimespan(Timespan.WEEK)
            }
            timespanButtonMonth.text = "1M"
            timespanButtonMonth.setOnClickListener {
                setChartTimespan(Timespan.MONTH)
            }
            timespanButtonYear.text = "1Y"
            timespanButtonYear.setOnClickListener {
                setChartTimespan(Timespan.YEAR)
            }
//            timespanButtonAll.setText("ALL")
//            timespanButtonAll.setOnClickListener {
//                setChartTimespan(Timespan.ALL)
//            }

            val stashedFills = prefs.getStashedFills(account.product.id)
            val stashedOrders = prefs.getStashedOrders(account.product.id)
            historyPager.adapter = HistoryPagerAdapter(childFragmentManager, stashedOrders, stashedFills,
                    { order -> orderOnClick(order)}, { fill -> fillOnClick(fill) })
            historyPager.setOnTouchListener(this)
            historyTabList = rootView.history_tab_layout
        }
        return rootView
    }


    private fun switchAccount(account: Account) {
        Companion.account = account
        val activity = activity as com.anyexchange.anyx.activities.MainActivity
        val currency = account.currency
        val price = account.product.price
        priceText.text = price.fiatFormat()

        candles = account.product.candlesForTimespan(chartTimeSpan)

        val prefs = Prefs(activity)
        val stashedFills = prefs.getStashedFills(account.product.id)
        val stashedOrders = prefs.getStashedOrders(account.product.id)
        //TODO: set orders/fills

        val now = Calendar.getInstance()
        val lastCandleTime = candles.lastOrNull()?.time?.toLong() ?: 0
        val nextCandleTime: Long = lastCandleTime + Candle.granularityForTimespan(chartTimeSpan)
        val areCandlesUpToDate = candles.isNotEmpty() && (nextCandleTime > now.timeInSeconds())

        if (areCandlesUpToDate) {
            lineChart.addCandles(candles, account.currency, chartTimeSpan)
            setPercentChangeText(chartTimeSpan)
            nameText.text = currency.fullName
            setButtonsAndBalanceText(account)
            updateHistoryListAdapter(stashedOrders, stashedFills)
        } else {
            activity.showProgressBar()
            miniRefresh({   //onfailure
                if (chartTimeSpan != Timespan.DAY) {
                    val backupTimespan = chartTimeSpan
                    chartTimeSpan = Timespan.DAY
                    miniRefresh({
                        toast("Error")
                        chartTimeSpan = backupTimespan
                        activity.dismissProgressBar()
                    }, {
                        checkTimespanButton()
                        candles = account.product.candlesForTimespan(chartTimeSpan)
                        lineChart.addCandles(candles, account.currency, chartTimeSpan)
                        setPercentChangeText(chartTimeSpan)
                        nameText.text = currency.fullName
                        activity.dismissProgressBar()
                        setButtonsAndBalanceText(account)

                        updateHistoryListAdapter(stashedOrders, stashedFills)
                    })
                } else {
                    toast("Error")
                    activity.dismissProgressBar()
                }
            }, {    //success
                candles = account.product.candlesForTimespan(chartTimeSpan)
                lineChart.addCandles(candles, account.currency, chartTimeSpan)
                setPercentChangeText(chartTimeSpan)
                nameText.text = currency.fullName
                setButtonsAndBalanceText(account)
                activity.dismissProgressBar()
                updateHistoryListAdapter(stashedOrders, stashedFills)
            })
        }
    }
    
    private fun setButtonsAndBalanceText(account: Account) {
        (activity as? com.anyexchange.anyx.activities.MainActivity)?.let { activity ->
            val currency = account.currency
            val buttonColors = currency.colorStateList(activity)
            buyButton.backgroundTintList = buttonColors
            sellButton.backgroundTintList = buttonColors
            val buttonTextColor = currency.buttonTextColor(activity)
            buyButton.textColor = buttonTextColor
            sellButton.textColor = buttonTextColor
            val tabColor = currency.colorPrimary(activity)
            historyTabList.setSelectedTabIndicatorColor(tabColor)
            val prefs = Prefs(activity)
            if (prefs.isLoggedIn) {
                tickerText.text = "$currency wallet"
                iconView.setImageResource(currency.iconId)
                balanceText.text = "${account.balance.btcFormat()} $currency"
                valueText.text = account.value.fiatFormat()

                historyPager.visibility = View.VISIBLE
            } else {
                tickerText.visibility = View.GONE
                iconView.visibility = View.GONE
                balanceText.visibility = View.GONE
                valueText.visibility = View.GONE

                historyPager.visibility = View.INVISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()

        showNavSpinner(account?.currency) { selectedCurrency ->
            account = Account.forCurrency(selectedCurrency)
            account?. let { account ->
                switchAccount(account)
            }
        }

        val account = account
        val activity = activity!!
        if (account == null) {
            activity.supportFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
        } else {
            switchAccount(account)
        }

        autoRefresh = Runnable {
            miniRefresh({ }, { })
            handler.postDelayed(autoRefresh, (TimeInSeconds.halfMinute * 1000))
        }
        handler.postDelayed(autoRefresh, (TimeInSeconds.halfMinute * 1000))
        doneLoading()
    }

    override fun onPause() {
        handler.removeCallbacks(autoRefresh)
        super.onPause()
    }

    private fun checkTimespanButton() {
        when (chartTimeSpan) {
            Timespan.HOUR -> timespanButtonHour.isChecked = true
            Timespan.DAY ->  timespanButtonDay.isChecked = true
            Timespan.WEEK -> timespanButtonWeek.isChecked = true
            Timespan.MONTH -> timespanButtonMonth.isChecked = true
            Timespan.YEAR -> timespanButtonYear.isChecked = true
//            Timespan.ALL -> timespanButtonAll.isChecked = true
        }
    }

    private fun setChartTimespan(timespan: Timespan) {
        checkTimespanButton()
        chartTimeSpan = timespan
//        (activity as MainActivity).showProgressBar()
        //TODO: show spinner on top
        miniRefresh({
            toast("Error updating chart time")
            (activity as? com.anyexchange.anyx.activities.MainActivity)?.dismissProgressBar()
        }, {
            checkTimespanButton()
            (activity as? com.anyexchange.anyx.activities.MainActivity)?.dismissProgressBar()
        })
    }

    private fun setPercentChangeText(timespan: Timespan) {
        account?.let { account ->
            val percentChange = account.product.percentChange(timespan)
            percentChangeText.text = percentChange.percentFormat()
            percentChangeText.textColor = if (percentChange >= 0) {
                Color.GREEN
            } else {
                Color.RED
            }
        }
    }

    private fun orderOnClick(order: ApiOrder) {
        alert {
            title = "Order"

            val layoutWidth = 1000
            val createdTimeRaw = order.created_at
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'")
            val createdTime = try {
                val date = format.parse(createdTimeRaw)
//                System.out.println(date)
                val outputFormat = SimpleDateFormat("h:mma, MM/dd/yyyy")
                outputFormat.format(date)

            } catch (e: ParseException) {
                //e.printStackTrace()
                createdTimeRaw
            }
            val fillFees = order.fill_fees.toDouble().fiatFormat()
            val price = order.price.toDouble().fiatFormat()
            val filledSize = order.filled_size.toDouble().toString()
            val size = (order.size ?: "0").toDouble().btcFormat()

            customView {
                linearLayout {
                    verticalLayout {
                        horizontalLayout("Side:", order.side).lparams(width = layoutWidth) {}
                        horizontalLayout("Size:", size).lparams(width = layoutWidth) {}
                        horizontalLayout("Filled Size:", filledSize).lparams(width = layoutWidth) {}
                        horizontalLayout("Price:", price).lparams(width = layoutWidth) {}
                        horizontalLayout("Status:", order.status).lparams(width = layoutWidth) {}
                        horizontalLayout("Fill fees:", fillFees).lparams(width = layoutWidth) {}
                        horizontalLayout("Time:", createdTime).lparams(width = layoutWidth) {}
                    }.lparams(width = matchParent) {leftMargin = dip(20) }
                }
            }
            positiveButton("OK") {  }
            negativeButton("Cancel Order") {
                GdaxApi.cancelOrder(order.id).executeRequest({ }) {
                    var orders = (historyPager.adapter as HistoryPagerAdapter).orders
                    orders = orders.filter { o -> o.id != order.id }
                    (historyPager.adapter as HistoryPagerAdapter).orders = orders
                    (historyPager.adapter as HistoryPagerAdapter).notifyDataSetChanged()
                    toast("Order cancelled")
                }
            }
        }.show()
    }

    private fun fillOnClick(fill: ApiFill) {
        alert {
            title = "Fill"

            val layoutWidth = 1000
            val createdTimeRaw = fill.created_at
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'")
            val createdTime = try {
                val date = format.parse(createdTimeRaw)
//                System.out.println(date)
                val outputFormat = SimpleDateFormat("h:mma, MM/dd/yyyy")
                outputFormat.format(date)

            } catch (e: ParseException) {
                //e.printStackTrace()
                createdTimeRaw
            }
            val fee = fill.fee.toDouble().fiatFormat()
            val price = fill.price.toDouble().fiatFormat()
            val size = fill.size.toDouble().btcFormat()
            customView {
                linearLayout {
                    verticalLayout {
                        horizontalLayout("Side:  ", fill.side).lparams(width = layoutWidth) {}
                        horizontalLayout("Size:  ", size).lparams(width = layoutWidth) {}
                        horizontalLayout("Price:  ", price).lparams(width = layoutWidth) {}
                        horizontalLayout("Fee:  ", fee).lparams(width = layoutWidth) {}
                        horizontalLayout("Time:  ", createdTime).lparams(width = layoutWidth) {}
                    }.lparams(width = matchParent) {leftMargin = dip(20) }
                }
            }
            positiveButton("OK") {  }
        }.show()
    }

    override fun onValueSelected(entry: Entry, h: Highlight) {
        val index = entry.x.toInt()
        if (candles.size > index) {
            priceText.text = entry.y.toDouble().fiatFormat()
            val candle = candles[index]
            percentChangeText.text = candle.time.toStringWithTimespan(chartTimeSpan)
            val prefs = Prefs(context!!)
            if (prefs.isDarkModeOn) {
                percentChangeText.textColor = Color.WHITE
            } else {
                percentChangeText.textColor = Color.BLACK
            }
        }
    }

    override fun onNothingSelected() {
        val account = account
        if (account != null) {
            priceText.text = account.product.price.fiatFormat()
            setPercentChangeText(chartTimeSpan)
            lineChart.highlightValues(arrayOf<Highlight>())
        }
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        when (view) {
            historyPager -> {
                when (motionEvent.action){
                    MotionEvent.ACTION_MOVE -> {
                        swipeRefreshLayout?.isEnabled = false
                    }
                    MotionEvent.ACTION_DOWN -> {
                        swipeRefreshLayout?.isEnabled = false
                    }
                    MotionEvent.ACTION_UP -> {
                        swipeRefreshLayout?.isEnabled = true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        swipeRefreshLayout?.isEnabled = true
                    }
                }
            }
        }
        return false
    }

    override fun onChartGestureStart(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) { }
    override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {
        swipeRefreshLayout?.isEnabled = true
        onNothingSelected()
    }
    override fun onChartLongPressed(me: MotionEvent) {
        swipeRefreshLayout?.isEnabled = false
    }
    override fun onChartDoubleTapped(me: MotionEvent) { }
    override fun onChartSingleTapped(me: MotionEvent) { }
    override fun onChartFling(me1: MotionEvent, me2: MotionEvent, velocityX: Float, velocityY: Float) { }
    override fun onChartScale(me: MotionEvent, scaleX: Float, scaleY: Float) { }
    override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) { }

    override fun refresh(onComplete: (Boolean) -> Unit) {
        val gson = Gson()
        val onFailure = { result: Result.Failure<String, FuelError> ->
            toast("Error!: ${result.error}")
            println("error!" )
            onComplete(false)
        }
        val prefs = Prefs(context!!)
        account?. let { account ->
            if (prefs.isLoggedIn) {
                GdaxApi.account(account.id).executeRequest(onFailure) { result ->
                    val apiAccount: ApiAccount = gson.fromJson(result.value, object : TypeToken<ApiAccount>() {}.type)
                    val newBalance = apiAccount.balance.toDoubleOrZero()
                    balanceText.text = newBalance.btcFormat() + " " + account.currency
                    valueText.text = account.value.fiatFormat()
                    miniRefresh(onFailure) {
                        account.apiAccount = apiAccount
                        onComplete(true)
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
//                    account.balance = 0.0
                    onComplete(true)
                }
            }
        }
    }

    //TODO: rename
    private fun updateHistoryListAdapter(orderList: List<ApiOrder>, fillList: List<ApiFill>) {
        (historyPager.adapter as HistoryPagerAdapter).orders = orderList
        (historyPager.adapter as HistoryPagerAdapter).fills  = fillList
        (historyPager.adapter as HistoryPagerAdapter).notifyDataSetChanged()
        //historyList.setHeightBasedOnChildren()
    }

    private fun miniRefresh(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        val account = account
        if (account == null) {
            onComplete()
        } else {
            account.product.updateCandles(chartTimeSpan, onFailure, { _ ->
                candles = account.product.candlesForTimespan(chartTimeSpan)

                GdaxApi.ticker(account.product.id).executeRequest(onFailure) { result ->
                    val ticker: ApiTicker = Gson().fromJson(result.value, object : TypeToken<ApiTicker>() {}.type)
                    val price = ticker.price.toDoubleOrNull()
                    if (price != null) {
                        account.product.price = price
                    }
                    priceText.text = account.product.price.fiatFormat()
                    valueText.text = account.value.fiatFormat()
                    
                    lineChart.addCandles(candles, account.currency, chartTimeSpan)
                    setPercentChangeText(chartTimeSpan)
                    checkTimespanButton()
                    onComplete()
                }
            })
        }
    }
}
