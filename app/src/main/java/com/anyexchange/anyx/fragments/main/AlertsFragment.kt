package com.anyexchange.anyx.fragments.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.*
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.fragment_alerts.view.*
import android.view.*
import com.anyexchange.anyx.activities.MainActivity
import com.anyexchange.anyx.adapters.AlertListViewAdapter
import org.jetbrains.anko.textColor


/**
 * Created by anyexchange on 11/5/2017.
 */
class AlertsFragment : RefreshFragment() {

    private lateinit var inflater: LayoutInflater
    private lateinit var titleText: TextView

    private lateinit var priceLabelText: TextView
    private lateinit var currentPriceText: TextView

    private lateinit var triggerLabelText: TextView
    private lateinit var priceEditText: EditText
    private lateinit var priceUnitText: TextView

    private var alertList: ListView? = null
    private var alertAdapter: AlertListViewAdapter? = null
    private lateinit var viewManager: RecyclerView.LayoutManager

    private var quickChangeAlertCheckBox: CheckBox? = null

    private lateinit var setButton: Button

    var currency: Currency
        get() = ChartFragment.currency
        set(value) { ChartFragment.currency = value }

    private val sortedAlerts : List<PriceAlert>
        get() {
            context?.let { context ->
                val alerts = Prefs(context).alerts.filter { it.currency == currency }
                return alerts.sortedWith(compareBy { it.price })
            } ?: run {
                return listOf()
            }
        }

    companion object {
        fun newInstance(): AlertsFragment {
            return AlertsFragment()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_alerts, container, false)
        setupSwipeRefresh(rootView.swipe_refresh_layout)

        this.inflater = inflater

        titleText = rootView.txt_alert_name


        priceLabelText = rootView.txt_alert_price_label
        currentPriceText = rootView.txt_alert_current_price

        triggerLabelText = rootView.txt_alert_trigger_label
        priceUnitText = rootView.txt_alert_price_unit
        priceEditText = rootView.etxt_alert_price

        quickChangeAlertCheckBox = rootView.cb_alert_price_movement
        setButton = rootView.btn_alert_set

        titleText.text = resources.getString(R.string.alerts_title)

        switchCurrency(currency)

        priceUnitText.text = ""
        triggerLabelText.text = resources.getString(R.string.alerts_new_alert_label)

        setButton.setOnClickListener { setAlert() }
        setButton.text = resources.getString(R.string.alerts_new_alert_button)

        quickChangeAlertCheckBox?.setOnCheckedChangeListener { _, isChecked ->
            context?.let {
                Prefs(it).setQuickChangeAlertActive(currency, isChecked)
            }
        }

        viewManager = LinearLayoutManager(context)
        alertList = rootView.list_view_alerts

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
            alertList?.adapter = alertAdapter
        }

        dismissProgressSpinner()

        return rootView
    }

    override fun onResume() {
        super.onResume()
        val currencyList = Product.map.keys.map { Currency(it) }
        showNavSpinner(ChartFragment.currency, currencyList) { selectedCurrency ->
            switchCurrency(selectedCurrency)
        }

        if ((activity as MainActivity).navSpinner.selectedItem == null) {
            (activity as MainActivity).navSpinner.setSelectedItem(0)
        }


    }

    private fun setAlert() {
        val price = priceEditText.text.toString().toDoubleOrZero()
        context?.let { context ->
            if (price > 0) {
                val productPrice = Product.forCurrency(currency)?.defaultPrice ?: 0.0
                val triggerIfAbove = price > productPrice
                Prefs(context).addAlert(PriceAlert(price, currency, triggerIfAbove))
                updatePagerAdapter()
                priceEditText.setText("")
            }
        }
    }

    private fun switchCurrency(currency: Currency) {
        this.currency = currency
        val product = Product.forCurrency(currency)
        val price = product?.priceForQuoteCurrency(Account.defaultFiatCurrency)
        if (price == null) {
            priceLabelText.text = ""
            currentPriceText.text = ""
        } else {
            priceLabelText.text = resources.getString(R.string.alerts_current_price_label, currency.fullName)
            currentPriceText.text = price.format(Account.defaultFiatCurrency)
        }

        context?.let { context ->
            alertAdapter = AlertListViewAdapter(context, inflater, sortedAlerts) { view, alert ->
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
            alertList?.adapter = alertAdapter

            quickChangeAlertCheckBox?.isChecked = Prefs(context).isQuickChangeAlertActive(currency)
            quickChangeAlertCheckBox?.text = resources.getString(R.string.alert_rapid_movement, currency.toString())

            val buttonColors = currency.colorStateList(context)
            val buttonTextColor = currency.buttonTextColor(context)
            setButton.backgroundTintList = buttonColors
            setButton.textColor = buttonTextColor
        }
    }

    override fun refresh(onComplete: (Boolean) -> Unit) {
        super.refresh(onComplete)
        alertAdapter?.alerts = sortedAlerts
        alertAdapter?.notifyDataSetChanged()

        if (currency == ChartFragment.currency) {
            (activity as? MainActivity)?.let { mainActivity ->

                mainActivity.updatePrices({ onComplete(false) }, {
                    mainActivity.loopThroughAlerts()
                })
                mainActivity.loopThroughAlerts()

                onComplete(true)

            } ?: run {
                onComplete(true)
            }
        } else {
            onComplete(true)
        }
    }

    fun updatePagerAdapter() {
        (alertList?.adapter as? AlertListViewAdapter)?.alerts = sortedAlerts
        (alertList?.adapter as? AlertListViewAdapter)?.notifyDataSetChanged()
    }


    private fun deleteAlert(alert: PriceAlert) {
        context?.let {
            Prefs(it).removeAlert(alert)
            alertAdapter?.alerts = sortedAlerts
            alertAdapter?.notifyDataSetChanged()
            alertList?.adapter = alertAdapter
        }
    }
}
