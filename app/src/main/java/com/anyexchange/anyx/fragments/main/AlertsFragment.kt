package com.anyexchange.anyx.fragments.main

import android.annotation.SuppressLint
import android.arch.lifecycle.Lifecycle
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
import com.anyexchange.anyx.adapters.AlertPagerAdapter
import com.anyexchange.anyx.adapters.HistoryPagerAdapter
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

    private lateinit var alertPager: LockableViewPager


    var currency: Currency
        get() = ChartFragment.currency
        set(value) { ChartFragment.currency = value }


    companion object {
        fun newInstance(): AlertsFragment {
            return AlertsFragment()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_alerts, container, false)

        this.inflater = inflater

        titleText = rootView.txt_alert_name

        currencyTabLayout = rootView.tabl_alerts_currency

        priceLabelText = rootView.txt_alert_price_label
        currentPriceText = rootView.txt_alert_current_price

        triggerLabelText = rootView.txt_alert_trigger_label
        priceUnitText = rootView.txt_alert_price_unit
        priceEditText = rootView.etxt_alert_price

        priceMovementCheckBox = rootView.cb_alert_price_movement
        setButton = rootView.btn_alert_set

        alertPager = rootView.alerts_view_pager

        titleText.text = resources.getString(R.string.alerts_title)

        switchCurrency(currency)
        currencyTabLayout.setupCryptoTabs {
//            if (lifecycle.currentState == Lifecycle.State.RESUMED) {
//                switchCurrency(it)
//            }
            switchCurrency(it)
        }

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
            alertPager.adapter = AlertPagerAdapter(it, childFragmentManager)
            alertPager.visibility = View.VISIBLE
        } ?: run {
            alertPager.visibility = View.GONE
        }

        dismissProgressSpinner()

        return rootView
    }

//    override fun onResume() {
//        super.onResume()
//        val index = Currency.cryptoList.indexOf(currency)
//        alertPager.setCurrentItem(index, false)
//    }

    private fun dp2px(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
                resources.displayMetrics).toInt()
    }

    private fun setAlert() {
        val price = priceEditText.text.toString().toDoubleOrZero()
        context?.let { context ->
            if (price > 0) {
                val productPrice = Account.forCurrency(currency)?.product?.defaultPrice ?: 0.0
                val triggerIfAbove = price > productPrice
                Prefs(context).addAlert(Alert(price, currency, triggerIfAbove))
                updatePagerAdapter()
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


    fun updatePagerAdapter() {
        (alertPager.adapter as? AlertPagerAdapter)?.notifyDataSetChanged()
    }
}
