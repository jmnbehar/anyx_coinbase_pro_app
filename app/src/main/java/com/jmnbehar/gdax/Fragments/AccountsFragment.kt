package com.jmnbehar.gdax.Fragments

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
import com.jmnbehar.gdax.Activities.MainActivity
import com.jmnbehar.gdax.Adapters.AccountListViewAdapter
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_accounts.view.*
import org.jetbrains.anko.textColor

/**
 * Created by jmnbehar on 11/5/2017.
 */
class AccountsFragment : RefreshFragment(), OnChartValueSelectedListener, OnChartGestureListener {
    lateinit var listView: ListView
    lateinit var inflater: LayoutInflater
    private lateinit var lineChart: PriceChart
    private lateinit var valueText: TextView
    private lateinit var percentChangeText: TextView
    private lateinit var titleText: TextView
    private lateinit var accountList: ListView

    private var chartTimeSpan = TimeInSeconds.oneDay
    private var accountTotalCandles = listOf<Candle>()

    companion object {
        fun newInstance(): AccountsFragment {
            return AccountsFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_accounts, container, false)

        listView = rootView.list_accounts

        this.inflater = inflater
        setupSwipeRefresh(rootView)

        lineChart = rootView.chart_accounts
        valueText = rootView.txt_accounts_total_value
        percentChangeText = rootView.txt_accounts_percent_change
        accountList = rootView.list_accounts
        titleText = rootView.account_text

        if (GdaxApi.credentials != null) {
            rootView.layout_accounts_chart_info.visibility = View.VISIBLE
            accountTotalCandles = sumAccountCandles()
            rootView.txt_all_accounts_label.text = "All accounts"

            lineChart.setOnChartValueSelectedListener(this)
            lineChart.onChartGestureListener = this

            val selectGroup = lambda@ { account: Account ->
                (activity as MainActivity).goToChartFragment(account.currency)
            }
            accountList.adapter = AccountListViewAdapter(selectGroup)
            titleText.visibility = View.GONE

            refresh { /*done refreshing */ }
        } else {
            accountList.visibility = View.GONE
            lineChart.visibility = View.GONE
            rootView.layout_accounts_chart_info.visibility = View.GONE
            //TODO: put a login button here
            titleText.visibility = View.VISIBLE
            titleText.text = "Sign in to view account info"
        }

        return rootView
    }


    override fun onValueSelected(entry: Entry, h: Highlight) {
        valueText.text = entry.y.toDouble().fiatFormat()
        val candle = accountTotalCandles[entry.x.toInt()]
        percentChangeText.text = candle.time.toStringWithTimeRange(chartTimeSpan)
        val prefs = Prefs(context)

        percentChangeText.textColor = if (prefs.isDarkModeOn) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    }

    override fun onNothingSelected() {
        setValueAndPercentChangeTexts()
        lineChart.highlightValues(arrayOf<Highlight>())
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

    private fun setValueAndPercentChangeTexts() {
        val usdValue = Account.usdAccount?.value ?: 0.0
        val totalValue = Account.list.map { a -> a.value }.sum() + usdValue
        valueText.text = totalValue.fiatFormat()

        val open = if (accountTotalCandles.isNotEmpty()) {
            accountTotalCandles.first().open
        } else {
            0.0
        }
        setPercentChangeText(totalValue, open)
    }

    override fun onChartGestureStart(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) { }
    override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {
        swipeRefreshLayout?.isEnabled = true
        LockableViewPager.isLocked = false
        onNothingSelected()
    }
    override fun onChartLongPressed(me: MotionEvent) {
        swipeRefreshLayout?.isEnabled = false
        LockableViewPager.isLocked = true
    }
    override fun onChartDoubleTapped(me: MotionEvent) { }
    override fun onChartSingleTapped(me: MotionEvent) { }
    override fun onChartFling(me1: MotionEvent, me2: MotionEvent, velocityX: Float, velocityY: Float) { }
    override fun onChartScale(me: MotionEvent, scaleX: Float, scaleY: Float) { }
    override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) { }

    fun sumAccountCandles() : List<Candle> {
        val btcAccount = Account.btcAccount?.product
        if (btcAccount != null) {
            var accountTotalCandleList: MutableList<Candle> = mutableListOf()
            for (i in 0..(btcAccount.dayCandles.size - 1)) {
                var totalCandleValue = Account.usdAccount?.value ?: 0.0
                val time = btcAccount.dayCandles[i].time
                for (account in Account.list) {
                    val accountCandleValue = if (account.product.dayCandles.size > i) {
                        account.product.dayCandles[i].close
                    } else {
                        1.0
                    }
                    totalCandleValue += (accountCandleValue * account.balance)
                }
                val newCandle = Candle(time, 0.0, 0.0, totalCandleValue, totalCandleValue, 0.0)
                accountTotalCandleList.add(newCandle)
            }
            return accountTotalCandleList
        }
        return listOf()
    }


    override fun refresh(onComplete: () -> Unit) {
        val prefs = Prefs(context)
        if (prefs.isLoggedIn) {

            setValueAndPercentChangeTexts()

            lineChart.configure(accountTotalCandles, Currency.USD, true, PriceChart.DefaultDragDirection.Horizontal, TimeInSeconds.oneDay,true) {
                swipeRefreshLayout?.isEnabled = false
                LockableViewPager.isLocked = true
            }

            Account.updateAllAccounts({ onComplete() }) {
                (accountList.adapter as AccountListViewAdapter).notifyDataSetChanged()
                onComplete()
            }
        } else {
            onComplete()
        }
    }
}
