package com.jmnbehar.gdax.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_send.view.*
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast

/**
 * Created by jmnbehar on 11/5/2017.
 */
class SendFragment : RefreshFragment() {


    lateinit private var inflater: LayoutInflater
    lateinit private var titleText: TextView

    lateinit private var radioButtonBtc: RadioButton
    lateinit private var radioButtonEth: RadioButton
    lateinit private var radioButtonLtc: RadioButton

    lateinit private var amountEditText: EditText
    lateinit private var amountUnitText: TextView
    lateinit private var amountLabelText: TextView

    lateinit private var destinationEditText: EditText
    lateinit private var destinationLabelText: TextView

    lateinit private var twoFactorEditText: EditText
    lateinit private var twoFactorLabelText: TextView
    lateinit private var twoFactorButton: Button

    lateinit private var totalLabelText: TextView
    lateinit private var totalText: TextView

    lateinit private var sendButton: Button

    var currency = Currency.BTC

    companion object {
        lateinit var currency: Currency
        fun newInstance(): SendFragment {
            return SendFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_send, container, false)

        this.inflater = inflater

        titleText = rootView.txt_send_name

        radioButtonBtc = rootView.rbtn_trade_btc
        radioButtonEth = rootView.rbtn_trade_eth
        radioButtonLtc = rootView.rbtn_trade_ltc

        amountLabelText = rootView.txt_send_amount_label
        amountEditText = rootView.etxt_send_amount
        amountUnitText = rootView.txt_send_amount_unit

        destinationLabelText = rootView.txt_send_destination_label
        destinationEditText = rootView.etxt_send_destination

        twoFactorLabelText = rootView.txt_send_two_factor_label
        twoFactorEditText = rootView.etxt_send_two_factor
        twoFactorButton = rootView.btn_send_two_factor

        sendButton = rootView.btn_send

        titleText.text = currency.toString()

        switchCurrency()

        radioButtonBtc.isChecked = true

        radioButtonBtc.setOnClickListener {
            switchCurrency(Currency.BTC)
        }
        radioButtonEth.setOnClickListener {
            switchCurrency(Currency.ETH)
        }
        radioButtonLtc.setOnClickListener {
            switchCurrency(Currency.LTC)
        }

        sendButton.setOnClickListener {
            val amount = amountEditText.text.toString()
            val destination = destinationEditText.text.toString()
            alert {
                title = "Send $amount $currency to $destination"

                positiveButton("Confirm") { submitSend() }
                negativeButton("Cancel") { }
            }.show()
        }

        return rootView
    }

    private fun submitSend() {
        val amount = amountEditText.text.toString().toDoubleOrZero()
        var destination = destinationEditText.text.toString()
        destination = "18vYdgX81Zc2XoY66rbfWvMz4QSxdW9mio"  //TODO: remove this: sry aksai

        val min = when (currency) {
            Currency.BTC -> .0001
            Currency.ETH -> .001
            Currency.BCH -> .001
            Currency.LTC -> .1
            else -> 100.0
        }

        fun onComplete(result: Result<ByteArray, FuelError>) {
            when (result) {
                is Result.Failure -> {
                    //error
                    println("Error!: ${result.error}")
                    toast("Error!: ${result.error}")
                }
                is Result.Success -> {
                    toast("success")
                }
            }
        }

        if (amount > min) {
            GdaxApi.send(amount, currency, destination).executePost({ }) { result ->
                onComplete(result)
            }
        } else {
            toast("error! Trying to send less than minimum which is $min")
        }
    }

    private fun switchCurrency(currency: Currency = this.currency) {
        this.currency = currency

        amountUnitText.text = currency.toString()
        destinationLabelText.text = "Destination ($currency address)"

    }

}
