package com.anyexchange.anyx.fragments.main

import android.content.Context
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.adapters.AlertListViewAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.fragment_alerts.view.*
import android.util.TypedValue
import android.view.MenuItem
import org.jetbrains.anko.textColor


/**
 * Created by anyexchange on 11/5/2017.
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

    private lateinit var alertList: ListView
    var alertAdapter: AlertListViewAdapter? = null

    var currency = Currency.BTC
    private var prefs: Prefs? = null
        get() {
            return if (field == null) {
                field
            } else if (context != null){
                field = Prefs(context!!)
                field
            }else {
                null
            }
        }

    companion object {
        lateinit var currency: Currency
        //var alerts: MutableSet<Alert> = mutableSetOf()

        fun newInstance(): AlertsFragment {
            return AlertsFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_alerts, container, false)

        this.inflater = inflater

        setupSwipeRefresh(rootView.swipe_refresh_layout)

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
        triggerLabelText.text = "New custom alert at: "

        setButton.setOnClickListener { setAlert() }
        setButton.text = "Set"


        alertAdapter = AlertListViewAdapter(inflater, sortedAlerts, { view, alert ->
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
        doneLoading()

        return rootView
    }

    private fun dp2px(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
                resources.displayMetrics).toInt()
    }
    private fun deleteAlert(alert: Alert) {
        prefs?.removeAlert(alert)
        alertAdapter?.alerts = sortedAlerts
        alertAdapter?.notifyDataSetChanged()
        alertList.adapter = alertAdapter
    }

    private fun setAlert() {
        val price = priceEditText.text.toString().toDoubleOrZero()
        if (price > 0) {
            val productPrice = Account.forCurrency(currency)?.product?.price ?: 0.0
            val triggerIfAbove = price > productPrice
            val alert = Alert(price, currency, triggerIfAbove)
            prefs?.addAlert(alert)
            alertAdapter?.alerts = sortedAlerts
            alertAdapter?.notifyDataSetChanged()
            priceEditText.setText("")
        }
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
        activity?.let { activity ->
            val tabAccentColor = currency.colorAccent(activity)
            currencyTabLayout.setSelectedTabIndicatorColor(tabAccentColor)

            val buttonColors = currency.colorStateList(activity)
            val buttonTextColor = currency.buttonTextColor(activity)
            setButton.backgroundTintList = buttonColors
            setButton.textColor = buttonTextColor
        }

    }

    private val sortedAlerts : List<Alert>
        get() {
            val prefs = Prefs(context!!)
            val alerts = prefs.alerts
            return alerts.sortedWith(compareBy({ it.price }))
        }

    override fun refresh(onComplete: (Boolean) -> Unit) {
        (activity as com.anyexchange.anyx.activities.MainActivity).updatePrices({ onComplete(false) }, {
            (activity as com.anyexchange.anyx.activities.MainActivity).loopThroughAlerts()
            alertAdapter?.alerts = sortedAlerts
            alertAdapter?.notifyDataSetChanged()
            onComplete(true)
        })
    }
}
