package com.jmnbehar.anyx.Fragments.Main

import android.content.Context
import android.content.Intent
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
import android.view.MenuItem
import com.jmnbehar.anyx.Activities.LoginHelpActivity
import com.jmnbehar.anyx.Activities.MainActivity


/**
 * Created by jmnbehar on 11/5/2017.
 */
class AlertsFragment : RefreshFragment() {

    private lateinit var inflater: LayoutInflater
    private lateinit var titleText: TextView

    private lateinit var currencyTabLayout: TabLayout

    private lateinit var priceLabelText: TextView
    private lateinit var currentPriceText: TextView

    private lateinit var triggerLabelText: TextView
    private lateinit var priceEditText: EditText
    private lateinit var priceUnitText: TextView

    private lateinit var setButton: Button

    private lateinit var alertList: SwipeMenuListView
    private lateinit var alertAdapter: AlertListViewAdapter

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_alerts, container, false)

        this.inflater = inflater

        setupSwipeRefresh(rootView)

        titleText = rootView.txt_alert_name

        currencyTabLayout = rootView.tabl_alerts_currency

        priceLabelText = rootView.txt_alert_price_label
        currentPriceText = rootView.txt_alert_current_price

        triggerLabelText = rootView.txt_alert_trigger_label
        priceUnitText = rootView.txt_alert_price_unit
        priceEditText = rootView.etxt_alert_price

        setButton = rootView.btn_alert_set

        alertList = rootView.list_alerts

        titleText.text = "Alerts"

        switchCurrency(this.currency)
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

        priceUnitText.text = ""
        triggerLabelText.text = "New alert at: "

        setButton.setOnClickListener { setAlert() }
        setButton.text = "Set"


        alertAdapter = AlertListViewAdapter(inflater, alerts.toList(), { view, alert ->
            val popup = PopupMenu(activity, view)
            //Inflating the Popup using xml file
            popup.menuInflater.inflate(R.menu.alert_popup_menu, popup.menu)
            popup.setOnMenuItemClickListener { item: MenuItem? ->
                when (item?.itemId ?: R.id.delete_alert) {
                    R.id.delete_alert -> {
                        deleteAlert(alert)
                    }
                }
                true
            }
            popup.show()
        })

        alertList.adapter = alertAdapter

//        val swipeMenuCreator = SwipeMenuCreator { menu ->
//            var deleteItem = SwipeMenuItem(context)
//            deleteItem.background = ColorDrawable(Color.RED)
//            deleteItem.width = dp2px(90)
//            deleteItem.title = "Delete"
//            menu.addMenuItem(deleteItem)
//        }
//        alertList.setMenuCreator(swipeMenuCreator)
//        alertList.setOnMenuItemClickListener { position, _, index ->
//            when (index) {
//                0 -> {
//                    val alertAtPos = alerts.toList()[position]
//                    deleteAlert(alertAtPos)
//                    alertAdapter.notifyDataSetChanged()
//                    toast("Item deleted")
//                }
//            }
//            true
//        }
//        alertList.setSwipeDirection(SwipeMenuListView.DIRECTION_LEFT)

        //alertList.setHeightBasedOnChildren()

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
        val productPrice = Account.forCurrency(currency)?.product?.price ?: 0.0
        priceEditText.setText(productPrice.toString())
        val triggerIfAbove = price > productPrice
        val alert = Alert(price, currency, triggerIfAbove)
        alerts.add(alert)
        prefs.addAlert(alert)
        alertAdapter.alerts = alerts.toList()
        alertAdapter.notifyDataSetChanged()
    }


    private fun switchCurrency(currency: Currency) {
        this.currency = currency

        val price = Account.forCurrency(currency)?.product?.price
        if (price == null) {
            priceLabelText.text = ""
            currentPriceText.text = ""
        } else {
            priceLabelText.text = "Current " + currency.fullName + " price: "
            currentPriceText.text = price.fiatFormat()
        }
        priceEditText.setText(price.toString())
    }

    override fun refresh(onComplete: () -> Unit) {
        val prefs = Prefs(context!!)
        (activity as MainActivity).updatePrices({ /* fail silently */ }, {
            (activity as MainActivity).loopThroughAlerts()
            alerts = prefs.alerts.toMutableSet()
            alertAdapter.alerts = alerts.toList()
            alertAdapter.notifyDataSetChanged()
            onComplete()
        })
    }
}
