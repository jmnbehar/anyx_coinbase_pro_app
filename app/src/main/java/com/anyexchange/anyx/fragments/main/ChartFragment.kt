package com.anyexchange.anyx.fragments.main

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.graphics.Color
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.view.*
import com.github.kittinunf.result.Result
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.gson.Gson
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.fragment_chart.view.*
import com.github.kittinunf.fuel.core.FuelError
import com.github.mikephil.charting.listener.ChartTouchListener
import org.jetbrains.anko.*
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast
import android.view.MotionEvent
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.anyexchange.anyx.activities.MainActivity
import com.anyexchange.anyx.adapters.HistoryPagerAdapter
import com.anyexchange.anyx.classes.Currency
import kotlinx.android.synthetic.main.fragment_chart.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by anyexchange on 11/5/2017.
 */
class ChartFragment : RefreshFragment(), OnChartValueSelectedListener, OnChartGestureListener, View.OnTouchListener, LifecycleOwner {
    private lateinit var inflater: LayoutInflater
    private var historyPager: ViewPager? = null
    private var chartTimeSpan = Timespan.DAY
    private var tradingPairSpinner: Spinner? = null

    private val tradingPair: TradingPair?
        get() = tradingPairSpinner?.selectedItem as? TradingPair

    private val quoteCurrency: Currency
        get() = tradingPair?.quoteCurrency ?: Currency.USD

    private var candles = listOf<Candle>()

    private var tradeFragment: TradeFragment? = null

    companion object {
        var account: Account? = null
        fun newInstance(account: Account): ChartFragment {
            this.account = account
            return ChartFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        println("ChartFragment - onCreateView - 1")
        val rootView = inflater.inflate(R.layout.fragment_chart, container, false)

        showDarkMode(rootView)

        this.inflater = inflater
        historyPager = rootView.history_view_pager

        setupSwipeRefresh(rootView.swipe_refresh_layout as SwipeRefreshLayout)

        println("ChartFragment - onCreateView - 2")
        val tempAccount = account
        val activity = activity!!
        if (tempAccount == null) {
            println("ChartFragment - onCreateView - 3")
            activity.supportFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
        } else {
            println("ChartFragment - onCreateView - 4")
            val prefs = Prefs(activity)
            candles = tempAccount.product.candlesForTimespan(chartTimeSpan, tradingPair)
            val currency = tempAccount.currency

            println("ChartFragment - onCreateView - 5")
            rootView.chart_fragment_chart.configure(candles, currency, true, PriceChart.DefaultDragDirection.Horizontal) {
                swipeRefreshLayout?.isEnabled = false
            }
            println("ChartFragment - onCreateView - 6")
            rootView.chart_fragment_chart.setOnChartValueSelectedListener(this)
            println("ChartFragment - onCreateView - 7")
            rootView.chart_fragment_chart.onChartGestureListener = this
            println("ChartFragment - onCreateView - 8")

            rootView.btn_chart_buy.setOnClickListener {
                buySellButtonOnClick(prefs.isLoggedIn, tempAccount, TradeSide.BUY)
            }

            println("ChartFragment - onCreateView - 9")
            rootView.btn_chart_sell.setOnClickListener {
                buySellButtonOnClick(prefs.isLoggedIn, tempAccount, TradeSide.SELL)
            }

            rootView.rbtn_chart_timespan_hour.text = resources.getString(R.string.chart_timespan_1h)
            rootView.rbtn_chart_timespan_hour.setOnClickListener {
                setChartTimespan(Timespan.HOUR)
            }
            rootView.rbtn_chart_timespan_day.text = resources.getString(R.string.chart_timespan_1d)
            rootView.rbtn_chart_timespan_day.setOnClickListener {
                setChartTimespan(Timespan.DAY)
            }
            rootView.rbtn_chart_timespan_week.text = resources.getString(R.string.chart_timespan_1w)
            rootView.rbtn_chart_timespan_week.setOnClickListener {
                setChartTimespan(Timespan.WEEK)
            }
            rootView.rbtn_chart_timespan_month.text = resources.getString(R.string.chart_timespan_1m)
            rootView.rbtn_chart_timespan_month.setOnClickListener {
                setChartTimespan(Timespan.MONTH)
            }
            rootView.rbtn_chart_timespan_year.text = resources.getString(R.string.chart_timespan_1y)
            rootView.rbtn_chart_timespan_year.setOnClickListener {
                setChartTimespan(Timespan.YEAR)
            }
            println("ChartFragment - onCreateView - 10")
//            timespanButtonAll.setText("ALL")
//            timespanButtonAll.setOnClickListener {
//                setChartTimespan(Timespan.ALL)
//            }

            val tradingPairs = if (tempAccount.product.tradingPairs.isNotEmpty()) {
                tempAccount.product.tradingPairs.sortedBy { it.quoteCurrency.orderValue }
            } else {
                listOf(tempAccount.id)
            }
            println("ChartFragment - onCreateView - 11")
            //TODO: don't use simple_spinner_item
            val arrayAdapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, tradingPairs)
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            tradingPairSpinner = rootView.spinner_chart_trading_pair
            tradingPairSpinner?.adapter = arrayAdapter
            tradingPairSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                        showProgressSpinner()
                        miniRefresh({
                            toast(R.string.chart_update_error)
                            dismissProgressSpinner()
                        }, {
                            dismissProgressSpinner()
                        })
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                }
            }

            println("ChartFragment - onCreateView - 12")
            val stashedFills = prefs.getStashedFills(tempAccount.product.id)
            val stashedOrders = prefs.getStashedOrders(tempAccount.product.id)
            historyPager?.adapter = HistoryPagerAdapter(childFragmentManager, stashedOrders, stashedFills,
                    { order -> orderOnClick(order)}, { fill -> fillOnClick(fill) })
            historyPager?.setOnTouchListener(this)
            println("ChartFragment - onCreateView - 13")
        }
        return rootView
    }

    override fun onResume() {
        println("ChartFragment - onResume - 1")
        super.onResume()
        println("ChartFragment - onResume - 2")
        checkTimespanButton()
        println("ChartFragment - onResume - 3")
        showNavSpinner(account?.currency) { selectedCurrency ->
            //            showProgressSpinner()
            account = Account.forCurrency(selectedCurrency)
            account?. let { account ->
                switchAccount(account)
            }
        }
        println("ChartFragment - onResume - 4")

        if (account != null) {
            println("ChartFragment - onResume - 5")
            setButtonColors()
            println("ChartFragment - onResume - 6")
            switchAccount(account!!)
            println("ChartFragment - onResume - 7")
        } else {
            println("ChartFragment - onResume - 8")
            val mainActivity = activity as? MainActivity
            val selectedCurrency = mainActivity?.spinnerNav?.selectedItem as? Currency
            println("ChartFragment - onResume - 9")
            account = if (selectedCurrency != null) {
                println("ChartFragment - onResume - 10")
                Account.forCurrency(selectedCurrency)
            } else {
                println("ChartFragment - onResume - 11")
                Account.forCurrency(Currency.BTC)
            }
            println("ChartFragment - onResume - 12")
            setButtonsAndBalanceText(account!!)
            println("ChartFragment - onResume - 13")
            switchAccount(account!!)
            println("ChartFragment - onResume - 14")
        }

        autoRefresh = Runnable {
            miniRefresh({ }, { })
            handler.postDelayed(autoRefresh, (TimeInSeconds.halfMinute * 1000))
        }
        handler.postDelayed(autoRefresh, (TimeInSeconds.halfMinute * 1000))
        println("ChartFragment - onResume - 15")
        dismissProgressSpinner()
        println("ChartFragment - onResume - 16")
    }

    override fun onPause() {
        handler.removeCallbacks(autoRefresh)
        super.onPause()
    }

    private fun buySellButtonOnClick(isLoggedIn: Boolean, account: Account, tradeSide: TradeSide) {
        if (!isLoggedIn) {
            toast(R.string.toast_please_login_message)
        } else if (CBProApi.credentials?.isValidated == null) {
            toast(R.string.toast_please_validate_message)
        } else if (CBProApi.credentials?.isValidated == false) {
            toast(R.string.toast_missing_permissions_message)
        } else {
            if (tradeFragment == null) {
                tradeFragment = TradeFragment.newInstance(account, tradeSide)
            } else {
                tradeFragment?.tradeSide = tradeSide
            }
            (activity as com.anyexchange.anyx.activities.MainActivity).goToFragment(tradeFragment!!, "Trade: " + tradeSide.name)
        }
    }

    private fun switchAccount(newAccount: Account) {
        println("ChartFragment - switchAccount - 1")
        account = newAccount

        val activity = activity as com.anyexchange.anyx.activities.MainActivity
        val currency = newAccount.currency

        candles = newAccount.product.candlesForTimespan(chartTimeSpan, tradingPair)

        val price = newAccount.product.priceForQuoteCurrency(quoteCurrency)
        txt_chart_price.text = price.format(quoteCurrency)

        println("ChartFragment - switchAccount - 2")
        val tradingPairs = account?.product?.tradingPairs ?: listOf()
        val arrayAdapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, tradingPairs)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_chart_trading_pair.adapter = arrayAdapter

        println("ChartFragment - switchAccount - 3")
        val prefs = Prefs(activity)
        val stashedFills = prefs.getStashedFills(newAccount.product.id)
        val stashedOrders = prefs.getStashedOrders(newAccount.product.id)

        println("ChartFragment - switchAccount - 4")
        val now = Calendar.getInstance()
        val lastCandleTime = candles.lastOrNull()?.time?.toLong() ?: 0
        val nextCandleTime: Long = lastCandleTime + Candle.granularityForTimespan(chartTimeSpan)
        val areCandlesUpToDate = candles.isNotEmpty() && (nextCandleTime > now.timeInSeconds())

        println("ChartFragment - switchAccount - 5")
        if (areCandlesUpToDate) {
            println("ChartFragment - switchAccount - 6")
            chart_fragment_chart.addCandles(candles, newAccount.currency)
            setPercentChangeText(chartTimeSpan)
            txt_chart_name.text = currency.fullName
            println("ChartFragment - switchAccount - 7")
            setButtonsAndBalanceText(newAccount)
            updateHistoryPagerAdapter(stashedOrders, stashedFills)
            println("ChartFragment - switchAccount - 8")
        } else {
            println("ChartFragment - switchAccount - 9")
            showProgressSpinner()
            println("ChartFragment - switchAccount - 10")
            miniRefresh({   //onFailure
                println("ChartFragment - switchAccount - minirefresh onFailure - 11")
                toast(R.string.error_message)
                dismissProgressSpinner()
                println("ChartFragment - switchAccount - minirefresh onFailure - 12")
            }, {    //success
                println("ChartFragment - switchAccount - 13")
                candles = newAccount.product.candlesForTimespan(chartTimeSpan, tradingPair)
                chart_fragment_chart.addCandles(candles, newAccount.currency)
                println("ChartFragment - switchAccount - 14")
                setPercentChangeText(chartTimeSpan)
                txt_chart_name.text = currency.fullName
                println("ChartFragment - switchAccount - 15")
                setButtonsAndBalanceText(newAccount)
                println("ChartFragment - switchAccount - 16")
                dismissProgressSpinner()
                updateHistoryPagerAdapter(stashedOrders, stashedFills)
                println("ChartFragment - switchAccount - 17")
            })
        }
    }
    
    private fun setButtonsAndBalanceText(account: Account) {
        context?.let {
            val currency = account.currency
            setButtonColors()
            val prefs = Prefs(it)
            if (prefs.isLoggedIn) {
                val value = account.valueForQuoteCurrency(quoteCurrency)
                txt_chart_ticker.text = resources.getString(R.string.chart_wallet_label, currency.toString())
                img_chart_account_icon.setImageResource(currency.iconId)
                txt_chart_account_balance.text = resources.getString(R.string.chart_balance_text, account.balance.btcFormat(), currency)
                txt_chart_account_value.text = value.format(quoteCurrency)

                historyPager?.visibility = View.VISIBLE
            } else {
                txt_chart_ticker.visibility = View.GONE
                img_chart_account_icon.visibility = View.GONE
                txt_chart_account_balance.visibility = View.GONE
                txt_chart_account_value.visibility = View.GONE

                historyPager?.visibility = View.INVISIBLE
            }
        }
    }

    private fun setButtonColors() {
        context?.let { context ->
            account?.currency?.let { currency ->
                val buttonColors = currency.colorStateList(context)
                btn_chart_buy.backgroundTintList = buttonColors
                btn_chart_sell.backgroundTintList = buttonColors
                val buttonTextColor = currency.buttonTextColor(context)
                btn_chart_buy.textColor = buttonTextColor
                btn_chart_sell.textColor = buttonTextColor
                val tabColor = currency.colorPrimary(context)
                history_tab_layout.setSelectedTabIndicatorColor(tabColor)
            }
        }
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
        showProgressSpinner()
        miniRefresh({
            toast(R.string.chart_update_error)
            dismissProgressSpinner()
        }, {
            checkTimespanButton()
            dismissProgressSpinner()
        })
    }


    private fun setPercentChangeText(timespan: Timespan) {
        account?.let { account ->
            val percentChange = account.product.percentChange(timespan, quoteCurrency)
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
            title = resources.getString(R.string.chart_history_order)
            val layoutWidth = 1000
            val createdTimeRaw = order.created_at
            val locale = Locale.getDefault()
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'", locale)
            val createdTime = try {
                val date = format.parse(createdTimeRaw)
                val outputFormat = SimpleDateFormat("h:mma, MM/dd/yyyy", locale)
                outputFormat.format(date)

            } catch (e: ParseException) {
                //e.printStackTrace()
                createdTimeRaw
            }
            val currency = Currency.forString(order.product_id) ?: Currency.USD
            val fillFees = order.fill_fees.toDouble().format(quoteCurrency)
            val price = order.price.toDouble().format(quoteCurrency)
            val filledSize = order.filled_size.toDouble().toString()
            val size = (order.size ?: "0").toDouble().btcFormat()

            customView {
                linearLayout {
                    verticalLayout {
                        horizontalLayout(R.string.chart_history_side_label, order.side).lparams(width = layoutWidth) {}
                        horizontalLayout(R.string.chart_history_size_label, size).lparams(width = layoutWidth) {}
                        horizontalLayout(R.string.chart_history_filled_size_label, filledSize).lparams(width = layoutWidth) {}
                        horizontalLayout(R.string.chart_history_price_label, price).lparams(width = layoutWidth) {}
                        horizontalLayout(R.string.chart_history_status_label, order.status).lparams(width = layoutWidth) {}
                        horizontalLayout(R.string.chart_history_filled_fees_label, fillFees).lparams(width = layoutWidth) {}
                        horizontalLayout(R.string.chart_history_time_label, createdTime).lparams(width = layoutWidth) {}
                    }.lparams(width = matchParent) {leftMargin = dip(20) }
                }
            }
            positiveButton(R.string.popup_ok_btn) {  }
            negativeButton(R.string.chart_cancel_order) {
                CBProApi.cancelOrder(order.id).executeRequest({ }) {
                    if (lifecycle.isCreatedOrResumed) {
                        var orders = (historyPager?.adapter as HistoryPagerAdapter).orders
                        orders = orders.filter { o -> o.id != order.id }
                        updateHistoryPagerAdapter(orders)
                        toast(R.string.chart_order_cancelled)
                    }
                }
            }
        }.show()
    }

    private fun fillOnClick(fill: ApiFill) {
        alert {
            title = resources.getString(R.string.chart_history_fill)

            val layoutWidth = 1000
            val createdTimeRaw = fill.created_at
            val locale = Locale.getDefault()
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'", locale)
            val createdTime = try {
                val date = format.parse(createdTimeRaw)
                val outputFormat = SimpleDateFormat("h:mma, MM/dd/yyyy", locale)
                outputFormat.format(date)

            } catch (e: ParseException) {
                //e.printStackTrace()
                createdTimeRaw
            }
            val fee = fill.fee.toDouble().format(quoteCurrency)
            val price = fill.price.toDouble().format(quoteCurrency)
            val size = fill.size.toDouble().btcFormat()
            customView {
                linearLayout {
                    verticalLayout {
                        horizontalLayout(R.string.chart_history_side_label, fill.side).lparams(width = layoutWidth) {}
                        horizontalLayout(R.string.chart_history_size_label, size).lparams(width = layoutWidth) {}
                        horizontalLayout(R.string.chart_history_price_label, price).lparams(width = layoutWidth) {}
                        horizontalLayout(R.string.chart_history_fee_label, fee).lparams(width = layoutWidth) {}
                        horizontalLayout(R.string.chart_history_time_label, createdTime).lparams(width = layoutWidth) {}
                    }.lparams(width = matchParent) {leftMargin = dip(20) }
                }
            }
            positiveButton(R.string.popup_ok_btn) {  }
        }.show()
    }

    override fun onValueSelected(entry: Entry, h: Highlight) {
        val index = entry.x.toInt()
        if (candles.size > index) {
            txt_chart_price.text = entry.y.toDouble().format(quoteCurrency)
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
            txt_chart_price.text = account.product.priceForQuoteCurrency(quoteCurrency).format(quoteCurrency)
            setPercentChangeText(chartTimeSpan)
            chart_fragment_chart.highlightValues(arrayOf<Highlight>())
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
        view.performClick()
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
        println("ChartFragment - refresh - 1")
        val onFailure = { result: Result.Failure<String, FuelError> ->
            println("ChartFragment - refresh - onFailure")
            toast(resources.getString(R.string.error_generic_message, result.errorMessage))
            onComplete(false)
        }
        println("ChartFragment - refresh - 2")
        val prefs = Prefs(context!!)
        account?. let { account ->
            println("ChartFragment - refresh - 3")
            if (prefs.isLoggedIn) {
                println("ChartFragment - refresh - 4")
                /* Refresh does 2 things, it updates the chart, account info first
                 * then candles etc in mini refresh, while simultaneously updating history info
                */
                CBProApi.account(account.id).get( onFailure) { apiAccount ->
                    println("ChartFragment - refresh - 5")
                    if (lifecycle.isCreatedOrResumed) {
                        println("ChartFragment - refresh - 6")
                        var newBalance = account.balance
                        if (apiAccount != null) {
                            println("ChartFragment - refresh - 7")
                            newBalance = apiAccount.balance.toDoubleOrZero()
                            account.apiAccount = apiAccount
                        }
                        println("ChartFragment - refresh - 8")
                        txt_chart_account_balance.text = resources.getString(R.string.chart_balance_text, newBalance.btcFormat(), account.currency)
                        txt_chart_account_value.text = account.valueForQuoteCurrency(quoteCurrency).format(quoteCurrency)
                        miniRefresh(onFailure) {
                            println("ChartFragment - refresh - 9")
                            onComplete(true)
                        }
                    }
                }

                var filteredOrders: List<ApiOrder>? = null
                var filteredFills: List<ApiFill>? = null
                CBProApi.listOrders(productId = account.product.id).getAndStash(context!!, onFailure) { apiOrderList ->
                    println("ChartFragment - refresh - 10")
                    if (lifecycle.isCreatedOrResumed) {
                        println("ChartFragment - refresh - 11")
                        filteredOrders = apiOrderList.filter { it.product_id == account.product.id }
                        if (filteredOrders != null && filteredFills != null) {
                            println("ChartFragment - refresh - 12")
                            updateHistoryPagerAdapter(filteredOrders!!, filteredFills!!)
                            println("ChartFragment - refresh - 13")
                        }
                    }
                }
                CBProApi.fills(productId = account.product.id).getAndStash(context!!, onFailure) { apiFillList ->
                    println("ChartFragment - refresh - 14")
                    if (lifecycle.isCreatedOrResumed) {
                        println("ChartFragment - refresh - 15")
                        filteredFills = apiFillList.filter { it.product_id == account.product.id }
                        if (filteredOrders != null && filteredFills != null) {
                            println("ChartFragment - refresh - 16")
                            updateHistoryPagerAdapter(filteredOrders!!, filteredFills!!)
                            println("ChartFragment - refresh - 17")
                        }
                    }
                }
            } else {
                miniRefresh(onFailure) {
//                    account.balance = 0.0
                    println("ChartFragment - refresh - 18")
                    onComplete(true)
                }
            }
        }
    }

    private fun updateHistoryPagerAdapter(orderList: List<ApiOrder>, fillList: List<ApiFill>? = null) {
        (historyPager?.adapter as HistoryPagerAdapter).orders = orderList
        fillList?.let {
            (historyPager?.adapter as HistoryPagerAdapter).fills = it
        }
        (historyPager?.adapter as HistoryPagerAdapter).notifyDataSetChanged()
        //historyList.setHeightBasedOnChildren()
    }

    private fun miniRefresh(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        println("ChartFragment - miniRefresh - 1")
        val account = account
        if (account == null) {
            println("ChartFragment - miniRefresh - 2")
            onComplete()
            println("ChartFragment - miniRefresh - 3")
        } else {
            println("ChartFragment - miniRefresh - 4")
            val tradingPairTemp = tradingPair
            account.product.updateCandles(chartTimeSpan, tradingPairTemp, onFailure) { _ ->
                println("ChartFragment - miniRefresh - 5")
                if (lifecycle.isCreatedOrResumed) {
                    println("ChartFragment - miniRefresh - 6")
                    if (tradingPairTemp == tradingPair) {
                        println("ChartFragment - miniRefresh - 7")
                        candles = account.product.candlesForTimespan(chartTimeSpan, tradingPair)
                        tradingPair?.let {
                            println("ChartFragment - miniRefresh - 8")
                            CBProApi.ticker(it).get(onFailure) {
                                println("ChartFragment - miniRefresh - 9")
                                if (lifecycle.isCreatedOrResumed) {
                                    println("ChartFragment - miniRefresh - 10")
                                    val price = account.product.priceForQuoteCurrency(quoteCurrency)
                                    completeMiniRefresh(price, candles, onComplete)
                                }
                            }
                        } ?: run {
                            println("ChartFragment - miniRefresh - 12")
                            val price = candles.lastOrNull()?.close ?: 0.0
                            completeMiniRefresh(price, candles, onComplete)
                        }
                    } else {
                        println("ChartFragment - miniRefresh - 13")
                        val error = Result.Failure<String, FuelError>(FuelError(Exception()))
                        onFailure(error)
                    }
                }
            }
        }
    }

    private fun completeMiniRefresh(price: Double, candles: List<Candle>, onComplete: () -> Unit) {
        //complete mini refresh assumes account is not null
        println("ChartFragment - completeMiniRefresh - 1")
        txt_chart_price.text = price.format(quoteCurrency)
        txt_chart_account_value.text = account!!.valueForQuoteCurrency(quoteCurrency).format(quoteCurrency)
        chart_fragment_chart.addCandles(candles, account!!.currency)
        println("ChartFragment - completeMiniRefresh - 2")
        setPercentChangeText(chartTimeSpan)
        checkTimespanButton()
        println("ChartFragment - completeMiniRefresh - 3")
        onComplete()
    }
}
