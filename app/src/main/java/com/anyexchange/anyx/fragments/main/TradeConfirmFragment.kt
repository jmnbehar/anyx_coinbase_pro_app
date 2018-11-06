package com.anyexchange.anyx.fragments.main

import android.graphics.Color
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

    private var submitOrder: (NewOrder?) -> Unit = { }

    var currentPrice: Double? = null
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

        currentPrice?.let { currentPrice ->
            newOrder?.let { newOrder ->
                setText(currentPrice, newOrder)
            }
        }

        confirmButton?.setOnClickListener {_ ->
            submitOrder(newOrder)
        }

        return rootView
    }

    fun setInfo(currentPrice: Double, newOrder: NewOrder, submitOrder: (NewOrder?) -> Unit) {
        this.currentPrice = currentPrice
        this.newOrder = newOrder
        this.submitOrder = submitOrder
    }

    private fun setText(currentPrice: Double, newOrder: NewOrder) {
        val tradingPair = newOrder.tradingPair

        context?.let {
            orderSummaryText?.text = newOrder.summaryText(it)
            orderSummaryText?.visibility = View.VISIBLE
        } ?: run {
            orderSummaryText?.visibility = View.GONE
        }

        val exchangeFee = newOrder.exchangeFee(currentPrice)

        row1Label?.text = getString(R.string.trade_confirm_exchange_fee_label, newOrder.tradingPair.exchange)
        if (exchangeFee.first > 0.0 && exchangeFee.first < 0.01 && exchangeFee.second.isFiat) {
            val oneCent = 0.01.format(exchangeFee.second)
            row1Text?.text = getString(R.string.trade_confirm_very_small_fee, oneCent)
        } else {
            row1Text?.text = exchangeFee.first.format(exchangeFee.second)
        }

        row2Label?.text = getString(R.string.trade_confirm_anyx_fee_label)
        row2Text?.text = newOrder.devFee(currentPrice).format(tradingPair.baseCurrency)

        row3Label?.text = getString(R.string.trade_confirm_total_label)
        row3Text?.text = when (newOrder.side) {
            TradeSide.BUY -> when (newOrder.type) {
                TradeType.MARKET -> if (newOrder.amount != null) {
                    newOrder.totalQuote(currentPrice).format(tradingPair.quoteCurrency)
                } else {
                    newOrder.totalBase(currentPrice).format(tradingPair.baseCurrency)
                }
                TradeType.LIMIT -> newOrder.totalQuote(currentPrice).format(tradingPair.quoteCurrency)
                TradeType.STOP -> newOrder.totalBase(currentPrice).format(tradingPair.baseCurrency)
            }
            TradeSide.SELL -> newOrder.totalQuote(currentPrice).format(tradingPair.quoteCurrency)
        }

        confirmButton?.text = getString(R.string.trade_confirm_button_text, newOrder.side.toString())
    }

}