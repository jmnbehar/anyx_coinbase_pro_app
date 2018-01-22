package com.jmnbehar.gdax.Fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_trade.view.*
import org.jetbrains.anko.*
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast

/**
 * Created by jmnbehar on 11/5/2017.
 */
class TradeFragment : RefreshFragment() {


    private lateinit var inflater: LayoutInflater
    private lateinit var titleText: TextView

    private lateinit var balancesLayout: LinearLayout
    private lateinit var usdBalanceText: TextView
    private lateinit var usdBalanceLabelText: TextView
    private lateinit var cryptoBalanceText: TextView
    private lateinit var cryptoBalanceLabelText: TextView

    private lateinit var radioButtonBuy: RadioButton
    private lateinit var radioButtonSell: RadioButton

    private lateinit var radioButtonMarket: RadioButton
    private lateinit var radioButtonLimit: RadioButton
    private lateinit var radioButtonStop: RadioButton

    private lateinit var amountEditText: EditText
    private lateinit var amountUnitText: TextView
    private lateinit var amountLabelText: TextView

    private lateinit var limitLayout: LinearLayout
    private lateinit var limitEditText: EditText
    private lateinit var limitUnitText: TextView
    private lateinit var limitLabelText: TextView

    private lateinit var totalLabelText: TextView
    private lateinit var totalText: TextView

    private lateinit var advancedOptionsCheckBox: CheckBox
    private lateinit var advancedOptionsLayout: LinearLayout
    private lateinit var advancedOptionsLimitLayout: LinearLayout
    private lateinit var advancedOptionsStopLayout: LinearLayout

    private lateinit var advancedOptionTimeInForceSpinner: Spinner

    private lateinit var advancedOptionStopLimitEditText: EditText
    private lateinit var advancedOptionStopLimitUser: TextView

    private lateinit var submitOrderButton: Button

    var tradeType: TradeType = TradeType.MARKET

    var tradeSide: TradeSide = Companion.tradeType

    companion object {
        var tradeType = TradeSide.BUY
        var localCurrency = "USD"

        lateinit var account: Account
        fun newInstance(accountIn: Account, tradeSideIn: TradeSide): TradeFragment {
            account = accountIn
            tradeType = tradeSideIn
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

        usdBalanceText = rootView.txt_trade_usd_balance
        usdBalanceLabelText = rootView.txt_trade_usd_balance_label
        cryptoBalanceText = rootView.txt_trade_crypto_balance
        cryptoBalanceLabelText = rootView.txt_trade_crypto_balance_label

        advancedOptionsCheckBox = rootView.cb_trade_advanced
        advancedOptionsLayout = rootView.layout_trade_advanced
        advancedOptionsLimitLayout = rootView.layout_trade_advanced_limit
        advancedOptionsStopLayout = rootView.layout_trade_advanced_stop

        totalLabelText = rootView.txt_trade_total_label
        totalText = rootView.txt_trade_total

        submitOrderButton = rootView.btn_place_order

        titleText.text = account.currency.toString()

        usdBalanceText.text = Account.usdAccount?.balance?.fiatFormat()

        cryptoBalanceLabelText.text = account.currency.toString()
        cryptoBalanceText.text = account.balance.btcFormat()

        switchTradeType(tradeSide, tradeType)

        amountEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                println(tradeSide)
                println(tradeType)
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
            switchTradeType(TradeSide.BUY)
        }
        radioButtonSell.setOnClickListener {
            switchTradeType(TradeSide.SELL)
        }

        radioButtonMarket.setOnClickListener {
            switchTradeType(tradeType =  TradeType.MARKET)
        }
        radioButtonLimit.setOnClickListener {
            switchTradeType(tradeType =  TradeType.LIMIT)
        }
        radioButtonStop.setOnClickListener {
            switchTradeType(tradeType =  TradeType.STOP)
        }
        advancedOptionsCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                advancedOptionsLayout.visibility = View.VISIBLE
            } else {
                advancedOptionsLayout.visibility = View.GONE
            }
        }

        val onFailure = { result: Result.Failure<String, FuelError> ->  println("Error!: ${result.error}") }
        submitOrderButton.setOnClickListener {
            val prefs = Prefs(activity)

            val amount = amountEditText.text.toString().toDoubleOrNull()
            val limit = limitEditText.text.toString().toDoubleOrNull()
            if (amount == null || amount <= 0) {
                toast("Amount is not valid")
            } else if ((tradeType == TradeType.LIMIT) && ((limit == null) || (limit <= 0.0))) {
                toast("Limit is not valid")
            } else if ((tradeType == TradeType.STOP) && ((limit == null) || (limit <= 0.0))) {
                toast("Stop is not valid")
            } else {
                if (prefs.shouldShowConfirmModal) {
                    GdaxApi.ticker(account.product.id).executeRequest(onFailure) { result ->
                        val ticker: ApiTicker = Gson().fromJson(result.value, object : TypeToken<ApiTicker>() {}.type)
                        val price = ticker.price.toDoubleOrNull()
                        if (price != null) {
                            account.updateAccount(price = price)
                            confirmPopup(price, amount, limit ?: 0.0)
                        }
                    }
                } else {
                    submitOrder(amount, limit ?: 0.0)
                }
            }
        }

        return rootView
    }

    private fun confirmPopup(updatedTicker: Double, amount: Double, limit: Double) {
        val cryptoTotal = totalInCrypto(amount, limit)
        val dollarTotal = totalInDollars(amount, limit)

        alert {
            title = "Alert"
            customView {
                linearLayout {
                    verticalLayout {
                        if (tradeType == TradeType.MARKET) {
                            linearLayout {
                                textView("${account.currency} price:")
                                textView(updatedTicker.fiatFormat()).lparams { textAlignment = right }
                                padding = dip(5)
                            }.lparams(width = matchParent) {}
                        }
                        linearLayout {
                            textView("Total ${account.currency}:")
                            textView(cryptoTotal.btcFormat()).lparams { textAlignment = right }
                            padding = dip(5)
                        }.lparams(width = matchParent) {}
                        linearLayout {
                            textView("Total $localCurrency:")
                            textView( dollarTotal.fiatFormat()).lparams { textAlignment = right }
                            padding = dip(5)
                        }.lparams(width = matchParent) {}
                        linearLayout {
                            textView("Estimated fees:")
                            textView(feeEstimate(dollarTotal, limit).fiatFormat()).lparams { textAlignment = right }
                            padding = dip(5)
                        }.lparams(width = matchParent) {}

                        checkBox("Don't show this again")
                    }.lparams(width = matchParent) {leftMargin = dip(10) }
                }
            }

            positiveButton("Confirm") {
                submitOrder(amount, limit)
            }
            negativeButton("Cancel") { }
        }.show()
    }

    private fun submitOrder(amount: Double, limitPrice: Double) {
//        var size: Double? = null

        fun onFailure(result: Result.Failure<ByteArray, FuelError>) {
            val errorCode = GdaxApi.ErrorCode.withCode(result.error.response.statusCode)
            when (errorCode) {
                GdaxApi.ErrorCode.BadRequest -> {toast("400 Error: Missing something from the request")}
                else -> GdaxApi.defaultPostFailure(result)
            }
        }

        fun onComplete(result: Result<ByteArray, FuelError>) {
            toast("success")
            activity.onBackPressed()
        }

        val productId = account.product.id
        when(tradeType) {
            TradeType.MARKET -> {
                when (tradeSide) {
                    TradeSide.BUY ->  GdaxApi.orderMarket(tradeSide, productId, size = null, funds = amount).executePost({ onFailure(it) }, { onComplete(it) })
                    TradeSide.SELL -> GdaxApi.orderMarket(tradeSide, productId, size = amount, funds = null).executePost({ onFailure(it) }, { onComplete(it) })
                }
            }
            TradeType.LIMIT -> {
                GdaxApi.orderLimit(tradeSide, account.product.id, limitPrice, amount ?: 0.0).executePost({ onFailure(it) }, { onComplete(it) })
            }
            TradeType.STOP -> {
                val stopPrice = limitPrice
                when (tradeSide) {
                    TradeSide.BUY ->  GdaxApi.orderStop(tradeSide, productId, stopPrice, size = null, funds = amount).executePost({ onFailure(it) }, { onComplete(it) })
                    TradeSide.SELL -> GdaxApi.orderStop(tradeSide, productId, stopPrice, size = amount, funds = null).executePost({ onFailure(it) }, { onComplete(it) })
                }
            }
        }
    }

    private fun updateTotalText(amount: Double = amountEditText.text.toString().toDoubleOrZero(), limitPrice: Double = limitEditText.text.toString().toDoubleOrZero()) {
        totalText.text = totalText(amount, limitPrice)
    }

    private fun totalText(amount: Double = amountEditText.text.toString().toDoubleOrZero(), limitPrice: Double = limitEditText.text.toString().toDoubleOrZero()) : String {
        return when (tradeSide) {
            TradeSide.BUY -> when (tradeType) {
                TradeType.MARKET ->  totalInCrypto(amount, limitPrice).btcFormat()
                TradeType.LIMIT -> totalInDollars(amount, limitPrice).fiatFormat()
                TradeType.STOP ->  totalInCrypto(amount, limitPrice).btcFormat()
            }
            TradeSide.SELL -> when (tradeType) {
                TradeType.MARKET ->  totalInDollars(amount, limitPrice).fiatFormat()
                TradeType.LIMIT ->  totalInDollars(amount, limitPrice).fiatFormat()
                TradeType.STOP ->  totalInDollars(amount, limitPrice).fiatFormat()
            }
        }
    }

    private fun totalInDollars(amount: Double = amountEditText.text.toString().toDoubleOrZero(), limitPrice: Double = limitEditText.text.toString().toDoubleOrZero()) : Double {
        return when (tradeSide) {
            TradeSide.BUY -> when (tradeType) {
                TradeType.MARKET -> amount
                TradeType.LIMIT -> amount * limitPrice
                TradeType.STOP -> amount
            }
            TradeSide.SELL -> when (tradeType) {
                TradeType.MARKET -> amount * account.product.price
                TradeType.LIMIT -> amount * limitPrice
                TradeType.STOP -> amount * limitPrice
            }
        }
    }

    private fun totalInCrypto(amount: Double = amountEditText.text.toString().toDoubleOrZero(), limitPrice: Double = limitEditText.text.toString().toDoubleOrZero()) : Double {
        return when (tradeSide) {
            TradeSide.BUY -> when (tradeType) {
                TradeType.MARKET -> amount / account.product.price
                TradeType.LIMIT -> amount
                TradeType.STOP -> amount/limitPrice
            }
            TradeSide.SELL -> amount
        }
    }

    private fun feeEstimate(amount: Double, limitPrice: Double?) : Double {
        val feePercentage = when (account.currency) {
            Currency.BTC -> 0.0025
            Currency.BCH -> 0.0025
            Currency.ETH -> 0.003
            Currency.LTC -> 0.003
            Currency.USD -> 0.0
        }

        val fee = amount * feePercentage
        return when (tradeType) {
            TradeType.MARKET -> fee
            TradeType.LIMIT -> if ((limitPrice != null) && (limitPrice >= account.product.price)) {
                0.0
            } else {
                fee
            }
            TradeType.STOP -> if ((limitPrice != null) && (limitPrice <= account.product.price)) {
                0.0
            } else {
                fee
            }
        }
    }

    enum class TimeInForce {
        GoodTilCancelled,
        GoodTilTime,
        ImmediateOrCancel,
        FirstOrKill;

        override fun toString(): String {
            return when (this) {
                GoodTilCancelled -> "GTC"
                GoodTilTime -> "GTT"
                ImmediateOrCancel -> "IOC"
                FirstOrKill -> "FOK"
            }
        }
        fun label(): String {
            return when (this) {
                GoodTilCancelled -> "Good Til Cancelled"
                GoodTilTime -> "Good Til Time"
                ImmediateOrCancel -> "Immediate Or Cancel"
                FirstOrKill -> "First Or Kill"
            }
        }
    }

    private fun switchTradeType(tradeSide: TradeSide = this.tradeSide, tradeType: TradeType = this.tradeType) {
        this.tradeSide = tradeSide
        this.tradeType = tradeType

        updateTotalText()

        if (advancedOptionsCheckBox.isChecked) {
            advancedOptionsLayout.visibility = View.VISIBLE
        } else {
            advancedOptionsLayout.visibility = View.GONE
        }
        when (tradeType) {
            TradeType.MARKET -> {
                radioButtonMarket.isChecked = true
                limitLayout.visibility = View.GONE
            }
            TradeType.LIMIT -> {
                radioButtonLimit.isChecked = true
                limitUnitText.text = localCurrency
                limitLayout.visibility = View.VISIBLE
                limitLabelText.text = "Limit Price"
                advancedOptionsLimitLayout.visibility = View.VISIBLE
                advancedOptionsStopLayout.visibility = View.GONE
                advancedOptionsCheckBox.visibility = View.VISIBLE
                //TODO: set Time In Force spinner
//                val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, list_of_items)
//                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//                advancedOptionTimeInForceSpinner.adapter = arrayAdapter
            }
            TradeType.STOP -> {
                radioButtonStop.isChecked = true
                limitUnitText.text = localCurrency
                limitLayout.visibility = View.VISIBLE
                limitLabelText.text = "Stop Price"
                advancedOptionsLimitLayout.visibility = View.GONE
                advancedOptionsStopLayout.visibility = View.VISIBLE
                advancedOptionsCheckBox.visibility = View.VISIBLE
            }
        }
        when (tradeSide) {
            TradeSide.BUY -> {
                radioButtonBuy.isChecked = true
                when (tradeType) {
                    TradeType.MARKET -> {
                        amountUnitText.text = localCurrency
                        totalLabelText.text = "Total (${account.currency}) ="
                    }
                    TradeType.LIMIT -> {
                        amountUnitText.text = account.currency.toString()
                        totalLabelText.text = "Total (${localCurrency}) ="
                    }
                    TradeType.STOP -> {
                        amountUnitText.text = localCurrency
                        totalLabelText.text = "Total (${account.currency}) ="

                    }
                }
            }
            TradeSide.SELL -> {
                radioButtonSell.isChecked = true
                when (tradeType) {
                    TradeType.MARKET -> {
                        amountUnitText.text = account.currency.toString()
                        totalLabelText.text = "Total (${localCurrency}) ="
                    }
                    TradeType.LIMIT -> {
                        amountUnitText.text = account.currency.toString()
                        totalLabelText.text = "Total (${localCurrency}) ="
                    }
                    TradeType.STOP -> {
                        amountUnitText.text = account.currency.toString()
                        totalLabelText.text = "Total (${localCurrency}) ="
                    }
                }
            }
        }

    }

}
