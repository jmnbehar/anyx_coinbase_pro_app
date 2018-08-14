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

    private var candleChart: PriceCandleChart? = null
    private var lineChart: PriceLineChart? = null

    private var activeChartType = ChartType.Line
        set(value) {
            when(value) {
                ChartType.Line -> {
                    lineChart?.visibility = View.VISIBLE
                    candleChart?.visibility = View.GONE
                }
                ChartType.Candle -> {
                    lineChart?.visibility = View.GONE
                    candleChart?.visibility = View.VISIBLE
                }
            }
            field = value
        }

    private var tradeFragment: TradeFragment? = null

    companion object {
        var account: Account? = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_chart, container, false)

        showDarkMode(rootView)

        this.inflater = inflater
        historyPager = rootView.history_view_pager

        setupSwipeRefresh(rootView.swipe_refresh_layout as SwipeRefreshLayout)

        val tempAccount = account
        val activity = activity!!
        if (tempAccount == null) {
            activity.supportFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
        } else {
            val prefs = Prefs(activity)
            candles = tempAccount.product.candlesForTimespan(chartTimeSpan, tradingPair)
            val currency = tempAccount.currency

            lineChart = rootView.chart_line_chart
            candleChart = rootView.chart_candle_chart

            lineChart?.configure(candles, currency, true, DefaultDragDirection.Horizontal) {
                swipeRefreshLayout?.isEnabled = false
            }
            lineChart?.setOnChartValueSelectedListener(this)
            lineChart?.onChartGestureListener = this

            candleChart?.configure(candles, currency, true, DefaultDragDirection.Horizontal) {
                swipeRefreshLayout?.isEnabled = false
            }
            candleChart?.setOnChartValueSelectedListener(this)
            candleChart?.onChartGestureListener = this

            rootView.btn_chart_buy.setOnClickListener {
                buySellButtonOnClick(prefs.isLoggedIn, tempAccount, TradeSide.BUY)
            }

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
//            timespanButtonAll.setText("ALL")
//            timespanButtonAll.setOnClickListener {
//                setChartTimespan(Timespan.ALL)
//            }

            val tradingPairs = if (tempAccount.product.tradingPairs.isNotEmpty()) {
                tempAccount.product.tradingPairs.sortedBy { it.quoteCurrency.orderValue }
            } else {
                listOf(tempAccount.id)
            }
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

            val stashedFills = prefs.getStashedFills(tempAccount.product.id)
            val stashedOrders = prefs.getStashedOrders(tempAccount.product.id)
            historyPager?.adapter = HistoryPagerAdapter(childFragmentManager, stashedOrders, stashedFills,
                    { order -> orderOnClick(order)}, { fill -> fillOnClick(fill) })
            historyPager?.setOnTouchListener(this)
        }
        return rootView
    }

    override fun onResume() {
        super.onResume()
        activeChartType = ChartType.Line
        checkTimespanButton()
        showNavSpinner(account?.currency) { selectedCurrency ->
            //            showProgressSpinner()
            account = Account.forCurrency(selectedCurrency)
            account?. let { account ->
                switchAccount(account)
            }
        }

        if (account != null) {
            setButtonColors()
            System.out.println("Account not null")
            switchAccount(account!!)
        } else {
            val mainActivity = activity as? MainActivity
            val selectedCurrency = mainActivity?.spinnerNav?.selectedItem as? Currency
            account = if (selectedCurrency != null) {
                System.out.println("Account retrieved from Spinner")
                Account.forCurrency(selectedCurrency)
            } else {
                System.out.println("Account reset to BTC")
                Account.forCurrency(Currency.BTC)
            }
            setButtonsAndBalanceText(account!!)
            switchAccount(account!!)
        }

        autoRefresh = Runnable {
            miniRefresh({ }, { })
            handler.postDelayed(autoRefresh, (TimeInSeconds.halfMinute * 1000))
        }
        handler.postDelayed(autoRefresh, (TimeInSeconds.halfMinute * 1000))
        dismissProgressSpinner()
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
            (activity as com.anyexchange.anyx.activities.MainActivity).goToFragment(tradeFragment!!, MainActivity.FragmentType.TRADE.toString())
        }
    }

    private fun switchAccount(newAccount: Account) {
        account = newAccount

        val activity = activity as com.anyexchange.anyx.activities.MainActivity
        val currency = newAccount.currency

        candles = newAccount.product.candlesForTimespan(chartTimeSpan, tradingPair)

        val price = newAccount.product.priceForQuoteCurrency(quoteCurrency)
        txt_chart_price.text = price.format(quoteCurrency)

        val tradingPairs = account?.product?.tradingPairs ?: listOf()
        val arrayAdapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, tradingPairs)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_chart_trading_pair.adapter = arrayAdapter

        val prefs = Prefs(activity)
        val stashedFills = prefs.getStashedFills(newAccount.product.id)
        val stashedOrders = prefs.getStashedOrders(newAccount.product.id)

        val now = Calendar.getInstance()
        val lastCandleTime = candles.lastOrNull()?.time?.toLong() ?: 0
        val nextCandleTime: Long = lastCandleTime + Candle.granularityForTimespan(chartTimeSpan)
        val areCandlesUpToDate = candles.isNotEmpty() && (nextCandleTime > now.timeInSeconds())

        if (areCandlesUpToDate) {
            addCandlesToActiveChart(candles, newAccount.currency)
            setPercentChangeText(chartTimeSpan)
            txt_chart_name.text = currency.fullName
            setButtonsAndBalanceText(newAccount)
            updateHistoryPagerAdapter(stashedOrders, stashedFills)
        } else {
            showProgressSpinner()
            miniRefresh({   //onFailure
                toast(R.string.error_message)
                dismissProgressSpinner()
            }, {    //success
                candles = newAccount.product.candlesForTimespan(chartTimeSpan, tradingPair)
                addCandlesToActiveChart(candles, newAccount.currency)
                setPercentChangeText(chartTimeSpan)
                txt_chart_name.text = currency.fullName
                setButtonsAndBalanceText(newAccount)
                dismissProgressSpinner()
                updateHistoryPagerAdapter(stashedOrders, stashedFills)
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
            when (activeChartType) {
                ChartType.Line -> lineChart?.highlightValues(arrayOf<Highlight>())
                ChartType.Candle -> candleChart?.highlightValues(arrayOf<Highlight>())
            }
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
        val onFailure = { result: Result.Failure<String, FuelError> ->
            toast(resources.getString(R.string.error_generic_message, result.errorMessage))
            onComplete(false)
        }
        val prefs = Prefs(context!!)
        account?. let { account ->
            if (prefs.isLoggedIn) {
                /* Refresh does 2 things, it updates the chart, account info first
                 * then candles etc in mini refresh, while simultaneously updating history info
                */
                CBProApi.account(account.id).get( onFailure) { apiAccount ->
                    if (lifecycle.isCreatedOrResumed) {
                        var newBalance = account.balance
                        if (apiAccount != null) {
                            newBalance = apiAccount.balance.toDoubleOrZero()
                            account.apiAccount = apiAccount
                        }
                        txt_chart_account_balance.text = resources.getString(R.string.chart_balance_text, newBalance.btcFormat(), account.currency)
                        txt_chart_account_value.text = account.valueForQuoteCurrency(quoteCurrency).format(quoteCurrency)
                        miniRefresh(onFailure) {
                            onComplete(true)
                        }
                    }
                }

                var filteredOrders: List<ApiOrder>? = null
                var filteredFills: List<ApiFill>? = null
                CBProApi.listOrders(productId = account.product.id).getAndStash(context!!, onFailure) { apiOrderList ->
                    if (lifecycle.isCreatedOrResumed) {
                        filteredOrders = apiOrderList.filter { it.product_id == account.product.id }
                        if (filteredOrders != null && filteredFills != null) {
                            updateHistoryPagerAdapter(filteredOrders!!, filteredFills!!)
                        }
                    }
                }
                CBProApi.fills(productId = account.product.id).getAndStash(context!!, onFailure) { apiFillList ->
                    if (lifecycle.isCreatedOrResumed) {
                        filteredFills = apiFillList.filter { it.product_id == account.product.id }
                        if (filteredOrders != null && filteredFills != null) {
                            updateHistoryPagerAdapter(filteredOrders!!, filteredFills!!)
                        }
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
        (historyPager?.adapter as HistoryPagerAdapter).orders = orderList
        fillList?.let {
            (historyPager?.adapter as HistoryPagerAdapter).fills = it
        }
        (historyPager?.adapter as HistoryPagerAdapter).notifyDataSetChanged()
        //historyList.setHeightBasedOnChildren()
    }

    private fun miniRefresh(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        val account = account
        if (account == null) {
            onComplete()
        } else {
            val tradingPairTemp = tradingPair
            account.product.updateCandles(chartTimeSpan, tradingPairTemp, onFailure) { _ ->
                if (lifecycle.isCreatedOrResumed) {
                    if (tradingPairTemp == tradingPair) {
                        candles = account.product.candlesForTimespan(chartTimeSpan, tradingPair)
                        tradingPair?.let {
                            CBProApi.ticker(it).get(onFailure) {
                                if (lifecycle.isCreatedOrResumed) {
                                    val price = account.product.priceForQuoteCurrency(quoteCurrency)
                                    completeMiniRefresh(price, candles, onComplete)
                                }
                            }
                        } ?: run {
                            val price = candles.lastOrNull()?.close ?: 0.0
                            completeMiniRefresh(price, candles, onComplete)
                        }
                    } else {
                        val error = Result.Failure<String, FuelError>(FuelError(Exception()))
                        onFailure(error)
                    }
                }
            }
        }
    }

    private fun completeMiniRefresh(price: Double, candles: List<Candle>, onComplete: () -> Unit) {
        //complete mini refresh assumes account is not null
        txt_chart_price.text = price.format(quoteCurrency)
        txt_chart_account_value.text = account!!.valueForQuoteCurrency(quoteCurrency).format(quoteCurrency)
        addCandlesToActiveChart(candles, account!!.currency)
        setPercentChangeText(chartTimeSpan)
        checkTimespanButton()
        onComplete()
    }

    private fun addCandlesToActiveChart(candles: List<Candle>, currency: Currency) {
        when (activeChartType) {
            ChartType.Line -> lineChart?.addCandles(candles, currency)
            ChartType.Candle -> candleChart?.addCandles(candles, currency)
        }
    }
}
