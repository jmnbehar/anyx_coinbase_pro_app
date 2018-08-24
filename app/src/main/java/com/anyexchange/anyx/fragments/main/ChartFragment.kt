package com.anyexchange.anyx.fragments.main

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.TabLayout
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
import android.widget.*
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
    private var tradingPairSpinner: Spinner? = null

    private var candles = listOf<Candle>()

    private var candleChart: PriceCandleChart? = null
    private var lineChart: PriceLineChart? = null

    private var tickerTextView: TextView? = null
    private var priceTextView: TextView? = null

    private var balanceTextView: TextView? = null
    private var valueTextView: TextView? = null
    private var accountIcon: ImageView? = null

    private var buyButton: Button? = null
    private var sellButton: Button? = null

    private var historyTabLayout: TabLayout? = null

    private var tradeFragment: TradeFragment? = null

    private var blockRefresh = false
    private var didTouchTradingPairSpinner = false

    val timeSpan: Timespan
        get() = viewModel.timeSpan

    val chartStyle: ChartStyle
        get() = viewModel.chartStyle

    val tradingPair: TradingPair?
        get() = viewModel.tradingPair

    private val quoteCurrency: Currency
        get() = viewModel.tradingPair?.quoteCurrency ?: Currency.USD

    companion object {
        var account: Account? = null
    }

    private lateinit var viewModel: ChartViewModel
    class ChartViewModel : ViewModel() {
        var timeSpan = Timespan.DAY
        var chartStyle = ChartStyle.Line
        var tradingPair: TradingPair? = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_chart, container, false)

        viewModel = ViewModelProviders.of(this).get(ChartViewModel::class.java)
        showDarkMode(rootView)

        setHasOptionsMenu(true)
        lockPortrait = false
        this.inflater = inflater

//        val tradingPairStr = savedInstanceState?.getString(CHART_TRADING_PAIR) ?: ""
//        val chartStyleStr  = savedInstanceState?.getString(CHART_STYLE) ?: ""
//        val timespanLong   = savedInstanceState?.getLong(CHART_TIMESPAN) ?: 0
//        viewModel.tradingPair = TradingPair(tradingPairStr)
//        viewModel.chartStyle = ChartStyle.forString(chartStyleStr)
//        viewModel.timeSpan = Timespan.forLong(timespanLong)

        setupSwipeRefresh(rootView.swipe_refresh_layout as SwipeRefreshLayout)

        val tempAccount = account

        candles = tempAccount?.product?.candlesForTimespan(timeSpan, tradingPair) ?: listOf()
        val currency = tempAccount?.currency ?: Currency.USD

        lineChart = rootView.chart_line_chart
        candleChart = rootView.chart_candle_chart

        val granularity = Candle.granularityForTimespan(timeSpan)
        lineChart?.configure(candles, granularity, currency, true, DefaultDragDirection.Horizontal) {
            swipeRefreshLayout?.isEnabled = false
        }
        lineChart?.setOnChartValueSelectedListener(this)
        lineChart?.onChartGestureListener = this

        candleChart?.configure(candles, currency, true, DefaultDragDirection.Horizontal) {
            swipeRefreshLayout?.isEnabled = false
        }
        candleChart?.setOnChartValueSelectedListener(this)
        candleChart?.onChartGestureListener = this

        priceTextView = rootView.txt_chart_price

        buyButton = rootView.btn_chart_buy
        context?.let {
            val prefs = Prefs(it)

            buyButton?.setOnClickListener {_ ->
                buySellButtonOnClick(prefs.isLoggedIn, TradeSide.BUY)
            }
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                tickerTextView = rootView.txt_chart_ticker

                balanceTextView = rootView.txt_chart_account_balance
                valueTextView = rootView.txt_chart_account_value
                accountIcon = rootView.img_chart_account_icon

                historyTabLayout = rootView.history_tab_layout

                sellButton = rootView.btn_chart_sell
                sellButton?.setOnClickListener { _ ->
                    buySellButtonOnClick(prefs.isLoggedIn, TradeSide.SELL)
                }
                historyPager = rootView.history_view_pager

                val stashedFills: List<ApiFill> = if (tempAccount != null) {
                    prefs.getStashedFills(tempAccount.product.id)
                } else { listOf() }
                val stashedOrders: List<ApiOrder> = if (tempAccount != null) {
                    prefs.getStashedOrders(tempAccount.product.id)
                } else { listOf() }
                historyPager?.adapter = HistoryPagerAdapter(childFragmentManager, stashedOrders, stashedFills,
                        { order -> orderOnClick(order)}, { fill -> fillOnClick(fill) })
                historyPager?.setOnTouchListener(this)
            }


            val tradingPairs = if (tempAccount != null && tempAccount.product.tradingPairs.isNotEmpty()) {
                tempAccount.product.tradingPairs.sortedBy { tradingPair ->  tradingPair.quoteCurrency.orderValue }
            } else {
                listOf(tempAccount?.id)
            }
            //TODO: don't use simple_spinner_item
            val tradingPairAdapter = ArrayAdapter(it, android.R.layout.simple_spinner_item, tradingPairs)
            tradingPairAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            tradingPairSpinner = rootView.spinner_chart_trading_pair
            tradingPairSpinner?.adapter = tradingPairAdapter
            val tradingPairListener = object : AdapterView.OnItemSelectedListener, View.OnTouchListener {
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    didTouchTradingPairSpinner = true
                    return false
                }
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (lifecycle.currentState == Lifecycle.State.RESUMED && didTouchTradingPairSpinner) {
                        viewModel.tradingPair = tradingPairSpinner?.selectedItem as? TradingPair
                        showProgressSpinner()
                        miniRefresh({ _ ->
                            toast(R.string.chart_update_error)
                            dismissProgressSpinner()
                        }, {
                            dismissProgressSpinner()
                        })
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
            tradingPairSpinner?.onItemSelectedListener = tradingPairListener
            tradingPairSpinner?.setOnTouchListener(tradingPairListener)
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

        return rootView
    }

    override fun onResume() {
        super.onResume()
        //TODO: reset trading pair and timespan

        val tradingPairs = account?.product?.tradingPairs?.sortedBy { it.quoteCurrency.orderValue }
        val index = tradingPairs?.indexOf(tradingPair) ?: -1
        if (index != -1) {
            tradingPairSpinner?.setSelection(index)
        }
        checkTimespanButton()
        updateChartStyle()

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
            if (!blockRefresh) {
                miniRefresh({ }, { })
                handler.postDelayed(autoRefresh, (TimeInSeconds.halfMinute * 1000))
            }
        }
        handler.postDelayed(autoRefresh, (TimeInSeconds.halfMinute * 1000))
        dismissProgressSpinner()
    }

    override fun onPause() {
        handler.removeCallbacks(autoRefresh)
        super.onPause()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val shouldShowOptions = lifecycle.isCreatedOrResumed
        menu.findItem(R.id.chart_style_line).isVisible = shouldShowOptions
        menu.findItem(R.id.chart_style_candle).isVisible = shouldShowOptions
        activity!!.invalidateOptionsMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.chart_style_line   -> viewModel.chartStyle = ChartStyle.Line
            R.id.chart_style_candle -> viewModel.chartStyle = ChartStyle.Candle
        }
        showProgressSpinner()
        miniRefresh({
            dismissProgressSpinner()
        }, {
            updateChartStyle()
            dismissProgressSpinner()
        })
        return false
    }

    private fun updateChartStyle() {
        when(chartStyle) {
            ChartStyle.Line -> {
                lineChart?.visibility = View.VISIBLE
                candleChart?.visibility = View.GONE
            }
            ChartStyle.Candle -> {
                lineChart?.visibility = View.GONE
                candleChart?.visibility = View.VISIBLE
            }
        }
    }

    private fun buySellButtonOnClick(isLoggedIn: Boolean, tradeSide: TradeSide) {
        if (!isLoggedIn) {
            toast(R.string.toast_please_login_message)
        } else if (CBProApi.credentials?.isVerified == null) {
            toast(R.string.toast_please_validate_message)
        } else if (CBProApi.credentials?.isVerified == false) {
            toast(R.string.toast_missing_permissions_message)
        } else {
            if (tradeFragment == null) {
                tradeFragment = TradeFragment.newInstance(tradeSide)
            } else {
                tradeFragment?.tradeSide = tradeSide
            }
            (activity as? MainActivity)?.goToFragment(tradeFragment!!, MainActivity.FragmentType.TRADE.toString())
        }
    }

    private fun switchAccount(newAccount: Account) {
        account = newAccount
        blockRefresh = true
        didTouchTradingPairSpinner = false
        //TODO: don't force unwrap:
        val activity = activity as MainActivity

        val price = newAccount.product.priceForQuoteCurrency(quoteCurrency)
        priceTextView?.text = price.format(quoteCurrency)

        val tradingPairs = account?.product?.tradingPairs ?: listOf()
        val arrayAdapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, tradingPairs)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_chart_trading_pair.adapter = arrayAdapter
        val relevantTradingPair = tradingPairs.find { it.quoteCurrency == tradingPair?.quoteCurrency }
        if (relevantTradingPair != null) {
            val index = tradingPairs.indexOf(relevantTradingPair)
            spinner_chart_trading_pair.setSelection(index)
            viewModel.tradingPair = relevantTradingPair
        } else {
            viewModel.tradingPair = tradingPairs.firstOrNull()
        }

        candles = newAccount.product.candlesForTimespan(timeSpan, tradingPair)
        val prefs = Prefs(activity)
        val nowInSeconds = Calendar.getInstance().timeInSeconds()
        val wereFillsRecentlyUpdated = (CBProApi.fills.dateLastUpdated ?: 0 + TimeInSeconds.fiveMinutes > nowInSeconds)

        if (!wereFillsRecentlyUpdated && context != null) {
            showProgressSpinner()
            CBProApi.fills(apiInitData, productId = newAccount.product.id).getAndStash(activity, { }) { apiFillList ->
                if (lifecycle.isCreatedOrResumed) {
                    switchAccountCandlesCheck(newAccount, apiFillList)
                    dismissProgressSpinner()
                }
            }
        } else {
            val stashedFills = prefs.getStashedFills(newAccount.product.id)
            switchAccountCandlesCheck(newAccount, stashedFills)
        }
    }
    private fun switchAccountCandlesCheck(account: Account, fills: List<ApiFill>) {
        val nowInSeconds = Calendar.getInstance().timeInSeconds()
        val lastCandleTime = candles.lastOrNull()?.time?.toLong() ?: 0
        val nextCandleTime = lastCandleTime + Candle.granularityForTimespan(timeSpan)
        val areCandlesUpToDate = candles.isNotEmpty() && (nextCandleTime > nowInSeconds)
        if (areCandlesUpToDate) {
            completeSwitchAccount(account, fills)
        } else {
            showProgressSpinner()
            miniRefresh({   //onFailure
                blockRefresh = false
                toast(R.string.error_message)
                dismissProgressSpinner()
            }, {    //success
                dismissProgressSpinner()
                candles = account.product.candlesForTimespan(timeSpan, tradingPair)
                completeSwitchAccount(account, fills)
            })
        }
    }
    private fun completeSwitchAccount(account: Account, fills: List<ApiFill>) {
        blockRefresh = false
        context?.let {
            val prefs = Prefs(it)
            val stashedOrders = prefs.getStashedOrders(account.product.id)
            addCandlesToActiveChart(candles, account.currency)
            setPercentChangeText(timeSpan)
            txt_chart_name.text = account.currency.fullName
            setButtonsAndBalanceText(account)
            updateHistoryPagerAdapter(stashedOrders, fills)
        }
    }

    private fun setButtonsAndBalanceText(account: Account) {
        context?.let {
            val currency = account.currency
            setButtonColors()
            val prefs = Prefs(it)
            if (prefs.isLoggedIn) {
                val value = account.valueForQuoteCurrency(quoteCurrency)
                tickerTextView?.text = resources.getString(R.string.chart_wallet_label, currency.toString())
                accountIcon?.setImageResource(currency.iconId)
                balanceTextView?.text = resources.getString(R.string.chart_balance_text, account.balance.btcFormat(), currency)
                valueTextView?.text = value.format(quoteCurrency)

                historyPager?.visibility = View.VISIBLE
            } else {
                tickerTextView?.visibility = View.GONE
                accountIcon?.visibility = View.GONE
                balanceTextView?.visibility = View.GONE
                valueTextView?.visibility = View.GONE

                historyPager?.visibility = View.INVISIBLE
            }
        }
    }

    private fun setButtonColors() {
        context?.let { context ->
            account?.currency?.let { currency ->
                val buttonColors = currency.colorStateList(context)
                buyButton?.backgroundTintList = buttonColors
                sellButton?.backgroundTintList = buttonColors
                val buttonTextColor = currency.buttonTextColor(context)
                buyButton?.textColor = buttonTextColor
                sellButton?.textColor = buttonTextColor
                val tabColor = currency.colorPrimary(context)
                historyTabLayout?.setSelectedTabIndicatorColor(tabColor)
            }
        }
    }

    private fun checkTimespanButton() {
        when (timeSpan) {
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
        viewModel.timeSpan = timespan
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
                CBProApi.cancelOrder(apiInitData, order.id).executeRequest({ _ -> }) { _ ->
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
        val time = entry.data as? Double
        priceTextView?.text = entry.y.toDouble().format(quoteCurrency)
        txt_chart_change_or_date.text = time?.toStringWithTimespan(timeSpan)
        context?.let {
            if (Prefs(it).isDarkModeOn) {
                txt_chart_change_or_date.textColor = Color.WHITE
            } else {
                txt_chart_change_or_date.textColor = Color.BLACK
            }
        }
    }

    override fun onNothingSelected() {
        val account = account
        if (account != null) {
            priceTextView?.text = account.product.priceForQuoteCurrency(quoteCurrency).format(quoteCurrency)
            setPercentChangeText(timeSpan)
            when (chartStyle) {
                ChartStyle.Line -> lineChart?.highlightValues(arrayOf<Highlight>())
                ChartStyle.Candle -> candleChart?.highlightValues(arrayOf<Highlight>())
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
        account?. let { account ->
            val context = context
            if (context != null && Prefs(context).isLoggedIn) {
                /* Refresh does 2 things, it updates the chart, account info first
                 * then candles etc in mini refresh, while simultaneously updating history info
                */
                CBProApi.account(apiInitData, account.id).get( onFailure) { apiAccount ->
                    if (lifecycle.isCreatedOrResumed) {
                        var newBalance = account.balance
                        if (apiAccount != null) {
                            newBalance = apiAccount.balance.toDoubleOrZero()
                            account.apiAccount = apiAccount
                        }
                        balanceTextView?.text = resources.getString(R.string.chart_balance_text, newBalance.btcFormat(), account.currency)
                        valueTextView?.text = account.valueForQuoteCurrency(quoteCurrency).format(quoteCurrency)
                        miniRefresh(onFailure) {
                            onComplete(true)
                        }
                    }
                }

                var filteredOrders: List<ApiOrder>? = null
                var filteredFills: List<ApiFill>? = null
                CBProApi.listOrders(apiInitData, productId = null).getAndStash(context, onFailure) { apiOrderList ->
                    if (lifecycle.isCreatedOrResumed) {
                        filteredOrders = apiOrderList.filter { it.product_id == account.product.id }
                        if (filteredOrders != null && filteredFills != null) {
                            updateHistoryPagerAdapter(filteredOrders!!, filteredFills!!)
                        }
                    }
                }
                CBProApi.fills(apiInitData, productId = account.product.id).getAndStash(context, onFailure) { apiFillList ->
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
        (historyPager?.adapter as? HistoryPagerAdapter)?.orders = orderList
        fillList?.let {
            (historyPager?.adapter as? HistoryPagerAdapter)?.fills = it
        }
        (historyPager?.adapter as? HistoryPagerAdapter)?.notifyDataSetChanged()
        //historyList.setHeightBasedOnChildren()
    }

    private fun miniRefresh(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        val account = account
        if (account == null) {
            onComplete()
        } else {
            val tradingPairTemp = tradingPair
            account.product.updateCandles(timeSpan, tradingPairTemp, apiInitData,  onFailure) { _ ->
                if (lifecycle.isCreatedOrResumed) {
                    if (tradingPairTemp == tradingPair) {
                        candles = account.product.candlesForTimespan(timeSpan, tradingPair)
                        tradingPair?.let {
                            CBProApi.ticker(apiInitData, it).get(onFailure) {_ ->
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
        priceTextView?.text = price.format(quoteCurrency)
        valueTextView?.text = account!!.valueForQuoteCurrency(quoteCurrency).format(quoteCurrency)

        addCandlesToActiveChart(candles, account!!.currency)
        setPercentChangeText(timeSpan)
        checkTimespanButton()
        onComplete()
    }

    private fun addCandlesToActiveChart(candles: List<Candle>, currency: Currency) {
        val granularity = Candle.granularityForTimespan(timeSpan)
        when (chartStyle) {
            ChartStyle.Line   ->   lineChart?.addCandles(candles, granularity, currency)
            ChartStyle.Candle -> candleChart?.addCandles(candles, currency)
        }
    }
}
