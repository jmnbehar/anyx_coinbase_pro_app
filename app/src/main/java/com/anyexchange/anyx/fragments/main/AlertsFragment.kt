package com.anyexchange.anyx.fragments.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.widget.*
import com.anyexchange.anyx.adapters.AlertListViewAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.fragment_alerts.view.*
import android.util.TypedValue
import android.view.*
import com.anyexchange.anyx.activities.MainActivity
import org.jetbrains.anko.support.v4.toast
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

    private var priceMovementCheckBox: CheckBox? = null

    private lateinit var setButton: Button

    private lateinit var alertList: ListView
    var alertAdapter: AlertListViewAdapter? = null

    var currency = Currency.BTC

    companion object {
        lateinit var currency: Currency
        //var alerts: MutableSet<Alert> = mutableSetOf()

        fun newInstance(): AlertsFragment {
            return AlertsFragment()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
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

        priceMovementCheckBox = rootView.cb_alert_price_movement
        setButton = rootView.btn_alert_set

        alertList = rootView.list_alerts

        titleText.text = resources.getString(R.string.alerts_title)

        switchCurrency(this.currency)
        currencyTabLayout.setupCryptoTabs { switchCurrency(it) }

        priceUnitText.text = ""
        triggerLabelText.text = resources.getString(R.string.alerts_new_alert_label)

        setButton.setOnClickListener { setAlert() }
        setButton.text = resources.getString(R.string.alerts_new_alert_button)

        priceMovementCheckBox?.setOnCheckedChangeListener {  _, isChecked ->
            context?.let {
                Prefs(it).setMovementAlert(currency, isChecked)
            }
        }

        context?.let {
            alertAdapter = AlertListViewAdapter(it, inflater, sortedAlerts) { view, alert ->
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
            }
            alertList.adapter = alertAdapter
        } ?: run {
            alertList.visibility = View.GONE
        }

        //alertList.setHeightBasedOnChildren()
        dismissProgressSpinner()

        return rootView
    }

    private fun dp2px(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
                resources.displayMetrics).toInt()
    }

    private fun deleteAlert(alert: Alert) {
        context?.let {
            Prefs(it).removeAlert(alert)
            alertAdapter?.alerts = sortedAlerts
            alertAdapter?.notifyDataSetChanged()
            alertList.adapter = alertAdapter
        } ?: run {
            toast(R.string.error_message)
        }
    }

    private fun setAlert() {
        val price = priceEditText.text.toString().toDoubleOrZero()
        context?.let { context ->
            if (price > 0) {
                val productPrice = Account.forCurrency(currency)?.product?.defaultPrice ?: 0.0
                val triggerIfAbove = price > productPrice
                val alert = Alert(price, currency, triggerIfAbove)
                Prefs(context).addAlert(alert)
                alertAdapter?.alerts = sortedAlerts
                alertAdapter?.notifyDataSetChanged()
                priceEditText.setText("")
            }
        }
    }

    private fun switchCurrency(currency: Currency) {
        this.currency = currency
        val price = Account.forCurrency(currency)?.product?.priceForQuoteCurrency(Account.defaultFiatCurrency)
        if (price == null) {
            priceLabelText.text = ""
            currentPriceText.text = ""
        } else {
            priceLabelText.text = resources.getString(R.string.alerts_current_price_label, currency.fullName)
            currentPriceText.text = price.fiatFormat(Account.defaultFiatCurrency)
        }
        context?.let { context ->
            val isMovementAlertActive = Prefs(context).isMovementAlertActive(currency)
            priceMovementCheckBox?.isChecked = isMovementAlertActive
            val priceMovementLabel = resources.getString(R.string.alert_rapid_movement, currency.toString())
            priceMovementCheckBox?.text = priceMovementLabel

            val tabAccentColor = currency.colorAccent(context)
            currencyTabLayout.setSelectedTabIndicatorColor(tabAccentColor)

            val buttonColors = currency.colorStateList(context)
            val buttonTextColor = currency.buttonTextColor(context)
            setButton.backgroundTintList = buttonColors
            setButton.textColor = buttonTextColor
        }
    }

    private val sortedAlerts : List<Alert>
        get() {
            context?.let { context ->
                val alerts = Prefs(context).alerts
                return alerts.sortedWith(compareBy { it.price })
            } ?: run {
                return listOf()
            }
        }

    override fun refresh(onComplete: (Boolean) -> Unit) {
        (activity as? MainActivity)?.let { mainActivity ->
            mainActivity.updatePrices({ onComplete(false) }, {
                mainActivity.loopThroughAlerts()
                alertAdapter?.alerts = sortedAlerts
                alertAdapter?.notifyDataSetChanged()
                onComplete(true)
            })
        }
    }
}
