package com.anyexchange.anyx.fragments.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.activities.ScanActivity
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.classes.api.CBProApi
import kotlinx.android.synthetic.main.fragment_send.view.*
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.textColor

/**
 * Created by anyexchange on 11/5/2017.
 */
class SendFragment : RefreshFragment() {

    private lateinit var inflater: LayoutInflater

    private var amountEditText: EditText? = null
    private var amountUnitText: TextView? = null
    private var amountLabelText: TextView? = null

    private var destinationEditText: EditText? = null
    private var destinationLabelText: TextView? = null

    private var scanButton: ImageButton? = null

    private var warning1TextView: TextView? = null
    private var warning2TextView: TextView? = null

    private var iconImageView: ImageView? = null
    private var currencyTickerTextView: TextView? = null
    private var accountBalanceTextView: TextView? = null
    private var accountValueTextView: TextView? = null

    private lateinit var sendButton: Button

    var currency: Currency
        get() = ChartFragment.currency
        set(value) { ChartFragment.currency = value }

    //TODO: make this changeable:
    val exchange: Exchange = Exchange.CBPro

    companion object {
        fun newInstance(): SendFragment {
            return SendFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_send, container, false)

        this.inflater = inflater

        amountLabelText = rootView.txt_send_amount_label
        amountEditText = rootView.etxt_send_amount
        amountUnitText = rootView.txt_send_amount_unit

        destinationLabelText = rootView.txt_send_destination_label
        destinationEditText = rootView.etxt_send_destination

        scanButton = rootView.btn_send_destination_camera

        warning1TextView = rootView.txt_send_warning
        warning2TextView = rootView.txt_send_warning_2

        iconImageView = rootView.img_send_account_icon
        currencyTickerTextView = rootView.txt_send_ticker
        accountBalanceTextView = rootView.txt_send_account_balance
        accountValueTextView = rootView.txt_send_account_value

        sendButton = rootView.btn_send

        setupSwipeRefresh(rootView.swipe_refresh_layout as SwipeRefreshLayout)

        switchCurrency()

        scanButton?.setOnClickListener { getAddressFromCamera() }


        sendButton.setOnClickListener { _ ->
            val amount = amountEditText?.text.toString()
            val destination = destinationEditText?.text.toString()
            val context = context
            if (amount.isBlank()) {
                toast(R.string.send_enter_amount_warning)
            } else if (destination.isBlank()) {
                toast(R.string.send_enter_destination_warning)
            } else if (context != null && Prefs(context).shouldShowSendConfirmModal) {
                alert {
                    title = resources.getString(R.string.send_confirm_message, amount, currency, destination)
                    positiveButton(R.string.send_confirm_button) { submitSend() }
                    negativeButton(R.string.send_cancel_button)  { }
                }.show()
            } else {
                submitSend()
            }
        }

        dismissProgressSpinner()

        return rootView
    }

    override fun onResume() {
        shouldHideSpinner = false
        super.onResume()
    }

    override fun refresh(onComplete: (Boolean) -> Unit) {
        super.refresh(onComplete)

        Account.forCurrency(currency, exchange)?.let { account ->
            account.update(apiInitData, {//onFailure
                onComplete(false)
            }) { //onSuccess
                setAccountBalanceText()
                onComplete(true)
            }
        } ?: run {
            onComplete(false)
        }
    }

    private fun setAccountBalanceText() {
        currencyTickerTextView?.text = currency.toString()
        iconImageView?.setImageResource(currency.iconId)
        Account.forCurrency(currency, exchange)?.let {
            accountBalanceTextView?.visibility = View.VISIBLE
            accountBalanceTextView?.text = resources.getString(R.string.send_balance_text, it.availableBalance.btcFormatShortened())
        } ?: run {
            accountBalanceTextView?.visibility = View.GONE
        }
        accountValueTextView?.visibility = View.GONE
    }

    private fun submitSend() {
        val amount = amountEditText?.text.toString().toDoubleOrZero()
        val destination = destinationEditText?.text.toString()

        val min = currency.minSendAmount

        if (amount >= min) {
            CBProApi.sendCrypto(apiInitData, amount, currency, destination).executePost(
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
                            "invalid crypto_address" -> resources.getString(R.string.send_invalid_destination_error,
                                    currency.toString())
                            else -> CBProApi.defaultPostFailure(context, result)
                        }
                        toast(errorString)
                    },
                    { _ -> //success
                        toast(R.string.toast_success)
                    })
        } else {
            toast(resources.getString(R.string.send_amount_error, min.toString()))
        }
    }

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
        val extras = data.extras
        if (extras != null) {
            val barcode = extras.getString("BarCode")
            if (barcode == "") {
                toast(R.string.send_address_not_found_warning)
            } else {
                //TODO: parse more advanced qr codes
                destinationEditText?.setText(barcode)
            }
        }
    }

    fun switchCurrency() {

        setAccountBalanceText()

        amountUnitText?.text = currency.toString()
        destinationLabelText?.text = resources.getString(R.string.send_destination_label, currency)

        context?.let { context ->
            val buttonColors = currency.colorStateList(context)
            val buttonTextColor = currency.buttonTextColor(context)
            sendButton.backgroundTintList = buttonColors
            sendButton.textColor = buttonTextColor


            warning1TextView?.visibility = View.VISIBLE
            warning2TextView?.visibility = View.VISIBLE
            when {
                currency == Currency.ETH -> {
                    warning1TextView?.setText(R.string.send_warning_1_eth)
                    warning2TextView?.setText(R.string.send_warning_2_eth)
                }
                currency.isFiat -> {
                    warning1TextView?.visibility = View.GONE
                    warning2TextView?.visibility = View.GONE
                }
                else -> {
                    warning1TextView?.text = resources.getString(R.string.send_warning_1, currency.toString(), currency.toString())
                    warning2TextView?.text = resources.getString(R.string.send_warning_2, currency.fullName, currency.toString())
                }
            }

            sendButton.backgroundTintList = buttonColors
            sendButton.textColor = buttonTextColor
        }
    }
}
