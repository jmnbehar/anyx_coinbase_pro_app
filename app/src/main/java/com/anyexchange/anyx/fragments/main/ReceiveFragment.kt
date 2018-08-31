package com.anyexchange.anyx.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.fragment_receive.view.*
import net.glxn.qrgen.android.QRCode
import org.jetbrains.anko.support.v4.toast

/**
 * Created by anyexchange on 11/5/2017.
 */
class ReceiveFragment : RefreshFragment() {

    private lateinit var inflater: LayoutInflater
    private lateinit var titleText: TextView

    private lateinit var qrCodeImageView: ImageView
    private lateinit var addressTextView: TextView

    private lateinit var warningIconImageView: ImageView
    private lateinit var warning1TextView: TextView
    private lateinit var warning2TextView: TextView

    var currency: Currency
        get() = ChartFragment.currency
        set(value) { ChartFragment.currency = value }

    companion object {
        fun newInstance(): ReceiveFragment {
            return ReceiveFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_receive, container, false)

        this.inflater = inflater

        titleText = rootView.txt_receive_name

        qrCodeImageView = rootView.img_receive_qr_code
        addressTextView = rootView.txt_receive_address

        warning1TextView = rootView.txt_receive_warning
        warning2TextView = rootView.txt_receive_warning_2

        warningIconImageView = rootView.img_receive_warning

        switchCurrency()

        dismissProgressSpinner()

        return rootView
    }

    override fun onResume() {
        shouldHideSpinner = false
        super.onResume()
        titleText.setText(R.string.receive_title)
    }

    fun showAddressInfo(address: String?) {
        if (address != null) {
            val bitmap = QRCode.from(address).withSize(1000, 1000).bitmap()
            qrCodeImageView.setImageBitmap(bitmap)
            qrCodeImageView.visibility = View.VISIBLE
            addressTextView.text = address
        } else {
            qrCodeImageView.visibility = View.GONE
            addressTextView.text = "Add a refresh button"
        }
    }
    private fun getDepositAddress() {
        val relevantAccount = Account.forCurrency(currency)
        val coinbaseAccountId = relevantAccount?.coinbaseAccount?.id
        if (coinbaseAccountId != null) {
            CBProApi.depositAddress(apiInitData, coinbaseAccountId).get({ _ ->
                dismissProgressSpinner()
                showAddressInfo(null)
            }) { depositAddress ->
                dismissProgressSpinner()
                Account.forCurrency(currency)?.depositAddress = depositAddress
                showAddressInfo(depositAddress)
            }
        }
    }

    fun switchCurrency() {
        val relevantAccount = Account.forCurrency(currency)
        if (relevantAccount != null && relevantAccount.depositAddress == null) {
            showProgressSpinner()
            if (relevantAccount.coinbaseAccount == null) {
                CBProApi.coinbaseAccounts(apiInitData).linkToAccounts({
                    dismissProgressSpinner()
                    showAddressInfo(null)
                }, {
                    getDepositAddress()
                })
            } else {
                getDepositAddress()
            }
        } else {
            showAddressInfo(relevantAccount?.depositAddress)
        }
        when (currency) {
            //TODO: make this smarter:
            Currency.BTC -> {
                warning1TextView.setText(R.string.receive_warning_1_btc)
                warning2TextView.setText(R.string.receive_warning_2_btc)
            }
            Currency.ETH -> {
                warning1TextView.setText(R.string.receive_warning_1_eth)
                warning2TextView.setText(R.string.receive_warning_2_eth)
            }
            Currency.ETC -> {
                warning1TextView.setText(R.string.receive_warning_1_etc)
                warning2TextView.setText(R.string.receive_warning_2_etc)
            }
            Currency.BCH -> {
                warning1TextView.setText(R.string.receive_warning_1_bch)
                warning2TextView.setText(R.string.receive_warning_2_bch)
            }
            Currency.LTC -> {
                warning1TextView.setText(R.string.receive_warning_1_ltc)
                warning2TextView.setText(R.string.receive_warning_2_ltc)
            }
            Currency.USD, Currency.EUR, Currency.GBP -> { /* how tho */ }
            Currency.OTHER -> { /* how tho */ }
        }
    }
}
