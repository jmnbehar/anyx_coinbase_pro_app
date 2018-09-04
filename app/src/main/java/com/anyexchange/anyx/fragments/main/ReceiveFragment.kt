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
    }

    private fun showAddressInfo(addressInfo: ApiDepositAddress?) {
        if (addressInfo != null) {
            val bitmap = QRCode.from(addressInfo.address).withSize(1000, 1000).bitmap()
            qrCodeImageView.setImageBitmap(bitmap)
            qrCodeImageView.visibility = View.VISIBLE
            addressTextView.text = addressInfo.address

            warning1TextView.visibility = View.VISIBLE
            warning2TextView.visibility = View.VISIBLE
            if (addressInfo.warning_title != null) {
                warning1TextView.text = addressInfo.warning_title
            } else {
                warning1TextView.text = getString(R.string.receive_warning_1, currency.fullName, currency.toString())
            }
            if (addressInfo.warning_details != null) {
                warning2TextView.text = addressInfo.warning_details
            } else {
                warning2TextView.text = getString(R.string.receive_warning_2)
            }
            warningIconImageView.setImageResource(currency.iconId)
        } else {
            qrCodeImageView.visibility = View.GONE
            addressTextView.text = "Add a refresh button"
            warning1TextView.visibility = View.GONE
            warning2TextView.visibility = View.GONE
        }
    }

    fun switchCurrency() {
        val relevantAccount = Account.forCurrency(currency)
        if (relevantAccount != null && relevantAccount.depositInfo == null) {
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
            showAddressInfo(relevantAccount?.depositInfo)
        }
    }

    private fun getDepositAddress() {
        val relevantAccount = Account.forCurrency(currency)
        val coinbaseAccountId = relevantAccount?.coinbaseAccount?.id
        if (coinbaseAccountId != null) {
            CBProApi.depositAddress(apiInitData, coinbaseAccountId).get({ _ ->
                dismissProgressSpinner()
                showAddressInfo(null)
            }) { depositInfo ->
                dismissProgressSpinner()
                Account.forCurrency(currency)?.depositInfo = depositInfo
                showAddressInfo(depositInfo)
            }
            context?.let {
                Prefs(it).stashedCryptoAccountList = Account.cryptoAccounts
            }
        }
    }
}
