package com.anyexchange.anyx.fragments.main

import android.graphics.Color
import android.support.v4.app.DialogFragment
import android.os.Bundle
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.anyexchange.anyx.R
import com.anyexchange.anyx.classes.*
import kotlinx.android.synthetic.main.dialog_stablecoin_conversion.view.*


class StablecoinConversionFragment: DialogFragment() {

    private var titleText: TextView? = null

    private var detailsText: TextView? = null

    private var amountEditText: EditText? = null

    private var confirmButton: Button? = null

    private var submitConversion: (TradingPair, Double) -> Unit = { _, _ -> }

    var tradingPair: TradingPair? = null

    companion object {
        private val usdUsdcCbPro =  TradingPair(Exchange.CBPro, Currency.USD, Currency.USDC)
        private val usdcUsdCbPro =  TradingPair(Exchange.CBPro, Currency.USDC, Currency.USD)

        val supportedTradingPairs = listOf(usdUsdcCbPro, usdcUsdCbPro)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.dialog_stablecoin_conversion, container, false)

        titleText = rootView.txt_stablecoin_conversion_title
        detailsText = rootView.txt_stablecoin_details
        confirmButton = rootView.btn_conversion_confirm
        amountEditText = rootView.etxt_conversion_amount

        tradingPair?.let { tradingPair ->
            setText(tradingPair)
        }

        confirmButton?.setOnClickListener { _ ->
            val amount = amountEditText?.text.toString().toDoubleOrZero()
            tradingPair?.let {
                submitConversion(it, amount)
            }
        }

        return rootView
    }

    fun setInfo(tradingPair: TradingPair, submitConversion: (TradingPair, Double) -> Unit) {
        this.tradingPair = tradingPair
        this.submitConversion = submitConversion
    }

    private fun setText(tradingPair: TradingPair) {
        context?.let {
            titleText?.text = getString(R.string.stable_coin_conversion_title, tradingPair.quoteCurrency, tradingPair.baseCurrency)
            titleText?.visibility = View.VISIBLE
            detailsText?.text = when (tradingPair) {
                usdUsdcCbPro -> getString(R.string.stable_coin_conversion_usd_usdc)
                usdcUsdCbPro -> getString(R.string.stable_coin_conversion_usdc_usd)
                else -> ""
            }
            detailsText?.visibility = View.VISIBLE
        } ?: run {
            titleText?.visibility = View.GONE
            detailsText?.visibility = View.GONE
        }

        confirmButton?.text = getString(R.string.trade_confirm_button_text)
    }

}