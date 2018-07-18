package com.anyexchange.anyx.fragments.main

import android.graphics.Color
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
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
import com.anyexchange.anyx.adapters.HistoryPagerAdapter
import kotlinx.android.synthetic.main.fragment_chart.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by anyexchange on 11/5/2017.
 */
class ChartFragment : RefreshFragment(), OnChartValueSelectedListener, OnChartGestureListener, View.OnTouchListener {
    private lateinit var inflater: LayoutInflater

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

        setupSwipeRefresh(rootView.swipe_refresh_layout as SwipeRefreshLayout)

        val account = account
        val activity = activity!!
        if (account == null) {
            activity.supportFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
        } else {
            val prefs = Prefs(activity)

            checkTimespanButton()
            candles = account.product.candlesForTimespan(chartTimeSpan)
            val currency = account.currency

            chart_fragment_chart.configure(candles, currency, true, PriceChart.DefaultDragDirection.Horizontal,  chartTimeSpan) {
                swipeRefreshLayout?.isEnabled = false
            }
            chart_fragment_chart.setOnChartValueSelectedListener(this)
            chart_fragment_chart.onChartGestureListener = this
            
            btn_chart_buy.setOnClickListener {
                if (!prefs.isLoggedIn) {
                    toast("Please log in")
                } else if (CBProApi.credentials?.isValidated == null) { //(CBProApi.credentials?.isValidated == null) {
                    toast("Please validate your account in Settings")
                } else if (CBProApi.credentials?.isValidated == false) { // (CBProApi.credentials?.isValidated == false) {
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

            btn_chart_sell.setOnClickListener {
                if (!prefs.isLoggedIn) {
                    toast("Please log in")
                } else if (CBProApi.credentials?.isValidated == null) { //(CBProApi.credentials?.isValidated == null) {
                    toast("Please validate your account in Settings")
                } else if (CBProApi.credentials?.isValidated == false) { // (CBProApi.credentials?.isValidated == false) {
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
            
            rbtn_chart_timespan_hour.text = "1H"
            rbtn_chart_timespan_hour.setOnClickListener {
                setChartTimespan(Timespan.HOUR)
            }
            rbtn_chart_timespan_day.text = "1D"
            rbtn_chart_timespan_day.setOnClickListener {
                setChartTimespan(Timespan.DAY)
            }
            rbtn_chart_timespan_week.text = "1W"
            rbtn_chart_timespan_week.setOnClickListener {
                setChartTimespan(Timespan.WEEK)
            }
            rbtn_chart_timespan_month.text = "1M"
            rbtn_chart_timespan_month.setOnClickListener {
                setChartTimespan(Timespan.MONTH)
            }
            rbtn_chart_timespan_year.text = "1Y"
            rbtn_chart_timespan_year.setOnClickListener {
                setChartTimespan(Timespan.YEAR)
            }
//            timespanButtonAll.setText("ALL")
//            timespanButtonAll.setOnClickListener {
//                setChartTimespan(Timespan.ALL)
//            }

            val stashedFills = prefs.getStashedFills(account.product.id)
            val stashedOrders = prefs.getStashedOrders(account.product.id)
            history_view_pager.adapter = HistoryPagerAdapter(childFragmentManager, stashedOrders, stashedFills,
                    { order -> orderOnClick(order)}, { fill -> fillOnClick(fill) })
            history_view_pager.setOnTouchListener(this)
        }
        return rootView
    }


    private fun switchAccount(account: Account) {
        Companion.account = account
        val activity = activity as com.anyexchange.anyx.activities.MainActivity
        val currency = account.currency
        val price = account.product.price
        txt_chart_price.text = price.fiatFormat()

        candles = account.product.candlesForTimespan(chartTimeSpan)

        val prefs = Prefs(activity)
        val stashedFills = prefs.getStashedFills(account.product.id)
        val stashedOrders = prefs.getStashedOrders(account.product.id)

        val now = Calendar.getInstance()
        val lastCandleTime = candles.lastOrNull()?.time?.toLong() ?: 0
        val nextCandleTime: Long = lastCandleTime + Candle.granularityForTimespan(chartTimeSpan)
        val areCandlesUpToDate = candles.isNotEmpty() && (nextCandleTime > now.timeInSeconds())

        if (areCandlesUpToDate) {
            chart_fragment_chart.addCandles(candles, account.currency, chartTimeSpan)
            setPercentChangeText(chartTimeSpan)
            txt_chart_name.text = currency.fullName
            setButtonsAndBalanceText(account)
            updateHistoryPagerAdapter(stashedOrders, stashedFills)
        } else {
//            activity.showProgressBar()
            miniRefresh({   //onfailure
                if (chartTimeSpan != Timespan.DAY) {
                    val backupTimespan = chartTimeSpan
                    chartTimeSpan = Timespan.DAY
                    miniRefresh({
                        toast("Error")
                        chartTimeSpan = backupTimespan
//                        activity.dismissProgressBar()
                    }, {
                        checkTimespanButton()
                        candles = account.product.candlesForTimespan(chartTimeSpan)
                        chart_fragment_chart.addCandles(candles, account.currency, chartTimeSpan)
                        setPercentChangeText(chartTimeSpan)
                        txt_chart_name.text = currency.fullName
//                        activity.dismissProgressBar()
                        setButtonsAndBalanceText(account)

                        updateHistoryPagerAdapter(stashedOrders, stashedFills)
                    })
                } else {
                    toast("Error")
//                    activity.dismissProgressBar()
                }
            }, {    //success
                candles = account.product.candlesForTimespan(chartTimeSpan)
                chart_fragment_chart.addCandles(candles, account.currency, chartTimeSpan)
                setPercentChangeText(chartTimeSpan)
                txt_chart_name.text = currency.fullName
                setButtonsAndBalanceText(account)
//                activity.dismissProgressBar()
                updateHistoryPagerAdapter(stashedOrders, stashedFills)
            })
        }
    }
    
    private fun setButtonsAndBalanceText(account: Account) {
        (activity as? com.anyexchange.anyx.activities.MainActivity)?.let { activity ->
            val currency = account.currency
            val buttonColors = currency.colorStateList(activity)
            btn_chart_buy.backgroundTintList = buttonColors
            btn_chart_sell.backgroundTintList = buttonColors
            val buttonTextColor = currency.buttonTextColor(activity)
            btn_chart_buy.textColor = buttonTextColor
            btn_chart_sell.textColor = buttonTextColor
            val tabColor = currency.colorPrimary(activity)
            history_tab_layout.setSelectedTabIndicatorColor(tabColor)
            val prefs = Prefs(activity)
            if (prefs.isLoggedIn) {
                txt_chart_ticker.text = "$currency wallet"
                img_chart_account_icon.setImageResource(currency.iconId)
                txt_chart_account_balance.text = "${account.balance.btcFormat()} $currency"
                txt_chart_account_value.text = account.value.fiatFormat()

                history_view_pager.visibility = View.VISIBLE
            } else {
                txt_chart_ticker.visibility = View.GONE
                img_chart_account_icon.visibility = View.GONE
                txt_chart_account_balance.visibility = View.GONE
                txt_chart_account_value.visibility = View.GONE

                history_view_pager.visibility = View.INVISIBLE
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
            Timespan.HOUR -> rbtn_chart_timespan_hour.isChecked = true
            Timespan.DAY ->  rbtn_chart_timespan_day.isChecked = true
            Timespan.WEEK -> rbtn_chart_timespan_week.isChecked = true
            Timespan.MONTH -> rbtn_chart_timespan_month.isChecked = true
            Timespan.YEAR -> rbtn_chart_timespan_year.isChecked = true
//            Timespan.ALL -> timespanButtonAll.isChecked = true
        }
    }

    private fun setChartTimespan(timespan: Timespan) {
        checkTimespanButton()
        chartTimeSpan = timespan
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
            txt_chart_change_or_date.text = percentChange.percentFormat()
            txt_chart_change_or_date.textColor = if (percentChange >= 0) {
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
            val locale = Locale.getDefault()
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'", locale)
            val createdTime = try {
                val date = format.parse(createdTimeRaw)
//                System.out.println(date)
                val outputFormat = SimpleDateFormat("h:mma, MM/dd/yyyy", locale)
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
                CBProApi.cancelOrder(order.id).executeRequest({ }) {
                    var orders = (history_view_pager.adapter as HistoryPagerAdapter).orders
                    orders = orders.filter { o -> o.id != order.id }
                    updateHistoryPagerAdapter(orders)
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
            val locale = Locale.getDefault()
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'", locale)
            val createdTime = try {
                val date = format.parse(createdTimeRaw)
//                System.out.println(date)
                val outputFormat = SimpleDateFormat("h:mma, MM/dd/yyyy", locale)
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
            txt_chart_price.text = entry.y.toDouble().fiatFormat()
            val candle = candles[index]
            txt_chart_change_or_date.text = candle.time.toStringWithTimespan(chartTimeSpan)
            val prefs = Prefs(context!!)
            if (prefs.isDarkModeOn) {
                txt_chart_change_or_date.textColor = Color.WHITE
            } else {
                txt_chart_change_or_date.textColor = Color.BLACK
            }
        }
    }

    override fun onNothingSelected() {
        val account = account
        if (account != null) {
            txt_chart_price.text = account.product.price.fiatFormat()
            setPercentChangeText(chartTimeSpan)
            chart_fragment_chart.highlightValues(arrayOf<Highlight>())
        }
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        when (view) {
            history_view_pager -> {
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
                CBProApi.account(account.id).executeRequest(onFailure) { result ->
                    val apiAccount: ApiAccount = gson.fromJson(result.value, object : TypeToken<ApiAccount>() {}.type)
                    val newBalance = apiAccount.balance.toDoubleOrZero()
                    txt_chart_account_balance.text = newBalance.btcFormat() + " " + account.currency
                    txt_chart_account_value.text = account.value.fiatFormat()
                    miniRefresh(onFailure) {
                        account.apiAccount = apiAccount
                        onComplete(true)
                    }
                }

                var filteredOrders: List<ApiOrder>? = null
                var filteredFills: List<ApiFill>? = null
                CBProApi.listOrders(productId = account.product.id).executeRequest(onFailure) { result ->
                    prefs.stashOrders(result.value)
                    val apiOrderList: List<ApiOrder> = gson.fromJson(result.value, object : TypeToken<List<ApiOrder>>() {}.type)
                    filteredOrders = apiOrderList.filter { it.product_id == account.product.id }
                    if (filteredOrders != null && filteredFills != null) {
                        updateHistoryPagerAdapter(filteredOrders!!, filteredFills!!)
                    }
                }
                CBProApi.fills(productId = account.product.id).executeRequest(onFailure) { result ->
                    prefs.stashFills(result.value)
                    val apiFillList: List<ApiFill> = gson.fromJson(result.value, object : TypeToken<List<ApiFill>>() {}.type)
                    filteredFills = apiFillList.filter { it.product_id == account.product.id }
                    if (filteredOrders != null && filteredFills != null) {
                        updateHistoryPagerAdapter(filteredOrders!!, filteredFills!!)
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

    private fun updateHistoryPagerAdapter(orderList: List<ApiOrder>, fillList: List<ApiFill>? = null) {
        (history_view_pager.adapter as HistoryPagerAdapter).orders = orderList
        fillList?.let {
            (history_view_pager.adapter as HistoryPagerAdapter).fills = it
        }
        (history_view_pager.adapter as HistoryPagerAdapter).notifyDataSetChanged()
        //historyList.setHeightBasedOnChildren()
    }

    private fun miniRefresh(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        val account = account
        if (account == null) {
            onComplete()
        } else {
            account.product.updateCandles(chartTimeSpan, onFailure, { _ ->
                candles = account.product.candlesForTimespan(chartTimeSpan)

                CBProApi.ticker(account.product.id).executeRequest(onFailure) { result ->
                    val ticker: ApiTicker = Gson().fromJson(result.value, object : TypeToken<ApiTicker>() {}.type)
                    val price = ticker.price.toDoubleOrNull()
                    if (price != null) {
                        account.product.price = price
                    }
                    txt_chart_price.text = account.product.price.fiatFormat()
                    txt_chart_account_value.text = account.value.fiatFormat()

                    chart_fragment_chart.addCandles(candles, account.currency, chartTimeSpan)
                    setPercentChangeText(chartTimeSpan)
                    checkTimespanButton()
                    onComplete()
                }
            })
        }
    }
}
