package com.anyexchange.anyx.fragments.main

import android.annotation.SuppressLint
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
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
import com.anyexchange.anyx.adapters.spinnerAdapters.TradingPairSpinnerAdapter
import com.anyexchange.anyx.classes.Currency
import com.github.mikephil.charting.data.CandleEntry
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

    private var highLabelTextView: TextView? = null
    private var highTextView: TextView? = null
    private var lowLabelTextView: TextView? = null
    private var lowTextView: TextView? = null

    private var openLabelTextView: TextView? = null
    private var openTextView: TextView? = null
    private var closeLabelTextView: TextView? = null
    private var volumeLabelTextView: TextView? = null
    private var volumeTextView: TextView? = null

    private var buyButton: Button? = null
    private var sellButton: Button? = null

    private var timespanRadioGroup: RadioGroup? = null

    private var historyTabLayout: TabLayout? = null

    private var tradeFragment: TradeFragment? = null

    private var blockRefresh = false
    private var didTouchTradingPairSpinner = false

    val timespan: Timespan
        get() = viewModel.timeSpan

    val chartStyle: ChartStyle
        get() = viewModel.chartStyle

    val tradingPair: TradingPair?
        get() = viewModel.tradingPair

    private val quoteCurrency: Currency
        get() = viewModel.tradingPair?.quoteCurrency ?: Currency.USD

    companion object {
        var account: Account = Account.dummyAccount

        var currency: Currency
            get() = account.currency
            set(value) {
                Account.forCurrency(value)?.let {
                    account = it
                }
            }
    }

    private lateinit var viewModel: ChartViewModel
    class ChartViewModel : ViewModel() {
        var timeSpan = Timespan.DAY
        var chartStyle = ChartStyle.Line
        var tradingPair: TradingPair? = null
    }

    @SuppressLint("ClickableViewAccessibility")
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
//        viewModel.timespan = Timespan.forLong(timespanLong)

        setupSwipeRefresh(rootView.swipe_refresh_layout as SwipeRefreshLayout)

        val tempAccount = account

        candles = tempAccount.product.candlesForTimespan(timespan, tradingPair)
        val currency = tempAccount.currency

        lineChart = rootView.chart_line_chart
        candleChart = rootView.chart_candle_chart

        val granularity = Candle.granularityForTimespan(timespan)
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

        highLabelTextView = rootView.txt_chart_high_label
        highTextView = rootView.txt_chart_high
        lowLabelTextView = rootView.txt_chart_low_label
        lowTextView = rootView.txt_chart_low

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            tickerTextView = rootView.txt_chart_ticker

            balanceTextView = rootView.txt_chart_account_balance
            valueTextView = rootView.txt_chart_account_value
            accountIcon = rootView.img_chart_account_icon

            historyTabLayout = rootView.history_tab_layout

            sellButton = rootView.btn_chart_sell
            historyPager = rootView.history_view_pager
        } else {
            openLabelTextView = rootView.txt_chart_open_label
            openTextView = rootView.txt_chart_open
            closeLabelTextView =  rootView.txt_chart_close_label

            volumeLabelTextView = rootView.txt_chart_volume_label
            volumeTextView = rootView.txt_chart_volume
        }

        context?.let {
            val prefs = Prefs(it)

            buyButton?.setOnClickListener {_ ->
                buySellButtonOnClick(prefs.isLoggedIn, TradeSide.BUY)
            }
            sellButton?.setOnClickListener { _ ->
                buySellButtonOnClick(prefs.isLoggedIn, TradeSide.SELL)
            }
            historyPager = rootView.history_view_pager

            val stashedFills: List<ApiFill> = prefs.getStashedFills(tempAccount.product.id)
            val stashedOrders: List<ApiOrder> = prefs.getStashedOrders(tempAccount.product.id)

            historyPager?.adapter = HistoryPagerAdapter(childFragmentManager, stashedOrders, stashedFills,
                    { order -> orderOnClick(order)}, { fill -> fillOnClick(fill) })
            historyPager?.setOnTouchListener(this)


            val tradingPairs: List<TradingPair> = if (tempAccount.product.tradingPairs.isNotEmpty()) {
                tempAccount.product.tradingPairs.sortedBy { tradingPair ->  tradingPair.quoteCurrency.orderValue }
            } else {
                listOf()
            }
            val tradingPairAdapter = TradingPairSpinnerAdapter(it, tradingPairs)
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
                        val tempTradingPairIndex = account.product.tradingPairs.indexOf(tradingPair)
                        viewModel.tradingPair = tradingPairSpinner?.selectedItem as? TradingPair
                        showProgressSpinner()
                        miniRefresh({ _ ->
                            toast(R.string.chart_update_error)
                            tradingPairSpinner?.setSelection(tempTradingPairIndex)
                            didTouchTradingPairSpinner = false

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

        timespanRadioGroup = rootView.rgroup_chart_timespans

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

    var blockNextAccountChange = false
    override fun onResume() {
        super.onResume()
        //TODO: reset trading pair and timespan

        val tradingPairs = account.product.tradingPairs.sortedBy { it.quoteCurrency.orderValue }
        val index = tradingPairs.indexOf(tradingPair)
        if (index != -1) {
            tradingPairSpinner?.setSelection(index)
        }

        txt_chart_name.text = currency.fullName
        setPercentChangeText(timespan)
        checkTimespanButton()
        updateChartStyle()
        highLabelTextView?.visibility = View.GONE
        highTextView?.visibility = View.GONE
        lowLabelTextView?.visibility = View.GONE
        lowTextView?.visibility = View.GONE

        openLabelTextView?.visibility = View.GONE
        openTextView?.visibility = View.GONE
        closeLabelTextView?.visibility = View.GONE
        volumeLabelTextView?.visibility = View.GONE
        volumeTextView?.visibility = View.GONE

        blockNextAccountChange = true
        showNavSpinner(currency, Currency.cryptoList) { selectedCurrency ->
            if (!blockNextAccountChange) {
                Account.forCurrency(selectedCurrency)?.let { tempAccount ->
                    account = tempAccount
                    switchAccount(tempAccount)
                }
            }
            blockNextAccountChange = false
        }

        if (currency.isFiat) {
            setButtonColors()
            System.out.println("Account not null")
            switchAccount(account)
        } else {
            val mainActivity = activity as? MainActivity
            val selectedCurrency = mainActivity?.spinnerNav?.selectedItem as? Currency
            currency = if (selectedCurrency != null) {
                selectedCurrency
            } else {
                System.out.println("Account reset to BTC")
                Currency.BTC
            }
            setButtonsAndBalanceText(account)
            switchAccount(account)
        }

        autoRefresh = Runnable {
            if (!blockRefresh) {
                miniRefresh({ }, { })
                handler.postDelayed(autoRefresh, (TimeInSeconds.halfMinute * 1000))
            }
        }
        handler.postDelayed(autoRefresh, (TimeInSeconds.halfMinute * 1000))
        dismissProgressSpinner()
        refresh { endRefresh() }
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
        //TODO: go directly to verify/login
        if (!isLoggedIn) {
            toast(R.string.toast_please_login_message)
        } else if (CBProApi.credentials?.isVerified == null) {
            toast(R.string.toast_please_verify_message)
        } else if (CBProApi.credentials?.isVerified == false) {
            toast(R.string.toast_missing_permissions_message)
        } else {
            if (tradeFragment == null) {
                tradeFragment = TradeFragment.newInstance(tradeSide)
            } else {
                tradeFragment?.tradeSide = tradeSide
            }
            (activity as? MainActivity)?.goToFragment(tradeFragment!!, FragmentType.TRADE.toString())
        }
    }

    private fun switchAccount(newAccount: Account) {
        account = newAccount
        blockRefresh = true
        didTouchTradingPairSpinner = false

        val price = newAccount.product.priceForQuoteCurrency(quoteCurrency)
        priceTextView?.text = price.format(quoteCurrency)

        context?.let { context ->
            val tradingPairs = account.product.tradingPairs
            val tradingPairSpinnerAdapter = TradingPairSpinnerAdapter(context, tradingPairs)
            tradingPairSpinner?.adapter = tradingPairSpinnerAdapter
            val relevantTradingPair = tradingPairs.find { it.quoteCurrency == tradingPair?.quoteCurrency }
            if (relevantTradingPair != null) {
                val index = tradingPairs.indexOf(relevantTradingPair)
                spinner_chart_trading_pair.setSelection(index)
                viewModel.tradingPair = relevantTradingPair
            } else {
                viewModel.tradingPair = tradingPairs.firstOrNull()
            }

            candles = newAccount.product.candlesForTimespan(timespan, tradingPair)
            val prefs = Prefs(context)
            val nowInSeconds = Calendar.getInstance().timeInSeconds()
            val wereFillsRecentlyUpdated = (CBProApi.fills.dateLastUpdated ?: 0 + TimeInSeconds.fiveMinutes > nowInSeconds)

            if (prefs.isLoggedIn && !wereFillsRecentlyUpdated) {
                showProgressSpinner()
                CBProApi.fills(apiInitData, productId = newAccount.product.id).getAndStash({
                    switchAccountCandlesCheck(newAccount, listOf())
                    dismissProgressSpinner()
                }) { apiFillList ->
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
    }
    private fun areCandlesUpToDate(timespan: Timespan): Boolean {
        val nowInSeconds = Calendar.getInstance().timeInSeconds()
        val candles = account.product.candlesForTimespan(timespan, tradingPair)
        val lastCandleTime = candles.lastOrNull()?.time?.toLong() ?: 0
        val nextCandleTime = lastCandleTime + Candle.granularityForTimespan(timespan)
        return candles.isNotEmpty() && (nextCandleTime > nowInSeconds)
    }

    private fun switchAccountCandlesCheck(account: Account, fills: List<ApiFill>) {
        if (areCandlesUpToDate(timespan)) {
            completeSwitchAccount(account, fills)
        } else {
            showProgressSpinner()
            miniRefresh({   //onFailure
                //Even if miniRefresh fails here, switch anyways
                dismissProgressSpinner()
                candles = account.product.candlesForTimespan(timespan, tradingPair)
                completeSwitchAccount(account, fills)
            }, {    //success
                dismissProgressSpinner()
                candles = account.product.candlesForTimespan(timespan, tradingPair)
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
            setPercentChangeText(timespan)
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

    private fun checkTimespanButton() {
        when (timespan) {
            Timespan.HOUR -> rbtn_chart_timespan_hour.isChecked = true
            Timespan.DAY ->  rbtn_chart_timespan_day.isChecked = true
            Timespan.WEEK -> rbtn_chart_timespan_week.isChecked = true
            Timespan.MONTH -> rbtn_chart_timespan_month.isChecked = true
            Timespan.YEAR -> rbtn_chart_timespan_year.isChecked = true
//            Timespan.ALL -> timespanButtonAll.isChecked = true
        }
    }

    private fun setChartTimespan(newTimespan: Timespan) {
        checkTimespanButton()
        val tempTimespan = timespan
        if (tempTimespan != newTimespan) {
            timespanRadioGroup?.isEnabled = false
            viewModel.timeSpan = newTimespan
            showProgressSpinner()
            if (areCandlesUpToDate(timespan)) {
                candles = account.product.candlesForTimespan(timespan, tradingPair)
                val price = account.product.priceForQuoteCurrency(quoteCurrency)
                completeMiniRefresh(price, candles) {
                    dismissProgressSpinner()
                    timespanRadioGroup?.isEnabled = true
                }
            } else {
                miniRefresh({
                    toast(R.string.chart_update_error)
                    viewModel.timeSpan = tempTimespan
                    timespanRadioGroup?.isEnabled = true
                    dismissProgressSpinner()
                }, {
                    checkTimespanButton()
                    dismissProgressSpinner()
                    timespanRadioGroup?.isEnabled = true
                })
            }
        }
    }


    private fun setPercentChangeText(timespan: Timespan) {
        val percentChange = account.product.percentChange(timespan, quoteCurrency)
        txt_chart_change_or_date.text = percentChange.percentFormat()
        txt_chart_change_or_date.textColor = if (percentChange >= 0) {
            Color.GREEN
        } else {
            Color.RED
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
        txt_chart_change_or_date.text = time?.toStringWithTimespan(timespan)
        context?.let {
            if (Prefs(it).isDarkModeOn) {
                txt_chart_change_or_date.textColor = Color.WHITE
            } else {
                txt_chart_change_or_date.textColor = Color.BLACK
            }
        }
        if (chartStyle == ChartStyle.Candle && entry is CandleEntry) {
            highLabelTextView?.visibility = View.VISIBLE
            highTextView?.visibility = View.VISIBLE
            lowLabelTextView?.visibility = View.VISIBLE
            lowTextView?.visibility = View.VISIBLE

            openLabelTextView?.visibility = View.VISIBLE
            openTextView?.visibility = View.VISIBLE
            closeLabelTextView?.visibility = View.VISIBLE



            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                highTextView?.typeface = Typeface.MONOSPACE
                lowTextView?.typeface = Typeface.MONOSPACE
                openTextView?.typeface = Typeface.MONOSPACE
                volumeTextView?.typeface = Typeface.MONOSPACE
                priceTextView?.typeface = Typeface.MONOSPACE
                //TODO: set percentChange text to monospace
            }

            if (quoteCurrency.isFiat) {
                highTextView?.text = entry.high.toDouble().fiatFormat(quoteCurrency)
                lowTextView?.text = entry.low.toDouble().fiatFormat(quoteCurrency)
                openTextView?.text = entry.open.toDouble().fiatFormat(quoteCurrency)
            } else {
                highTextView?.text = entry.high.toDouble().btcFormatShortened()
                lowTextView?.text = entry.low.toDouble().btcFormatShortened()
                openTextView?.text = entry.open.toDouble().btcFormatShortened()
            }

            //TODO: add volume to CandleEntry

            volumeLabelTextView?.visibility = View.GONE
            volumeTextView?.visibility = View.GONE
        }
    }

    override fun onNothingSelected() {
        priceTextView?.text = account.product.priceForQuoteCurrency(quoteCurrency).format(quoteCurrency)
        setPercentChangeText(timespan)
        when (chartStyle) {
            ChartStyle.Line -> lineChart?.highlightValues(arrayOf<Highlight>())
            ChartStyle.Candle -> candleChart?.highlightValues(arrayOf<Highlight>())
        }

        highLabelTextView?.visibility = View.GONE
        highTextView?.visibility = View.GONE
        lowLabelTextView?.visibility = View.GONE
        lowTextView?.visibility = View.GONE

        openLabelTextView?.visibility = View.GONE
        openTextView?.visibility = View.GONE
        closeLabelTextView?.visibility = View.GONE
        volumeLabelTextView?.visibility = View.GONE
        volumeTextView?.visibility = View.GONE

        //format these:
        highTextView?.typeface = Typeface.DEFAULT
        lowTextView?.typeface = Typeface.DEFAULT
        openTextView?.typeface = Typeface.DEFAULT
        volumeTextView?.typeface = Typeface.DEFAULT
        priceTextView?.typeface = Typeface.DEFAULT
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
            if (context != null) {
                toast(resources.getString(R.string.error_generic_message, result.errorMessage))
            }
            onComplete(false)
        }

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
            CBProApi.listOrders(apiInitData, productId = null).getAndStash(onFailure) { apiOrderList ->
                if (lifecycle.isCreatedOrResumed) {
                    filteredOrders = apiOrderList.filter { it.product_id == account.product.id }
                    if (filteredOrders != null && filteredFills != null) {
                        updateHistoryPagerAdapter(filteredOrders!!, filteredFills!!)
                    }
                }
            }
            CBProApi.fills(apiInitData, productId = account.product.id).getAndStash(onFailure) { apiFillList ->
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

    private fun updateHistoryPagerAdapter(orderList: List<ApiOrder>, fillList: List<ApiFill>? = null) {
        (historyPager?.adapter as? HistoryPagerAdapter)?.orders = orderList
        fillList?.let {
            (historyPager?.adapter as? HistoryPagerAdapter)?.fills = it
        }
        (historyPager?.adapter as? HistoryPagerAdapter)?.notifyDataSetChanged()
        //historyList.setHeightBasedOnChildren()
    }

    private fun miniRefresh(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        if (currency.isFiat) {
            onComplete()
        } else {
            val tradingPairTemp = tradingPair
            account.product.updateCandles(timespan, tradingPairTemp, apiInitData,  onFailure) { _ ->
                if (lifecycle.isCreatedOrResumed) {
                    if (tradingPairTemp == tradingPair) {
                        candles = account.product.candlesForTimespan(timespan, tradingPair)
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
        valueTextView?.text = account.valueForQuoteCurrency(quoteCurrency).format(quoteCurrency)

        addCandlesToActiveChart(candles, currency)
        setPercentChangeText(timespan)
        checkTimespanButton()
        onComplete()
    }

    private fun addCandlesToActiveChart(candles: List<Candle>, currency: Currency) {
        val granularity = Candle.granularityForTimespan(timespan)
        when (chartStyle) {
            ChartStyle.Line   ->   lineChart?.addCandles(candles, granularity, currency)
            ChartStyle.Candle -> candleChart?.addCandles(candles, currency)
        }
    }
}
