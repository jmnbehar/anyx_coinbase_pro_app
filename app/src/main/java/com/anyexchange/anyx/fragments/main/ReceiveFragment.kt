package com.anyexchange.anyx.fragments.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.fragment_receive.view.*
import net.glxn.qrgen.android.QRCode
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import com.anyexchange.anyx.api.CBProApi
import com.anyexchange.anyx.api.CBProDepositAddress


/**
 * Created by anyexchange on 11/5/2017.
 */
class ReceiveFragment : RefreshFragment() {

    private var inflater: LayoutInflater? = null

    private var qrCodeImageView: ImageView? = null
    private var addressLabelTextView: TextView? = null
    private var addressTextView: TextView? = null

    private var warningIconImageView: ImageView? = null
    private var warning1TextView: TextView? = null
    private var warning2TextView: TextView? = null

    var currency: Currency
        get() = ChartFragment.currency
        set(value) { ChartFragment.currency = value }

    //TODO: make this changable
    val exchange: Exchange = Exchange.CBPro

    companion object {
        fun newInstance(): ReceiveFragment {
            return ReceiveFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_receive, container, false)

        setupSwipeRefresh(rootView.swipe_refresh_layout as SwipeRefreshLayout)

        this.inflater = inflater

        qrCodeImageView = rootView.img_receive_qr_code
        addressTextView = rootView.txt_receive_address
        addressLabelTextView = rootView.txt_receive_address_label

        warning1TextView = rootView.txt_receive_warning
        warning2TextView = rootView.txt_receive_warning_2

        warningIconImageView = rootView.img_receive_warning

        switchCurrency(false)

        dismissProgressSpinner()

        return rootView
    }

    override fun onResume() {
        shouldHideSpinner = false
        super.onResume()
    }

    override fun refresh(onComplete: (Boolean) -> Unit) {
        super.refresh(onComplete)
        switchCurrency(true)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showAddressInfo(addressInfo: DepositAddressInfo?) {
        if (addressInfo != null) {
            val bitmap = QRCode.from(addressInfo.address).withSize(1000, 1000).bitmap()
            qrCodeImageView?.setImageBitmap(bitmap)
            qrCodeImageView?.visibility = View.VISIBLE

            addressLabelTextView?.text = resources.getString(R.string.receive_address_label, currency.toString())
            addressTextView?.text = addressInfo.address
            addressTextView?.setOnTouchListener { _, _ ->
                copyAddressToClipboard()
                true
            }

            warning1TextView?.visibility = View.VISIBLE
            warning2TextView?.visibility = View.VISIBLE
            if (addressInfo.warning_title != null) {
                warning1TextView?.text = addressInfo.warning_title
            } else {
                warning1TextView?.text = getString(R.string.receive_warning_1, currency.fullName, currency.toString())
            }
            if (addressInfo.warning_details != null) {
                warning2TextView?.text = addressInfo.warning_details
            } else {
                warning2TextView?.text = getString(R.string.receive_warning_2)
            }
            warningIconImageView?.setImageResource(currency.iconId ?: R.drawable.fail_icon)
        } else {
            qrCodeImageView?.visibility = View.GONE
            if (context != null){
                addressTextView?.visibility = View.VISIBLE
                addressTextView?.text = resources.getString(R.string.receive_refresh_label)
            } else {
                addressTextView?.visibility = View.GONE
            }
            warning1TextView?.visibility = View.GONE
            warning2TextView?.visibility = View.GONE
        }
    }

    fun switchCurrency(forceRefresh: Boolean) {
        val relevantAccount = Account.forCurrency(currency, exchange)
        if (relevantAccount != null && (forceRefresh || relevantAccount.depositInfo == null)) {
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
        val relevantAccount = Account.forCurrency(currency, exchange)
        val coinbaseAccountId = relevantAccount?.coinbaseAccount?.id
        if (coinbaseAccountId != null) {
            Product.map[currency.id]?.accounts?.get(Exchange.CBPro)?.let {account ->
                account.getDepositAddress(apiInitData, {
                    dismissProgressSpinner()
                    showAddressInfo(null)
                }) { depositInfo ->
                    dismissProgressSpinner()
                    Account.forCurrency(currency, exchange)?.depositInfo = depositInfo
                    showAddressInfo(depositInfo)
                }

            } ?: run {
                showAddressInfo(null)
            }
        }
    }

    private fun copyAddressToClipboard() {
        context?.let { context ->
            Account.forCurrency(currency, exchange)?.depositInfo?.let { depositInfo ->
                val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Copied Address", depositInfo.address)
                clipboard.primaryClip = clip
                toast("Copied Address to Clipboard")
            }
        }
    }
}
