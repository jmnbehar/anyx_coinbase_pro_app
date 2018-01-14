package com.jmnbehar.gdax.Fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_alerts.view.*

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

        setButton.setOnClickListener { setAlert() }

        return rootView
    }


    private fun setAlert() {
        val price = priceEditText.text.toString().toDoubleOrZero()
        val productPrice = Account.forCurrency(currency)?.product?.price ?: 0.0
        val triggerIfAbove = price < productPrice
        val alert = Alert(price, currency, triggerIfAbove)
        alerts.add(alert)
        prefs.addAlert(alert)
    }

    private fun switchCurrency(currency: Currency = this.currency) {
        this.currency = currency

        priceUnitText.text = currency.toString()
        when (currency) {
            Currency.BTC -> radioButtonBtc.isChecked = true
            Currency.ETH -> radioButtonEth.isChecked = true
            Currency.LTC -> radioButtonLtc.isChecked = true
        }

    }

}
