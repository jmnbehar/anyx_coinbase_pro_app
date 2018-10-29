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

    private var row1Label: TextView? = null
    private var row1Text: TextView? = null

    private var row2Label: TextView? = null
    private var row2Text: TextView? = null

    private var row3Label: TextView? = null
    private var row3Text: TextView? = null

    private var confirmButton: Button? = null

    var updatedTicker: Double? = null
    var newOrder: NewOrder? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.dialog_trade_confirm, container, false)

        orderSummaryText = rootView.txt_trade_confirm_summary

        row1Label = rootView.txt_trade_confirm_row_1_label
        row1Text  = rootView.txt_trade_confirm_row_1_text

        row2Label = rootView.txt_trade_confirm_row_2_label
        row2Text  = rootView.txt_trade_confirm_row_2_text

        row3Label = rootView.txt_trade_confirm_row_3_label
        row3Text  = rootView.txt_trade_confirm_row_3_text

        confirmButton = rootView.btn_dialog_confirm

        return inflater.inflate(R.layout.dialog_trade_confirm, container, false)
    }

    fun setInfo(updatedTicker: Double, newOrder: NewOrder) {
        this.updatedTicker = updatedTicker
        this.newOrder = newOrder

        val tradingPair = newOrder.tradingPair

        orderSummaryText?.text = when (newOrder.type) {
            TradeType.MARKET -> if (newOrder.amount != null) {
                resources.getString(R.string.trade_summary_market_fixed_base, newOrder.side.name, newOrder.amount.format(tradingPair.baseCurrency))
            } else {
                resources.getString(R.string.trade_summary_market_fixed_quote, newOrder.side.name, newOrder.funds!!.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
            }
            TradeType.LIMIT -> when (newOrder.side) {
                TradeSide.BUY -> resources.getString(R.string.trade_summary_limit_buy,
                        newOrder.amount!!.format(tradingPair.baseCurrency), newOrder.priceLimit!!.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
                TradeSide.SELL -> resources.getString(R.string.trade_summary_limit_sell,
                        newOrder.amount!!.format(tradingPair.baseCurrency), newOrder.priceLimit!!.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
            }
            TradeType.STOP -> when (newOrder.side) {
                TradeSide.BUY -> resources.getString(R.string.trade_summary_stop_buy,
                        newOrder.amount!!.format(tradingPair.baseCurrency), newOrder.priceLimit!!.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
                TradeSide.SELL -> resources.getString(R.string.trade_summary_stop_sell,
                        newOrder.amount!!.format(tradingPair.baseCurrency), newOrder.priceLimit!!.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
            }
        }

        val exchangeFee = newOrder.exchangeFee(updatedTicker)
        val devFee = newOrder.devFee(updatedTicker)

        //TODO: approximate =
        row1Label?.text = "${tradingPair.exchange.name} fee: "
        row1Text?.text = exchangeFee.first.format(exchangeFee.second)

        row2Label?.text = "AnyX fee:"
        row2Text?.text = devFee.format(tradingPair.baseCurrency)

        row3Label?.text = "Total:"
        row3Text?.text = when (newOrder.side) {
            TradeSide.BUY -> when (newOrder.type) {
                TradeType.MARKET -> if (newOrder.amount == null) {
                    newOrder.totalQuote(updatedTicker).format(tradingPair.quoteCurrency)
                } else {
                    newOrder.totalBase(updatedTicker).format(tradingPair.baseCurrency)
                }
                TradeType.LIMIT -> newOrder.totalQuote(updatedTicker).format(tradingPair.quoteCurrency)
                TradeType.STOP -> newOrder.totalBase(updatedTicker).format(tradingPair.baseCurrency)
            }
            TradeSide.SELL -> newOrder.totalQuote(updatedTicker).format(tradingPair.quoteCurrency)
        }


    }

}