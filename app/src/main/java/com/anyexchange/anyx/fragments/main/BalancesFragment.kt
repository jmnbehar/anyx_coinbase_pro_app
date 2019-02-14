package com.anyexchange.anyx.fragments.main

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.anyexchange.anyx.adapters.BalanceListViewAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.views.LockableScrollView
import com.anyexchange.anyx.activities.MainActivity
import com.anyexchange.anyx.api.AnyApi
import com.anyexchange.anyx.classes.Currency
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import kotlinx.android.synthetic.main.fragment_balances.view.*
import org.jetbrains.anko.textColor

/**
 * Created by anyexchange on 11/5/2017.
 */
class BalancesFragment : RefreshFragment(), OnChartValueSelectedListener, OnChartGestureListener {
    private var listView: ListView? = null
    //Consider deleting this:
    lateinit var inflater: LayoutInflater
    private var lineChart: PriceLineChart? = null
    private var valueText: TextView? = null
    private var percentChangeText: TextView? = null
    private var titleText: TextView? = null
    private var accountList: ListView? = null
    private var lockableScrollView: LockableScrollView? = null

    private var chartTimeSpan = Timespan.DAY
    private var accountTotalCandles = listOf<Candle>()

    private lateinit var viewModel: BalancesViewModel
    class BalancesViewModel : ViewModel() {
        var activeExchange: Exchange? = null
    }
    companion object {
        val dummyTradingPair = TradingPair(Exchange.CBPro, Currency.USD, Currency.USD)
        var resetHomeListeners = { }
    }

    private val granularity = Candle.granularityForTimespan(Timespan.DAY)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_balances, container, false)

        listView = rootView.list_balances

        this.inflater = inflater
        setupSwipeRefresh(rootView.swipe_refresh_layout)
        viewModel = ViewModelProviders.of(this).get(BalancesViewModel::class.java)

        lineChart = rootView.chart_balances
        valueText = rootView.txt_balances_total_value
        percentChangeText = rootView.txt_balances_percent_change
        accountList = rootView.list_balances
        titleText = rootView.txt_balances_title
        lockableScrollView = rootView.lockscroll_balances
        setHasOptionsMenu(true)

        val context = context
        if (context != null && Exchange.isAnyLoggedIn()) {
            rootView.layout_balances_chart_info.visibility = View.VISIBLE
            accountTotalCandles = sumAccountCandles()
            rootView.txt_all_balances_label.text = resources.getString(R.string.balances_title)

            lineChart?.setOnChartValueSelectedListener(this)
            lineChart?.onChartGestureListener = this

            lineChart?.configure(accountTotalCandles, granularity, Timespan.DAY, dummyTradingPair, true, DefaultDragDirection.Horizontal) {
                swipeRefreshLayout?.isEnabled = false
                lockableScrollView?.scrollToTop(800)
                lockableScrollView?.scrollLocked = true
                HomeFragment.viewPager?.isLocked = true
            }

            accountList?.adapter = BalanceListViewAdapter(context, viewModel.activeExchange)

            accountList?.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
                val account = (listView?.adapter as BalanceListViewAdapter).sortedAccountList[pos]
                onClickAccount(account)
            }
            accountList?.setHeightBasedOnChildren()
            titleText?.visibility = View.GONE

        } else {
            accountList?.visibility = View.GONE
            lineChart?.visibility = View.GONE
            rootView.layout_balances_chart_info.visibility = View.GONE
            //TODO: put a login button here
            titleText?.visibility = View.VISIBLE
            titleText?.text = resources.getString(R.string.balances_logged_out_message)
            dismissProgressSpinner()
        }
        return rootView
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.setGroupVisible(R.id.group_balances, true)
    }


    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater?.inflate(R.menu.balances_menu, menu)
        setOptionsMenuTextColor(menu)

        when (viewModel.activeExchange){
            null             -> menu?.findItem(R.id.balances_show_all)?.isChecked = true
            Exchange.Binance -> menu?.findItem(R.id.balances_filter_binance)?.isChecked = true
            Exchange.CBPro   -> menu?.findItem(R.id.balances_filter_cbpro)?.isChecked = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.balances_show_all       -> viewModel.activeExchange = null
            R.id.balances_filter_binance -> viewModel.activeExchange = Exchange.Binance
            R.id.balances_filter_cbpro   -> viewModel.activeExchange = Exchange.CBPro
        }
        (accountList?.adapter as BalanceListViewAdapter).exchange = viewModel.activeExchange
        completeRefresh()
        return false
    }

    private fun onClickAccount(account: Account) {
        when (account.currency.type) {
            Currency.Type.CRYPTO -> {
                (activity as MainActivity).goToChartFragment(account.currency)
            }
            Currency.Type.STABLECOIN -> {
                val relevantFiat = account.currency.relevantFiat ?: account.currency
                val tradingPair = TradingPair(account.exchange, account.currency, relevantFiat)
                if (StablecoinConversionFragment.supportedTradingPairs.contains(tradingPair)) {
                    showStablecoinConversionDialog(tradingPair)
                }
            }
            Currency.Type.FIAT -> {
                val relevantFiat = account.currency.relevantStableCoin ?: account.currency
                val tradingPair = TradingPair(account.exchange, account.currency, relevantFiat)
                if (StablecoinConversionFragment.supportedTradingPairs.contains(tradingPair)) {
                    showStablecoinConversionDialog(tradingPair)
                }
            }
        }
    }

    private fun showStablecoinConversionDialog(tradingPair: TradingPair) {
        val dialogFragment = StablecoinConversionFragment()
        dialogFragment.setInfo(tradingPair) { returnedTradingPair, amount ->
            val finalTradingPair = returnedTradingPair ?: tradingPair
            AnyApi(apiInitData).stablecoinDirectConversion(finalTradingPair, amount, {
                toast(resources.getString(R.string.error_generic_message, it.errorMessage))
                dialogFragment.dismiss()
            }, {
                refresh { dialogFragment.dismiss() }
            })
        }
        dialogFragment.showNow(fragmentManager, "stablecoinConversionDialog")
    }

    override fun onValueSelected(entry: Entry, h: Highlight) {
        valueText?.text = entry.y.toDouble().format(Account.defaultFiatCurrency)
        if (accountTotalCandles.size > entry.x) {
            val candle = accountTotalCandles[entry.x.toInt()]

            var timeString = candle.closeTime.toStringWithTimespan(chartTimeSpan)
            timeString = timeString.replace(" ", "\n")
            percentChangeText?.text = timeString
        }
        context?.let {
            percentChangeText?.textColor = if (Prefs(it).isDarkModeOn) {
                Color.WHITE
            } else {
                Color.BLACK
            }
        }
    }

    override fun onNothingSelected() {
        setValueAndPercentChangeTexts()
        lineChart?.highlightValues(arrayOf<Highlight>())
    }

    private fun setPercentChangeText(price: Double, open: Double) {
        val change = price - open
        val weightedChange: Double = (change / open)
        val percentChange: Double = weightedChange * 100.0
        val sign = if (change >= 0) { "+" } else { "" }
        context?.let {
            percentChangeText?.text = resources.getString(R.string.balances_percent_change_text,
                    percentChange.percentFormat(), sign, change.format(Account.defaultFiatCurrency))

            percentChangeText?.textColor = if (percentChange >= 0) {
                Color.GREEN
            } else {
                Color.RED
            }
        }
    }

    private fun setValueAndPercentChangeTexts() {
        val totalValue = Account.totalValue(viewModel.activeExchange)
        valueText?.text = totalValue.format(Account.defaultFiatCurrency)

        val open = if (accountTotalCandles.isNotEmpty()) {
            accountTotalCandles.first().close
        } else {
            0.0
        }
        if (totalValue == 0.0) {
            valueText?.visibility = View.GONE
            percentChangeText?.visibility = View.GONE
        } else {
            valueText?.visibility = View.VISIBLE
            percentChangeText?.visibility = View.VISIBLE
            setPercentChangeText(totalValue, open)
        }
    }

    override fun onChartGestureStart(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) { }
    override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {
        swipeRefreshLayout?.isEnabled = true
        lockableScrollView?.scrollLocked = false
        HomeFragment.viewPager?.isLocked = false
        onNothingSelected()
    }
    override fun onChartLongPressed(me: MotionEvent) {
        swipeRefreshLayout?.isEnabled = false
        lockableScrollView?.scrollLocked = true
        HomeFragment.viewPager?.isLocked = true
    }
    override fun onChartDoubleTapped(me: MotionEvent) { }
    override fun onChartSingleTapped(me: MotionEvent) { }
    override fun onChartFling(me1: MotionEvent, me2: MotionEvent, velocityX: Float, velocityY: Float) { }
    override fun onChartScale(me: MotionEvent, scaleX: Float, scaleY: Float) { }
    override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) { }

    private fun sumAccountCandles() : List<Candle> {
        //Refactor:

        val ownedProducts = Product.map.values.filter { product ->
            product.accounts.values.any { it.balance > 0 }
        }
        val btcProduct = Product.map[Currency.BTC.id]
        if (btcProduct != null) {
            val accountTotalCandleList: MutableList<Candle> = mutableListOf()
            val filteredFiatAccounts = Account.fiatAccounts.filter { it.exchange == (viewModel.activeExchange ?: it.exchange) }.asSequence()
            val fiatValue = filteredFiatAccounts.map { it.defaultValue }.sum()
            for (i in 0..(btcProduct.defaultDayCandles.size - 1)) {
                var totalCandleValue = fiatValue
                val openTime = btcProduct.defaultDayCandles[i].openTime
                val closeTime = btcProduct.defaultDayCandles[i].closeTime
                for (product in ownedProducts) {
                    val accountCandleValue = if (product.defaultDayCandles.size > i) {
                        product.defaultDayCandles[i].close
                    } else {
                        product.defaultDayCandles.lastOrNull()?.close ?: 0.0
                    }
                    when (viewModel.activeExchange) {
                        null -> {
                            for (accountPair in product.accounts) {
                                totalCandleValue += (accountCandleValue * accountPair.value.balance)
                            }
                        }
                        else -> {
                            totalCandleValue += (accountCandleValue * (product.accounts[viewModel.activeExchange!!]?.balance ?: 0.0))
                        }
                    }
                }

                val newCandle = Candle(openTime, closeTime, 0.0, 0.0, totalCandleValue, totalCandleValue, 0.0)
                accountTotalCandleList.add(newCandle)
            }
            return accountTotalCandleList
        }
        return listOf()
    }


    override fun refresh(onComplete: (Boolean) -> Unit) {
        val onFailure: (result: Result.Failure<String, FuelError>) -> Unit = { result ->
            toast("Error: ${result.errorMessage}")
            onComplete(false)
        }
        swipeRefreshLayout?.isRefreshing = true
        val context = context
        if (context != null && Exchange.isAnyLoggedIn()) {
            AnyApi(apiInitData).getAllAccounts(onFailure) {
                completeRefresh()
                onComplete(true)
            }
        } else {
            onComplete(true)
        }
    }

    private fun completeRefresh() {
        endRefresh()
        (accountList?.adapter as? BalanceListViewAdapter)?.notifyDataSetChanged()
        accountList?.setHeightBasedOnChildren()

        accountTotalCandles = sumAccountCandles()
        setValueAndPercentChangeTexts()

        if (Account.totalValue(viewModel.activeExchange) == 0.0) {
            lineChart?.visibility = View.GONE
        } else {
            lineChart?.visibility = View.VISIBLE
            //doesn't matter which fiat currency you use here:

            lineChart?.addCandles(accountTotalCandles, granularity, Timespan.DAY, dummyTradingPair)
        }
    }


    override fun onResume() {
        shouldHideSpinner = false
        super.onResume()

        (activity as MainActivity).navSpinner.selectedItem = null

        resetHomeListeners()
        setValueAndPercentChangeTexts()

        refresh { endRefresh() }
    }

}
