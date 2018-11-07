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
import com.anyexchange.anyx.classes.api.CBProApi
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

    private var chartTimeSpan = Timespan.DAY
    private var accountTotalCandles = listOf<Candle>()

    companion object {
        fun newInstance(): AccountsFragment {
            return AccountsFragment()
        }
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

        val context = context
        if (context != null && Prefs(context).isLoggedIn) {
            rootView.layout_accounts_chart_info.visibility = View.VISIBLE
            accountTotalCandles = sumAccountCandles()
            rootView.txt_all_accounts_label.text = resources.getString(R.string.accounts_title)

            lineChart?.setOnChartValueSelectedListener(this)
            lineChart?.onChartGestureListener = this

            lineChart?.configure(accountTotalCandles, granularity, Currency.USD, true, DefaultDragDirection.Horizontal) {
                swipeRefreshLayout?.isEnabled = false
                HomeFragment.viewPager?.isLocked = true
            }

            accountList?.adapter = AccountListViewAdapter(context) { account: Account ->
                (activity as com.anyexchange.anyx.activities.MainActivity).goToChartFragment(account.currency)
            }
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

    override fun onResume() {
        super.onResume()
        refresh { endRefresh() }
    }

    override fun onValueSelected(entry: Entry, h: Highlight) {
        valueText?.text = entry.y.toDouble().fiatFormat(Account.defaultFiatCurrency)
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
                    percentChange.percentFormat(), sign, change.fiatFormat(Account.defaultFiatCurrency))

            percentChangeText?.textColor = if (percentChange >= 0) {
                Color.GREEN
            } else {
                Color.RED
            }
        }
    }

    private fun setValueAndPercentChangeTexts() {
        val totalValue = Account.totalValue()
        valueText?.text = totalValue.fiatFormat(Account.defaultFiatCurrency)

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
        HomeFragment.viewPager?.isLocked = false
        onNothingSelected()
    }
    override fun onChartLongPressed(me: MotionEvent) {
        swipeRefreshLayout?.isEnabled = false
        HomeFragment.viewPager?.isLocked = false
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
        val context = context
        if (context != null && Prefs(context).isLoggedIn) {
            CBProApi.accounts(apiInitData).updateAllAccounts({ onComplete(false) }) {
                refreshComplete()
                onComplete(true)
            }
        } else {
            onComplete(true)
        }
    }

    fun refreshComplete() {
        (accountList?.adapter as? AccountListViewAdapter)?.notifyDataSetChanged()

        accountTotalCandles = sumAccountCandles()
        setValueAndPercentChangeTexts()

        if (Account.totalValue() == 0.0) {
            lineChart?.visibility = View.GONE
        } else {
            lineChart?.visibility = View.VISIBLE
            //doesn't matter which fiat currency you use here:
            lineChart?.addCandles(accountTotalCandles, granularity, Currency.USD)
        }
    }
}
