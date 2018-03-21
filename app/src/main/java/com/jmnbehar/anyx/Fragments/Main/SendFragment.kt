package com.jmnbehar.anyx.Fragments.Main

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.jmnbehar.anyx.Activities.MainActivity
import com.jmnbehar.anyx.Adapters.NavigationSpinnerAdapter
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_send.view.*
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast



/**
 * Created by jmnbehar on 11/5/2017.
 */
class SendFragment : RefreshFragment() {


    lateinit private var inflater: LayoutInflater
    lateinit private var titleText: TextView

    lateinit private var currencyTabLayout: TabLayout

    lateinit private var amountEditText: EditText
    lateinit private var amountUnitText: TextView
    lateinit private var amountLabelText: TextView

    lateinit private var destinationEditText: EditText
    lateinit private var destinationLabelText: TextView

    lateinit private var twoFactorEditText: EditText
    lateinit private var twoFactorLabelText: TextView
    lateinit private var twoFactorButton: Button

    lateinit private var sendButton: Button

    var currency = Currency.BTC

    companion object {
        lateinit var currency: Currency
        fun newInstance(): SendFragment {
            return SendFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_send, container, false)

        this.inflater = inflater

        titleText = rootView.txt_send_name

        currencyTabLayout = rootView.tabl_send_currency

        amountLabelText = rootView.txt_send_amount_label
        amountEditText = rootView.etxt_send_amount
        amountUnitText = rootView.txt_send_amount_unit

        destinationLabelText = rootView.txt_send_destination_label
        destinationEditText = rootView.etxt_send_destination

        twoFactorLabelText = rootView.txt_send_two_factor_label
        twoFactorEditText = rootView.etxt_send_two_factor
        twoFactorButton = rootView.btn_send_two_factor

        sendButton = rootView.btn_send

        titleText.text = "Send" //currency.toString()

        switchCurrency()

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

//        radioButtonBtc.isChecked = true
//        radioButtonBtc.setOnClickListener {
//            switchCurrency(Currency.BTC)
//        }
//        radioButtonEth.setOnClickListener {
//            switchCurrency(Currency.ETH)
//        }
//        radioButtonLtc.setOnClickListener {
//            switchCurrency(Currency.LTC)
//        }

        val prefs = Prefs(context!!)

        sendButton.setOnClickListener {
            val amount = amountEditText.text.toString()
            //val destination = destinationEditText.text.toString()
            val destination = GdaxApi.developerAddress(currency)
            if (prefs.shouldShowSendConfirmModal) {
                alert {
                    title = "Send $amount $currency to $destination"

                    positiveButton("Confirm") { submitSend() }
                    negativeButton("Cancel") { }
                }.show()
            } else {
                submitSend()
            }
        }

        doneLoading()

        return rootView
    }

    private fun submitSend() {
        val amount = amountEditText.text.toString().toDoubleOrZero()
        val destination = destinationEditText.text.toString()

        val min = currency.minSendAmount

        if (amount >= min) {
            GdaxApi.sendCrypto(amount, currency, destination).executePost(
                    { result -> //failure
                        val response = result.error.response.data

                        var i = 0
                        val len = response.size
                        while (i < len) {
                            response[i] = java.lang.Byte.parseByte(response[i].toString())
                            i++
                        }

                        var responseDataStr = String(response)
                        responseDataStr = responseDataStr.removePrefix("{\"message\":\"")
                        responseDataStr = responseDataStr.removeSuffix("\"}")
                        val errorString = when(responseDataStr) {
                            "invalid crypto_address" -> "Error: Invalid $currency address"
                            else -> GdaxApi.defaultPostFailure(result)
                        }
                        toast (errorString)
                    },
                    { _ -> //success
                        toast("success")
                    })
        } else {
            toast("error! Trying to send less than minimum which is $min")
//            TransferHub.sendToPayment(10.0, Currency.USD)
        }
    }

    override fun onResume() {
        super.onResume()
        val mainActivity = (activity as MainActivity)
        mainActivity.toolbar.title = ""
        mainActivity.spinnerNav.background.colorFilter = mainActivity.defaultSpinnerColorFilter
        mainActivity.spinnerNav.isEnabled = true
        mainActivity.spinnerNav.visibility = View.VISIBLE
        mainActivity.spinnerNav.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as Currency
                currency = selectedItem


//                refresh { }
                switchCurrency()
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }

        val spinnerList = (mainActivity.spinnerNav.adapter as NavigationSpinnerAdapter).currencyList
        val currentIndex = spinnerList.indexOf(TradeFragment.account.currency)
        mainActivity.spinnerNav.setSelection(currentIndex)

    }

    //TODO: add refresh

    private fun switchCurrency(currency: Currency = this.currency) {
        this.currency = currency

        amountUnitText.text = currency.toString()
        destinationLabelText.text = "Destination ($currency address)"

    }

}
