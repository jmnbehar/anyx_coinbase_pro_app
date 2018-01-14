package com.jmnbehar.gdax.Fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_trade.view.*
import org.jetbrains.anko.*
import org.jetbrains.anko.support.v4.alert

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

    lateinit var submitOrderButton: Button

    var tradeSubType: TradeSubType = TradeSubType.MARKET

    var tradeType: TradeType = Companion.tradeType

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

        submitOrderButton = rootView.btn_place_order

        titleText.text = account.currency.toString()

        switchTradeType(tradeType, tradeSubType)

        amountEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                println(tradeType)
                println(tradeSubType)
                val amount = p0.toString().toDoubleOrZero()
                updateTotalText(amount)

            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        limitEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                val limitPrice = p0.toString().toDoubleOrZero()
                updateTotalText(limitPrice = limitPrice)
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })



        radioButtonBuy.setOnClickListener {
            switchTradeType(TradeType.BUY)
        }
        radioButtonSell.setOnClickListener {
            switchTradeType(TradeType.SELL)
        }

        radioButtonMarket.setOnClickListener {
            switchTradeType(tradeSubType =  TradeSubType.MARKET)
        }
        radioButtonLimit.setOnClickListener {
            switchTradeType(tradeSubType =  TradeSubType.LIMIT)
        }
        radioButtonStop.setOnClickListener {
            switchTradeType(tradeSubType =  TradeSubType.STOP)
        }




        submitOrderButton.setOnClickListener {
//            alert {
//                ankoView() {
//                    editText()
//                }
//            }.show()


            alert {
                title = "Alert"
                positiveButton("Confirm") { submitOrder() }

                val amount = amountEditText.text.toString().toDoubleOrZero()
                val limitPrice = limitEditText.text.toString().toDoubleOrZero()

                val cryptoTotal = totalInCrypto(amount, limitPrice)
                val dollarTotal = totalInDollars(amount, limitPrice)

                customView {
                    linearLayout {
                        verticalLayout {
                            linearLayout {
                                textView("Total ${account.currency}:")
                                textView( "%.8f".format(cryptoTotal)).lparams { textAlignment = right }
                                padding = dip(5)
                            }.lparams(width = matchParent) {}
                            linearLayout {
                                textView("Total $localCurrency:")
                                textView( "%.2f".format(dollarTotal)).lparams { textAlignment = right }
                                padding = dip(5)
                            }.lparams(width = matchParent) {}
                            linearLayout {
                                textView("Total fees:")
                                textView("%.2f".format(feeEstimate(dollarTotal))).lparams { textAlignment = right }
                                padding = dip(5)
                            }.lparams(width = matchParent) {}

                            checkBox("Don't show this again")
                        }.lparams(width = matchParent) {leftMargin = dip(10) }
                    }
                }

                positiveButton("Confirm") { submitOrder() }
                negativeButton("Cancel") { }
            }.show()
        }

        return rootView
    }


    private fun submitOrder() {
        val amount = amountEditText.text.toString().toDoubleOrNull()
//        var size: Double? = null

        fun onComplete(result: Result<ByteArray, FuelError>) {
            when (result) {
                is Result.Failure -> {
                    //error
                    println("Error!: ${result.error}")
                    toast("Error!: ${result.error}", context)
                }
                is Result.Success -> {
//                    val data = result.getAs()
//                    println("Success!: ${data}")
                    toast("success", context)
                }
            }
        }

        when(tradeSubType) {
            TradeSubType.MARKET -> {
                val productId = account.product.id
                when (tradeType) {
                    TradeType.BUY ->  GdaxApi.orderMarket(tradeType, productId, size = null, funds = amount).executePost { onComplete(it) }
                    TradeType.SELL -> GdaxApi.orderMarket(tradeType, productId, size = amount, funds = null).executePost { onComplete(it) }
                }
            }
            TradeSubType.LIMIT -> {
                val limitPrice = limitEditText.text.toString().toDoubleOrZero()
                GdaxApi.orderLimit(tradeType, account.product.id, limitPrice, amount ?: 0.0).executePost { onComplete(it) }
            }
            TradeSubType.STOP -> {
                val stopPrice = limitEditText.text.toString().toDoubleOrZero()
                when (tradeType) {
                    TradeType.BUY ->  GdaxApi.orderStop(tradeType, account.product.id, stopPrice, size = null, funds = amount).executePost { onComplete(it) }
                    TradeType.SELL -> GdaxApi.orderStop(tradeType, account.product.id, stopPrice, size = amount, funds = null).executePost { onComplete(it) }
                }
            }
        }
    }

    private fun updateTotalText(amount: Double = amountEditText.text.toString().toDoubleOrZero(), limitPrice: Double = limitEditText.text.toString().toDoubleOrZero()) {
        totalText.text = totalText(amount, limitPrice)
    }

    private fun totalText(amount: Double = amountEditText.text.toString().toDoubleOrZero(), limitPrice: Double = limitEditText.text.toString().toDoubleOrZero()) : String {
        return when (tradeType) {
            TradeType.BUY -> when (tradeSubType) {
                TradeSubType.MARKET ->  "%.8f".format(totalInCrypto(amount, limitPrice))
                TradeSubType.LIMIT -> "%.2f".format(totalInDollars(amount, limitPrice))
                TradeSubType.STOP ->  "%.8f".format(totalInCrypto(amount, limitPrice))
            }
            TradeType.SELL -> when (tradeSubType) {
                TradeSubType.MARKET ->  "%.2f".format(totalInDollars(amount, limitPrice))
                TradeSubType.LIMIT ->  "%.2f".format(totalInDollars(amount, limitPrice))
                TradeSubType.STOP ->  "%.2f".format(totalInDollars(amount, limitPrice))
            }
        }
    }

    private fun totalInDollars(amount: Double = amountEditText.text.toString().toDoubleOrZero(), limitPrice: Double = limitEditText.text.toString().toDoubleOrZero()) : Double {
        return when (tradeType) {
            TradeType.BUY -> when (tradeSubType) {
                TradeSubType.MARKET -> amount
                TradeSubType.LIMIT -> amount * limitPrice
                TradeSubType.STOP -> amount
            }
            TradeType.SELL -> when (tradeSubType) {
                TradeSubType.MARKET -> amount * account.product.price
                TradeSubType.LIMIT -> amount * limitPrice
                TradeSubType.STOP -> amount * limitPrice
            }
        }
    }

    private fun totalInCrypto(amount: Double = amountEditText.text.toString().toDoubleOrZero(), limitPrice: Double = limitEditText.text.toString().toDoubleOrZero()) : Double {
        return when (tradeType) {
            TradeType.BUY -> when (tradeSubType) {
                TradeSubType.MARKET -> amount / account.product.price
                TradeSubType.LIMIT -> amount
                TradeSubType.STOP -> amount/limitPrice
            }
            TradeType.SELL -> amount
        }
    }

    private fun feeEstimate(amount: Double) : Double {
        //TODO: make this work
        return when (tradeType) {
            TradeType.BUY -> when (tradeSubType) {
                TradeSubType.MARKET -> amount * .01
                TradeSubType.LIMIT -> 0.0
                TradeSubType.STOP -> 0.0
            }
            TradeType.SELL -> when (tradeSubType) {
                TradeSubType.MARKET -> amount * .01
                TradeSubType.LIMIT -> amount * .01
                TradeSubType.STOP -> amount * .01
            }
        }
    }

    private fun switchTradeType(tradeType: TradeType = this.tradeType, tradeSubType: TradeSubType = this.tradeSubType) {
        this.tradeType = tradeType
        this.tradeSubType = tradeSubType

        updateTotalText()
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
                        amountUnitText.text = account.currency.toString()
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
                        amountUnitText.text = account.currency.toString()
                        limitLayout.visibility = View.GONE
                        totalLabelText.text = "Total (${localCurrency}) ="
                    }
                    TradeSubType.LIMIT -> {
                        radioButtonLimit.isChecked = true
                        amountUnitText.text = account.currency.toString()
                        limitLayout.visibility = View.VISIBLE
                        limitUnitText.text = localCurrency
                        limitLabelText.text = "Limit Price"
                        totalLabelText.text = "Total (${localCurrency}) ="

                    }
                    TradeSubType.STOP -> {
                        radioButtonStop.isChecked = true
                        amountUnitText.text = account.currency.toString()
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
