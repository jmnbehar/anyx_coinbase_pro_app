package com.jmnbehar.gdax.Fragments

import android.opengl.Visibility
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.jmnbehar.gdax.Classes.Account
import com.jmnbehar.gdax.Classes.TradeSubType
import com.jmnbehar.gdax.Classes.TradeType
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_trade.view.*

/**
 * Created by jmnbehar on 11/5/2017.
 */
class TradeFragment : Fragment() {


    private lateinit var inflater: LayoutInflater
    lateinit var titleText: TextView

    lateinit var radioButtonBuy: RadioButton
    lateinit var radioButtonSell: RadioButton

    lateinit var radioButtonMarket: RadioButton
    lateinit var radioButtonLimit: RadioButton
    lateinit var radioButtonStop: RadioButton

    lateinit var amountEditText: EditText
    lateinit var amountUnitText: TextView
    lateinit var amountLabelText: TextView

    lateinit var limitLayout: LinearLayout
    lateinit var limitEditText: EditText
    lateinit var limitUnitText: TextView
    lateinit var limitLabelText: TextView

    lateinit var totalLabelText: TextView
    lateinit var totalText: TextView

    lateinit var advancedOptionsCheckBox: CheckBox

    lateinit var placeOrderButton: Button

    var tradeSubType = TradeSubType.MARKET
        set(value) {
            switchTradeType(tradeType, value)
        }

    var tradeType = Companion.tradeType
        set(value) {
            switchTradeType(value, tradeSubType)
        }

    companion object {
        var tradeType = TradeType.BUY
        var localCurrency = "USD"
        lateinit var account: Account
        fun newInstance(accountIn: Account, tradeTypeIn: TradeType): TradeFragment {
            account = accountIn
            tradeType = tradeTypeIn
            return TradeFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_trade, container, false)

//        rootView.setBackgroundColor(Color.YELLOW)
        this.inflater = inflater

        titleText = rootView.txt_trade_name

        radioButtonBuy = rootView.rbtn_trade_buy
        radioButtonSell = rootView.rbtn_trade_sell

        radioButtonMarket = rootView.rbtn_trade_market
        radioButtonLimit = rootView.rbtn_trade_limit
        radioButtonStop = rootView.rbtn_trade_stop

        amountLabelText = rootView.txt_trade_amount_label
        amountEditText = rootView.etxt_trade_amount
        amountUnitText = rootView.txt_trade_amount_unit

        limitLayout = rootView.layout_trade_limit
        limitLabelText = rootView.txt_trade_limit_label
        limitEditText = rootView.etxt_trade_limit
        limitUnitText = rootView.txt_trade_limit_unit

        advancedOptionsCheckBox = rootView.cb_trade_advanced

        totalLabelText = rootView.txt_trade_total_label
        totalText = rootView.txt_trade_total

        placeOrderButton = rootView.btn_place_order

        titleText.text = account.currency

        switchTradeType(tradeType, tradeSubType)


        radioButtonBuy.setOnClickListener {
            tradeType = TradeType.BUY
        }
        radioButtonSell.setOnClickListener {
            tradeType = TradeType.SELL
        }

        radioButtonMarket.setOnClickListener {
            tradeSubType = TradeSubType.MARKET
        }
        radioButtonLimit.setOnClickListener {
            tradeSubType = TradeSubType.LIMIT
        }
        radioButtonStop.setOnClickListener {
            tradeSubType = TradeSubType.STOP
        }

        return rootView
    }

    private fun switchTradeType(tradeType: TradeType, tradeSubType: TradeSubType) {

        when (tradeType) {
            TradeType.BUY -> {
                radioButtonBuy.isChecked = true
                when (tradeSubType) {
                    TradeSubType.MARKET -> {
                        radioButtonMarket.isChecked = true
                        amountUnitText.text = localCurrency
                        limitLayout.visibility = View.GONE
                        totalLabelText.text = "Total (${account.currency}) ="
                    }
                    TradeSubType.LIMIT -> {
                        radioButtonLimit.isChecked = true
                        amountUnitText.text = account.currency
                        limitLayout.visibility = View.VISIBLE
                        limitUnitText.text = localCurrency
                        limitLabelText.text = "Limit Price"
                        totalLabelText.text = "Total (${localCurrency}) ="
                    }
                    TradeSubType.STOP -> {
                        radioButtonStop.isChecked = true
                        amountUnitText.text = localCurrency
                        limitLayout.visibility = View.VISIBLE
                        limitUnitText.text = localCurrency
                        limitLabelText.text = "Stop Price"
                        totalLabelText.text = "Total (${account.currency}) ="

                    }
                }
            }
            TradeType.SELL -> {
                radioButtonSell.isChecked = true
                when (tradeSubType) {
                    TradeSubType.MARKET -> {
                        radioButtonMarket.isChecked = true
                        amountUnitText.text = account.currency
                        limitLayout.visibility = View.GONE
                        totalLabelText.text = "Total (${localCurrency}) ="
                    }
                    TradeSubType.LIMIT -> {
                        radioButtonLimit.isChecked = true
                        amountUnitText.text = account.currency
                        limitLayout.visibility = View.VISIBLE
                        limitUnitText.text = localCurrency
                        limitLabelText.text = "Limit Price"
                        totalLabelText.text = "Total (${localCurrency}) ="

                    }
                    TradeSubType.STOP -> {
                        radioButtonStop.isChecked = true
                        amountUnitText.text = account.currency
                        limitLayout.visibility = View.VISIBLE
                        limitUnitText.text = localCurrency
                        limitLabelText.text = "Stop Price"
                        totalLabelText.text = "Total (${localCurrency}) ="

                    }
                }
            }
        }

    }

}
