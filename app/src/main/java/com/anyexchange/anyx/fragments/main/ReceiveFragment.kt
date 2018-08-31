package com.anyexchange.anyx.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.fragment_receive.view.*

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
        super.onResume()
        titleText.setText(R.string.receive_title)
        val address = Account.forCurrency(currency)?.depositAddress ?: ""
        addressTextView.text = address
    }

    fun switchCurrency() {
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
