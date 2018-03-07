package com.jmnbehar.anyx.Fragments.Main

import android.graphics.Color
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import com.github.kittinunf.result.Result
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jmnbehar.anyx.Activities.MainActivity
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_chart.view.*
import com.github.kittinunf.fuel.core.FuelError
import com.github.mikephil.charting.listener.ChartTouchListener
import org.jetbrains.anko.*
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast
import android.view.MotionEvent
import android.widget.ArrayAdapter
import android.widget.RadioButton
import com.jmnbehar.anyx.Adapters.HistoryPagerAdapter
import java.text.ParseException
import java.text.SimpleDateFormat

/**
 * Created by jmnbehar on 11/5/2017.
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
    private lateinit var timespanButtonAll: RadioButton

    private var chartTimeSpan = Timespan.DAY
    private var candles = listOf<Candle>()

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
        timespanButtonAll = rootView.rbtn_chart_timespan_all

        val account = account
        val activity = activity!!
        if (account == null) {
            activity.supportFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
        } else {
            val prefs = Prefs(activity)

            checkTimespanButton()
            candles = account.product.candlesForTimespan(chartTimeSpan)
            val currency = account.currency
            setupSwipeRefresh(rootView)

            historyPager = rootView.history_view_pager
            lineChart = rootView.chart
            lineChart.configure(candles, currency, true, PriceChart.DefaultDragDirection.Horizontal,  chartTimeSpan) {
                swipeRefreshLayout?.isEnabled = false
            }
            lineChart.setOnChartValueSelectedListener(this)
            lineChart.onChartGestureListener = this

            val price = account.product.price
            priceText.text = price.fiatFormat()

            setPercentChangeText(price, candles.first().open)

            nameText.text = currency.fullName

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

            val buyButton = rootView.btn_chart_buy
            val sellButton = rootView.btn_chart_sell

            val buttonColors = currency.colorStateList(activity)
            buyButton.backgroundTintList = buttonColors
            sellButton.backgroundTintList = buttonColors
            val buttonTextColor = currency.buttonTextColor(activity)
            buyButton.textColor = buttonTextColor
            sellButton.textColor = buttonTextColor

            //TODO: send over more info
            buyButton.setOnClickListener {
                if (GdaxApi.credentials?.isValidated == null) {
                    toast("Validate your account in Settings to buy or sell $currency")
                } else if (GdaxApi.credentials?.isValidated == false) {
                    toast("Please use an API Key with all permissions.")
                } else if (!prefs.isLoggedIn) {
                    toast("Please log in to buy or sell $currency")
                } else {
                    (activity as MainActivity).goToFragment(TradeFragment.newInstance(account, TradeSide.BUY), "Trade: Buy")
                }
            }

            sellButton.setOnClickListener {
                if (GdaxApi.credentials?.isValidated == null) {
                    toast("Validate your account in Settings to buy or sell $currency")
                } else if (GdaxApi.credentials?.isValidated == false) {
                    toast("Please use an API Key with all permissions.")
                } else if (!prefs.isLoggedIn) {
                    toast("Please log in to buy or sell $currency")
                } else {
                    (activity as MainActivity).goToFragment(TradeFragment.newInstance(account, TradeSide.SELL), "Trade: Sell")
                }
            }

            timespanButtonHour.setText("1H")
            timespanButtonHour.setOnClickListener {
                setChartTimespan(Timespan.HOUR)
            }
            timespanButtonDay.setText("1D")
            timespanButtonDay.setOnClickListener {
                setChartTimespan(Timespan.DAY)
            }
            timespanButtonWeek.setText("1W")
            timespanButtonWeek.setOnClickListener {
                setChartTimespan(Timespan.WEEK)
            }
            timespanButtonMonth.setText("1M")
            timespanButtonMonth.setOnClickListener {
                setChartTimespan(Timespan.MONTH)
            }
            timespanButtonYear.setText("1Y")
            timespanButtonYear.setOnClickListener {
                setChartTimespan(Timespan.YEAR)
            }
            timespanButtonAll.setText("ALL")
            timespanButtonAll.setOnClickListener {
                setChartTimespan(Timespan.ALL)
            }

            val stashedFills = prefs.getStashedFills(account.product.id)
            val stashedOrders = prefs.getStashedOrders(account.product.id)
            historyPager.adapter = HistoryPagerAdapter(childFragmentManager, stashedOrders, stashedFills,
                    { order -> orderOnClick(order)}, { fill -> fillOnClick(fill) })
            historyPager.setOnTouchListener(this)
            val historyTabList = rootView.history_tab_layout
            val color = currency.colorPrimary(activity)
            historyTabList.setSelectedTabIndicatorColor(color)
        }
        return rootView
    }

    override fun onResume() {
        super.onResume()
        val mainActivity = (activity as MainActivity)
        mainActivity.spinnerNav.background.colorFilter = mainActivity.defaultSpinnerColorFilter
        mainActivity.spinnerNav.isEnabled = true
        val spinnerStrings = Account.list.map { account -> account.currency.fullName }.toList()
        mainActivity.spinnerNav.adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, spinnerStrings)
        autoRefresh = Runnable {
            miniRefresh({ }, { })
            handler.postDelayed(autoRefresh, (TimeInSeconds.halfMinute * 1000))
        }
        handler.postDelayed(autoRefresh, (TimeInSeconds.halfMinute * 1000))
    }

    override fun onPause() {
        handler.removeCallbacks(autoRefresh)
        super.onPause()
    }

    private fun checkTimespanButton() {
        val lifetimeInSeconds = account?.currency?.lifetimeInSeconds ?: 0.0
        when (chartTimeSpan) {
            Timespan.HOUR -> timespanButtonHour.isChecked = true
            Timespan.DAY ->  timespanButtonDay.isChecked = true
            Timespan.WEEK -> timespanButtonWeek.isChecked = true
            Timespan.MONTH -> timespanButtonMonth.isChecked = true
            Timespan.YEAR -> timespanButtonYear.isChecked = true
            Timespan.ALL -> timespanButtonAll.isChecked = true
        }
    }

    private fun setChartTimespan(timespan: Timespan) {
      //  MainActivity.progressDialog?.show()
        checkTimespanButton()
        chartTimeSpan = timespan
        miniRefresh({
            toast("Error updating chart time")
            MainActivity.progressDialog?.dismiss()
        }, {
            checkTimespanButton()
            MainActivity.progressDialog?.dismiss()
        })
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

    private fun orderOnClick(order: ApiOrder) {
        alert {
            title = "Order"

            val layoutWidth = 1000
            val createdTimeRaw = order.created_at
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'")
            val createdTime = try {
                val date = format.parse(createdTimeRaw)
//                System.out.println(date)
                val outputFormat = SimpleDateFormat("II: mm MM/dd/yyyy")
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
                val outputFormat = SimpleDateFormat("hh:mm, MM/dd/yyyy")
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
        priceText.text = entry.y.toDouble().fiatFormat()
        account?. let { account ->
            val candle = candles[entry.x.toInt()]
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
            val price = account.product.price
            val open = candles.first().open
            priceText.text = price.fiatFormat()
            setPercentChangeText(price, open)
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

    override fun refresh(onComplete: () -> Unit) {
        val gson = Gson()
        val onFailure = { result: Result.Failure<String, FuelError> ->
            toast("Error!: ${result.error}")
            println("error!" )}
        val prefs = Prefs(context!!)
        account?. let { account ->
            if (prefs.isLoggedIn) {
                GdaxApi.account(account.id).executeRequest(onFailure) { result ->
                    val apiAccount: ApiAccount = gson.fromJson(result.value, object : TypeToken<ApiAccount>() {}.type)
                    val newBalance = apiAccount.balance.toDoubleOrZero()
                    balanceText.text = newBalance.btcFormat() + " " + account.currency
                    valueText.text = account.value.fiatFormat()
                    miniRefresh(onFailure) {
                        account.updateAccount(newBalance, account.product.price)
                        onComplete()
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
                    account.updateAccount(0.0, account.product?.price)
                    onComplete()
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
        account?. let { account ->
            account.product.updateCandles(chartTimeSpan, onFailure, { _ ->
                candles = account.product.candlesForTimespan(chartTimeSpan)
                lineChart.addCandles(candles, account.currency, chartTimeSpan)
                setPercentChangeText(account.product.price, candles.firstOrNull()?.open ?: 0.0)
                checkTimespanButton()

                GdaxApi.ticker(account.product.id).executeRequest(onFailure) { result ->
                    val ticker: ApiTicker = Gson().fromJson(result.value, object : TypeToken<ApiTicker>() {}.type)
                    val price = ticker.price.toDoubleOrNull()
                    if (price != null) {
                        account.product.price = price
                    }

                    priceText.text = account.product.price.fiatFormat()
                    valueText.text = account.value.fiatFormat()

                    setPercentChangeText(account.product.price, candles.firstOrNull()?.open ?: 0.0)
                    onComplete()
                }
            })
        }
    }
}
