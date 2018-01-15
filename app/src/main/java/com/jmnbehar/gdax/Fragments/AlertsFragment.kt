package com.jmnbehar.gdax.Fragments

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.baoyz.swipemenulistview.SwipeMenuCreator
import com.baoyz.swipemenulistview.SwipeMenuItem
import com.baoyz.swipemenulistview.SwipeMenuListView
import com.jmnbehar.gdax.Adapters.AlertListViewAdapter
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_alerts.view.*
import org.jetbrains.anko.support.v4.toast
import android.util.TypedValue
import com.jmnbehar.gdax.Activities.MainActivity


/**
 * Created by jmnbehar on 11/5/2017.
 */
class AlertsFragment : RefreshFragment() {

    lateinit private var inflater: LayoutInflater
    lateinit private var titleText: TextView

    lateinit private var radioButtonBtc: RadioButton
    lateinit private var radioButtonEth: RadioButton
    lateinit private var radioButtonLtc: RadioButton

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

        radioButtonBtc = rootView.rbtn_alert_btc
        radioButtonEth = rootView.rbtn_alert_eth
        radioButtonLtc = rootView.rbtn_alert_ltc

        priceUnitText = rootView.txt_alert_price_unit
        priceEditText = rootView.etxt_alert_price

        setButton = rootView.btn_alert_set

        alertList = rootView.list_alerts

        titleText.text = currency.toString()

        switchCurrency()

        radioButtonBtc.setOnClickListener {
            switchCurrency(Currency.BTC)
        }
        radioButtonEth.setOnClickListener {
            switchCurrency(Currency.ETH)
        }
        radioButtonLtc.setOnClickListener {
            switchCurrency(Currency.LTC)
        }

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

        alertList.setOnMenuItemClickListener { position, menu, index ->
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


    private fun switchCurrency(currency: Currency = this.currency) {
        this.currency = currency

        when (currency) {
            Currency.BTC -> radioButtonBtc.isChecked = true
            Currency.ETH -> radioButtonEth.isChecked = true
            Currency.LTC -> radioButtonLtc.isChecked = true
        }

    }

    override fun refresh(onComplete: () -> Unit) {
        alertAdapter.alerts = alerts.toList()
        alertAdapter.notifyDataSetChanged()
        alertList.adapter = alertAdapter

        (activity as MainActivity).updatePrices {
            (activity as MainActivity).loopThroughAlerts()
            onComplete()
        }

    }

}
