package com.anyexchange.anyx.fragments.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.activities.ScanActivity
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.fragment_send.view.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.textColor

/**
 * Created by anyexchange on 11/5/2017.
 */
class SendFragment : RefreshFragment() {


    private lateinit var inflater: LayoutInflater
    private lateinit var titleText: TextView

    private lateinit var currencyTabLayout: TabLayout

    private lateinit var amountEditText: EditText
    private lateinit var amountUnitText: TextView
    private lateinit var amountLabelText: TextView

    private lateinit var destinationEditText: EditText
    private lateinit var destinationLabelText: TextView

    private lateinit var scanButton: ImageButton

    private lateinit var warning1TextView: TextView
    private lateinit var warning2TextView: TextView

    private lateinit var sendButton: Button

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

        scanButton = rootView.btn_send_destination_camera

        warning1TextView = rootView.txt_send_warning
        warning2TextView = rootView.txt_send_warning_2

        sendButton = rootView.btn_send

        titleText.text = resources.getText(R.string.send_title)

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

        scanButton.setOnClickListener { getAddressFromCamera() }

        val prefs = Prefs(context!!)

        sendButton.setOnClickListener {
            val amount = amountEditText.text.toString()
            val destination = destinationEditText.text.toString()
            if (amount.isBlank()) {
                toast(R.string.send_enter_amount_warning)
            } else if (destination.isBlank()) {
                toast(R.string.send_enter_destination_warning)
            } else if (prefs.shouldShowSendConfirmModal) {
                alert {
                    title = resources.getString(R.string.send_confirm_message, amount, currency, destination)
                    positiveButton(R.string.send_confirm_button) { submitSend() }
                    negativeButton(R.string.send_cancel_button)  { }
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
            CBProApi.sendCrypto(amount, currency, destination).executePost(
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
                            "invalid crypto_address" -> resources.getText(R.string.send_invalid_destination_error,
                                    currency.toString())
                            else -> CBProApi.defaultPostFailure(context, result)
                        }
                        toast(errorString)
                    },
                    { _ -> //success
                        toast(R.string.toast_success)
                    })
        } else {
            toast(resources.getText(R.string.send_amount_error, min.toString()))
//            TransferHub.sendToPayment(10.0, Currency.USD)
        }
    }

//    override fun onResume() {
//        super.onResume()
////        showNavSpinner(ChartFragment.account?.currency) { selectedCurrency ->
////            currency = selectedCurrency
//////                refresh { }
////            switchCurrency()
////        }
//    }

    //TODO: add refresh

    private fun getAddressFromCamera() {
        activity?.let { activity ->
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                // Ask for camera permission
                ActivityCompat.requestPermissions(activity,
                        arrayOf(Manifest.permission.CAMERA),
                        666)
            } else {
                val intent = Intent(activity, ScanActivity::class.java)
                startActivityForResult(intent, 2)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        val barcode = data.extras.getString("BarCode")

        if (barcode == "") {
            toast(R.string.send_address_not_found_warning)
        } else {
            //TODO: parse more advanced qr codes
            destinationEditText.setText(barcode)
        }
    }

    private fun switchCurrency(currency: Currency = this.currency) {
        this.currency = currency

        amountUnitText.text = currency.toString()
        destinationLabelText.text = resources.getString(R.string.send_destination_label, currency)

        context?.let { context ->
            val buttonColors = currency.colorStateList(context)
            val buttonTextColor = currency.buttonTextColor(context)
            sendButton.backgroundTintList = buttonColors
            sendButton.textColor = buttonTextColor

            val tabAccentColor = currency.colorAccent(activity!!)
            currencyTabLayout.setSelectedTabIndicatorColor(tabAccentColor)

            when (currency) {
                Currency.BTC -> {
                    warning1TextView.setText(R.string.send_warning_1_btc)
                    warning2TextView.setText(R.string.send_warning_2_btc)
                }
                Currency.ETH -> {
                    warning1TextView.setText(R.string.send_warning_1_eth)
                    warning2TextView.setText(R.string.send_warning_2_eth)
                }
                Currency.BCH -> {
                    warning1TextView.setText(R.string.send_warning_1_bch)
                    warning2TextView.setText(R.string.send_warning_2_bch)
                }
                Currency.LTC -> {
                    warning1TextView.setText(R.string.send_warning_1_ltc)
                    warning2TextView.setText(R.string.send_warning_2_ltc)
                }
                Currency.USD, Currency.EUR -> { /* how tho */ }
            }
        }
    }

}
