package com.anyexchange.anyx.fragments.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.anyexchange.anyx.adapters.AccountListViewAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.activities.MainActivity
import com.anyexchange.anyx.classes.Currency
import com.anyexchange.anyx.classes.api.CBProApi
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import kotlinx.android.synthetic.main.fragment_accounts.view.*
import org.jetbrains.anko.textColor

/**
 * Created by anyexchange on 11/5/2017.
 */
class AccountsFragment : RefreshFragment(), OnChartValueSelectedListener, OnChartGestureListener {
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

    companion object {
        val dummyTradingPair = TradingPair(Exchange.CBPro, Currency.USD, Currency.USD)
        var resetHomeListeners = { }
    }

    private val granularity = Candle.granularityForTimespan(Timespan.DAY)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_accounts, container, false)

        listView = rootView.list_accounts

        this.inflater = inflater
        setupSwipeRefresh(rootView.swipe_refresh_layout)

        lineChart = rootView.chart_accounts
        valueText = rootView.txt_accounts_total_value
        percentChangeText = rootView.txt_accounts_percent_change
        accountList = rootView.list_accounts
        titleText = rootView.account_text
        lockableScrollView = rootView.lockscroll_accounts

        val context = context
        if (context != null && Prefs(context).isLoggedIn) {
            rootView.layout_accounts_chart_info.visibility = View.VISIBLE
            accountTotalCandles = sumAccountCandles()
            rootView.txt_all_accounts_label.text = resources.getString(R.string.accounts_title)

            lineChart?.setOnChartValueSelectedListener(this)
            lineChart?.onChartGestureListener = this

            lineChart?.configure(accountTotalCandles, granularity, Timespan.DAY, dummyTradingPair, true, DefaultDragDirection.Horizontal) {
                swipeRefreshLayout?.isEnabled = false
                lockableScrollView?.scrollToTop(800)
                lockableScrollView?.scrollLocked = true
                HomeFragment.viewPager?.isLocked = true
            }

            accountList?.adapter = AccountListViewAdapter(context) { account: Account ->
                onClickAccount(account)
            }
            accountList?.setHeightBasedOnChildren()
            titleText?.visibility = View.GONE

        } else {
            accountList?.visibility = View.GONE
            lineChart?.visibility = View.GONE
            rootView.layout_accounts_chart_info.visibility = View.GONE
            //TODO: put a login button here
            titleText?.visibility = View.VISIBLE
            titleText?.text = resources.getString(R.string.accounts_logged_out_message)
            dismissProgressSpinner()
        }
        return rootView
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
            CBProApi.stablecoinConversion(apiInitData, amount, returnedTradingPair ?: tradingPair)
            dialogFragment.dismiss()
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
            percentChangeText?.text = resources.getString(R.string.accounts_percent_change_text,
                    percentChange.percentFormat(), sign, change.format(Account.defaultFiatCurrency))

            percentChangeText?.textColor = if (percentChange >= 0) {
                Color.GREEN
            } else {
                Color.RED
            }
        }
    }

    private fun setValueAndPercentChangeTexts() {
        val totalValue = Account.totalValue()
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
        val btcProduct = Product.map[Currency.BTC.id]
        if (btcProduct != null) {
            val accountTotalCandleList: MutableList<Candle> = mutableListOf()
            val fiatValue = Account.fiatAccounts.asSequence().map { it.defaultValue }.sum()
            for (i in 0..(btcProduct.defaultDayCandles.size - 1)) {
                var totalCandleValue = fiatValue
                val openTime = btcProduct.defaultDayCandles[i].openTime
                val closeTime = btcProduct.defaultDayCandles[i].closeTime
                for (product in Product.map.values) {
                    val accountCandleValue = if (product.defaultDayCandles.size > i) {
                        product.defaultDayCandles[i].close
                    } else {
                        product.defaultDayCandles.lastOrNull()?.close ?: 0.0
                    }
                    for (accountPair in product.accounts) {
                        totalCandleValue += (accountCandleValue * accountPair.value.balance)
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
        refresh(true, onComplete)
    }
    fun refresh(fullRefresh: Boolean, onComplete: (Boolean) -> Unit) {
        val onFailure: (result: Result.Failure<String, FuelError>) -> Unit = { result ->
            toast("Error: ${result.errorMessage}")
            onComplete(false)
        }
        swipeRefreshLayout?.isRefreshing = true
        val context = context
        if (context != null && Prefs(context).isLoggedIn) {
            CBProApi.accounts(apiInitData).updateAllAccounts(onFailure) {
                //Complete accounts refresh
                if (fullRefresh) {
                    refreshCompleteListener?.refreshComplete()
                }
                completeRefresh()
                onComplete(true)
            }
        } else {
            onComplete(true)
        }
    }

    private fun completeRefresh() {
        endRefresh()
        (accountList?.adapter as? AccountListViewAdapter)?.notifyDataSetChanged()
        accountList?.setHeightBasedOnChildren()

        accountTotalCandles = sumAccountCandles()
        setValueAndPercentChangeTexts()

        if (Account.totalValue() == 0.0) {
            lineChart?.visibility = View.GONE
        } else {
            lineChart?.visibility = View.VISIBLE
            //doesn't matter which fiat currency you use here:

            lineChart?.addCandles(accountTotalCandles, granularity, Timespan.DAY, dummyTradingPair)
        }
    }

    private var refreshCompleteListener: MarketFragment.RefreshCompleteListener? = null

    fun setRefreshListener(listener: MarketFragment.RefreshCompleteListener) {
        this.refreshCompleteListener = listener
    }


    override fun onResume() {
        super.onResume()
        resetHomeListeners()
        setValueAndPercentChangeTexts()

        refresh(false) { endRefresh() }
    }

}
