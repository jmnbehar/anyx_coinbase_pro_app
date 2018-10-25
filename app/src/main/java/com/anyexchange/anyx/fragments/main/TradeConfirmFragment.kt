package com.anyexchange.anyx.fragments.main

import android.support.v4.app.DialogFragment
import android.os.Bundle
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.anyexchange.anyx.R
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.classes.api.CBProApi
import kotlinx.android.synthetic.main.dialog_trade_confirm.view.*


class TradeConfirmFragment: DialogFragment() {

    private var orderSummaryText: TextView? = null

    private var totalLabelText: TextView? = null
    private var totalText: TextView? = null

    private var feesLabelText: TextView? = null
    private var feesText: TextView? = null

    private var confirmButton: Button? = null

    var updatedTicker: Double? = null
    var amount: Double? = null
    var limit: Double? = null
    var devFee: Double? = null
    var timeInForce: TimeInForce? = null
    var cancelAfter: String? = null
    var cryptoTotal: Double? = null
    var dollarTotal: Double? = null
    var feeEstimate: Double? = null

    var tradingPair: TradingPair? = null
    var tradeType: TradeType? = null
    var tradeSide: TradeSide? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.dialog_trade_confirm, container, false)

        orderSummaryText = rootView.txt_trade_confirm_summary

        totalLabelText = rootView.txt_trade_confirm_est_total_label
        totalText = rootView.txt_trade_confirm_est_total

        feesLabelText = rootView.txt_trade_confirm_est_fees_label
        feesText = rootView.txt_trade_confirm_est_fees

        confirmButton = rootView.btn_dialog_confirm

        return inflater.inflate(R.layout.dialog_trade_confirm, container, false)
    }

    fun setInfo(updatedTicker: Double, amount: Double, limit: Double, devFee: Double, timeInForce: TimeInForce?, cancelAfter: String?,
                cryptoTotal: Double, dollarTotal: Double, feeEstimate: Double) {
        this.updatedTicker = updatedTicker
        this.amount = amount
        this.limit = limit
        this.devFee = devFee
        this.timeInForce = timeInForce
        this.cancelAfter = cancelAfter
        this.cryptoTotal = cryptoTotal
        this.dollarTotal = dollarTotal
        this.feeEstimate = feeEstimate
    }

    fun setText() {
        this.updatedTicker = updatedTicker
        this.amount = amount
        this.limit = limit
        this.devFee = devFee
        this.timeInForce = timeInForce
        this.cancelAfter = cancelAfter
        this.cryptoTotal = cryptoTotal
        this.dollarTotal = dollarTotal
        this.feeEstimate = feeEstimate



//        orderSummaryText?.text = when (tradeType) {
//            TradeType.MARKET -> {
//                when (amountUnitCurrency) {
//                    tradingPair.baseCurrency -> resources.getString(R.string.trade_summary_market_fixed_base,
//                            sideString, amount.format(tradingPair.baseCurrency))
//                    tradingPair.quoteCurrency -> resources.getString(R.string.trade_summary_market_fixed_quote,
//                            sideString, amount.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
//                    else -> ""
//                }
//            }
//            TradeType.LIMIT -> when (TradeFragment.tradeSide) {
//                TradeSide.BUY -> resources.getString(R.string.trade_summary_limit_buy,
//                        amount.format(tradingPair.baseCurrency), limitPrice.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
//                TradeSide.SELL -> resources.getString(R.string.trade_summary_limit_sell,
//                        amount.format(tradingPair.baseCurrency), limitPrice.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
//            }
//            TradeType.STOP -> when (TradeFragment.tradeSide) {
//                TradeSide.BUY -> resources.getString(R.string.trade_summary_stop_buy,
//                        amount.format(tradingPair.baseCurrency), limitPrice.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
//                TradeSide.SELL -> resources.getString(R.string.trade_summary_stop_sell,
//                        amount.format(tradingPair.baseCurrency), limitPrice.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
//            }
//        }
    }
}