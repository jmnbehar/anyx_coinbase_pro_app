package com.jmnbehar.anyx.Fragments.Main

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.baoyz.swipemenulistview.SwipeMenuCreator
import com.baoyz.swipemenulistview.SwipeMenuItem
import com.baoyz.swipemenulistview.SwipeMenuListView
import com.jmnbehar.anyx.Adapters.AlertListViewAdapter
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_alerts.view.*
import org.jetbrains.anko.support.v4.toast
import android.util.TypedValue
import com.jmnbehar.anyx.Activities.MainActivity


/**
 * Created by jmnbehar on 11/5/2017.
 */
class AlertsFragment : RefreshFragment() {

    lateinit private var inflater: LayoutInflater
    lateinit private var titleText: TextView

    lateinit private var currencyTabLayout: TabLayout

    lateinit private var priceEditText: EditText
    lateinit private var priceUnitText: TextView
    lateinit private var priceLabelText: TextView

    lateinit private var setButton: Button

    lateinit private var alertList: SwipeMenuListView
    lateinit private var alertAdapter: AlertListViewAdapter

    var currency = Currency.BTC

    companion object {
        lateinit var currency: Currency
        lateinit var prefs: Prefs
        var alerts: MutableSet<Alert> = mutableSetOf()

        fun newInstance(ctx: Context): AlertsFragment {
            prefs = Prefs(ctx)
            alerts = prefs.alerts.toMutableSet()
            return AlertsFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_alerts, container, false)

        this.inflater = inflater

        titleText = rootView.txt_alert_name

        currencyTabLayout = rootView.tabl_alerts_currency

        priceUnitText = rootView.txt_alert_price_unit
        priceEditText = rootView.etxt_alert_price

        setButton = rootView.btn_alert_set

        alertList = rootView.list_alerts

        titleText.text = currency.toString()

//        switchCurrency(this.currency)
        currencyTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when(tab.position) {
                    0 -> switchCurrency(Currency.BTC)
                    1 -> switchCurrency(Currency.ETH)
                    2 -> switchCurrency(Currency.BCH)
                    3 -> switchCurrency(Currency.LTC)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        priceUnitText.text = "USD"

        setButton.setOnClickListener { setAlert() }


        alertAdapter = AlertListViewAdapter(inflater, alerts.toList(), { alert ->
            deleteAlert(alert)
        })
        alertList.adapter = alertAdapter

        val swipeMenuCreator = SwipeMenuCreator { menu ->
            var deleteItem = SwipeMenuItem(context)
            deleteItem.background = ColorDrawable(Color.RED)
            deleteItem.width = dp2px(90)
            deleteItem.title = "Delete"
            menu.addMenuItem(deleteItem)
        }

        alertList.setMenuCreator(swipeMenuCreator)

        alertList.setOnMenuItemClickListener { position, _, index ->
            when (index) {
                0 -> {
                    val alertAtPos = alerts.toList()[position]
                    deleteAlert(alertAtPos)

                    alertAdapter.notifyDataSetChanged()
                    toast("Item deleted")
                }
            }
            true
        }
        alertList.setSwipeDirection(SwipeMenuListView.DIRECTION_LEFT)

        alertList.setHeightBasedOnChildren()

        return rootView
    }


    private fun dp2px(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
                resources.displayMetrics).toInt()
    }
    private fun deleteAlert(alert: Alert) {
        alerts.removeAlert(alert)
        prefs.alerts = alerts
        alertAdapter.alerts = alerts.toList()
        alertAdapter.notifyDataSetChanged()
        alertList.adapter = alertAdapter
    }

    private fun setAlert() {
        val price = priceEditText.text.toString().toDoubleOrZero()
        priceEditText.setText("", TextView.BufferType.EDITABLE)
        val productPrice = Account.forCurrency(currency)?.product?.price ?: 0.0
        val triggerIfAbove = price > productPrice
        val alert = Alert(price, currency, triggerIfAbove)
        alerts.add(alert)
        prefs.addAlert(alert)
        alertAdapter.alerts = alerts.toList()
        alertAdapter.notifyDataSetChanged()
        alertList.setHeightBasedOnChildren()
    }


    private fun switchCurrency(currency: Currency) {
        this.currency = currency
//        when (currency) {
//            Currency.BTC -> currencyTabLayout.
//            Currency.ETH -> radioButtonEth.isChecked = true
//            Currency.LTC -> radioButtonLtc.isChecked = true
//            Currency.BCH -> radioButtonLtc.isChecked = true
//            Currency.USD -> { }
//        }
    }

    override fun refresh(onComplete: () -> Unit) {
        alertAdapter.alerts = alerts.toList()
        alertAdapter.notifyDataSetChanged()
        alertList.adapter = alertAdapter

        (activity as MainActivity).updatePrices({ /* fail silently */ }, {
            (activity as MainActivity).loopThroughAlerts()
            onComplete()
        })
    }
}
