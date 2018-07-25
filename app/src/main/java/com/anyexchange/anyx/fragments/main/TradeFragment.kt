package com.anyexchange.anyx.fragments.main

import android.arch.lifecycle.LifecycleOwner
import android.os.Bundle
import android.support.design.widget.TabLayout
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
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.classes.CBProApi.ErrorMessage
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.fragment_trade.view.*
import org.jetbrains.anko.*
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast
import android.text.InputFilter



/**
 * Created by anyexchange on 11/5/2017.
 */
class TradeFragment : RefreshFragment(), LifecycleOwner {

    private lateinit var inflater: LayoutInflater
    private lateinit var titleText: TextView

    private lateinit var fiatBalanceText: TextView
    private lateinit var fiatBalanceLabelText: TextView
    private lateinit var cryptoBalanceText: TextView
    private lateinit var cryptoBalanceLabelText: TextView
    private lateinit var currentPriceLabelText: TextView
    private lateinit var currentPriceText: TextView

    private lateinit var tradeTypeTabLayout: TabLayout

    private lateinit var tradeSideBuyRadioButton : RadioButton
    private lateinit var tradeSideSellRadioButton: RadioButton

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

    private lateinit var advancedOptionTimeInForceSpinner: Spinner
    private lateinit var advancedOptionEndTimeSpinner: Spinner

    private lateinit var submitOrderButton: Button

    var tradeType: TradeType = TradeType.MARKET

    var tradeSide: TradeSide = Companion.tradeSide
    val account: Account?
        get() {
            return ChartFragment.account
        }

    companion object {
        var tradeSide = TradeSide.BUY
        var fiatCurrency = Currency.USD

        private var account: Account? = null
        fun newInstance(account: Account, tradeSide: TradeSide, fiatCurrency: Currency): TradeFragment {
            this.account = account
            this.tradeSide = tradeSide
            this.fiatCurrency = fiatCurrency
            return TradeFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_trade, container, false)

        this.inflater = inflater
        val activity = activity!!
        setupSwipeRefresh(rootView.swipe_refresh_layout)

        titleText = rootView.txt_trade_name

        amountLabelText = rootView.txt_trade_amount_label
        amountEditText = rootView.etxt_trade_amount
        amountUnitText = rootView.txt_trade_amount_unit

        limitLayout = rootView.layout_trade_limit
        limitLabelText = rootView.txt_trade_limit_label
        limitEditText = rootView.etxt_trade_limit
        limitUnitText = rootView.txt_trade_limit_unit

        fiatBalanceText = rootView.txt_trade_fiat_balance
        fiatBalanceLabelText = rootView.txt_trade_fiat_balance_label
        cryptoBalanceText = rootView.txt_trade_crypto_balance
        cryptoBalanceLabelText = rootView.txt_trade_crypto_balance_label
        currentPriceLabelText = rootView.txt_trade_crypto_current_price_label
        currentPriceText = rootView.txt_trade_crypto_current_price

        advancedOptionsCheckBox = rootView.cb_trade_advanced
        advancedOptionsLayout = rootView.layout_trade_advanced

        advancedOptionTimeInForceSpinner = rootView.spinner_trade_time_in_force
        advancedOptionEndTimeSpinner = rootView.spinner_trade_good_til_time

        totalLabelText = rootView.txt_trade_total_label
        totalText = rootView.txt_trade_total

        submitOrderButton = rootView.btn_place_order

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
        amountEditText.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(4, 8))

        limitEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                val limitPrice = p0.toString().toDoubleOrZero()
                updateTotalText(limitPrice = limitPrice)
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        limitEditText.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(5, 2))



        tradeSideBuyRadioButton = rootView.rbtn_trade_buy
        tradeSideSellRadioButton = rootView.rbtn_trade_sell

        tradeSideBuyRadioButton.setOnClickListener {
            switchTradeType(TradeSide.BUY)
            if (tradeType != TradeType.LIMIT) {
                amountEditText.setText("")
            }
        }
        tradeSideSellRadioButton.setOnClickListener {
            switchTradeType(TradeSide.SELL)
            if (tradeType != TradeType.LIMIT) {
                amountEditText.setText("")
            }
        }


        tradeTypeTabLayout = rootView.tabl_trade_type
        tradeTypeTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when(tab.position) {
                    0 -> switchTradeType(newTradeType =  TradeType.MARKET)
                    1 -> switchTradeType(newTradeType =  TradeType.LIMIT)
                    2 -> switchTradeType(newTradeType =  TradeType.STOP)
                    else -> switchTradeType(newTradeType =  TradeType.MARKET)
                }
                amountEditText.setText("")
                limitEditText.setText("")
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

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

            var amount = amountEditText.text.toString().toDoubleOrZero()
            val limit = limitEditText.text.toString().toDoubleOrZero()

            var timeInForce: CBProApi.TimeInForce? = null
            var cancelAfter: String? = null

            val cryptoTotal = totalInCrypto(amount, limit)
            val dollarTotal = totalInDollars(amount, limit)
            val feeEstimate = feeEstimate(dollarTotal, limit)
            val devFee: Double

            if (feeEstimate > 0.0) {
                //half fees for limit orders that are fuckin up? consider this later
                devFee = cryptoTotal * DEV_FEE_PERCENTAGE

                //TODO: test this more
                if (tradeSide == TradeSide.SELL) {
                    amount -= devFee
                    //don't sell the devFee because it will be deducted by sending fees
                    //amount is always in crypto for sell orders
                }
                //should never need to change amount here, just buy it all and then deduct a bit
            } else {
                devFee = 0.0
            }

            if (advancedOptionsCheckBox.isChecked) {
                when (tradeType) {
                    TradeType.LIMIT -> {
                        val tifIndex = advancedOptionTimeInForceSpinner.selectedItemPosition
                        timeInForce = CBProApi.TimeInForce.values()[tifIndex]
                        if (timeInForce == CBProApi.TimeInForce.GoodTilTime) {
                            cancelAfter = advancedOptionEndTimeSpinner.selectedItem as String
                        }
                    }
                    TradeType.STOP -> { /* consider adding stop limit if that becomes possible */ }
                    TradeType.MARKET -> { /* do nothing */ }
                }
            }

            if (amount <= 0) {
                toast(R.string.trade_invalid_amount)
            } else if ((tradeType == TradeType.LIMIT) &&  (limit <= 0.0)) {
                toast(R.string.trade_invalid_limit)
            } else if ((tradeType == TradeType.STOP) && (limit <= 0.0)) {
                toast(R.string.trade_invalid_stop)
            } else {
                if (prefs.shouldShowTradeConfirmModal) {
                    account?.let { account ->
                        CBProApi.ticker(account.product.id).executeRequest(onFailure) { result ->
                            val ticker: ApiTicker = Gson().fromJson(result.value, object : TypeToken<ApiTicker>() {}.type)
                            val price = ticker.price.toDoubleOrNull()
                            if (price != null) {
                                account.product.price = price
                                confirmPopup(price, amount, limit, devFee, timeInForce, cancelAfter, cryptoTotal, dollarTotal, feeEstimate)
                            }
                        }
                    }
                } else {
                    submitOrder(amount, limit, devFee, timeInForce, cancelAfter)
                }
            }
        }

        doneLoading()

        return rootView
    }

    override fun onResume() {
        super.onResume()

        showNavSpinner(ChartFragment.account?.currency) { selectedCurrency ->
            val selectedAccount = Account.forCurrency(selectedCurrency)
            if (selectedAccount != null) {
                ChartFragment.account = selectedAccount
                amountEditText.setText("")
                limitEditText.setText("")
                updateButtonsAndText()
            }
        }

        updateButtonsAndText()
    }


    override fun refresh(onComplete: (Boolean) -> Unit) {
        val onFailure = { result: Result.Failure<String, FuelError> ->
            toast(resources.getString(R.string.error_generic_message, result.errorMessage))
            println("error!" )}
        account?.update(onFailure) {
            if (lifecycle.isCreatedOrResumed) {
                updateButtonsAndText()
                onComplete(false)
            }
        }
        account?.let { account ->
            CBProApi.ticker(account.product.id).executeRequest(onFailure) { result ->
                val ticker: ApiTicker = Gson().fromJson(result.value, object : TypeToken<ApiTicker>() {}.type)
                val price = ticker.price.toDoubleOrNull()
                if (price != null) {
                    account.product.price = price
                }
                updateButtonsAndText()
                onComplete(true)
            }
        }
    }

    private fun updateButtonsAndText() {
        account?.let { account ->
            context?.let { context ->
                val buttonColors = account.currency.colorStateList(context)
                val buttonTextColor = account.currency.buttonTextColor(context)
                submitOrderButton.backgroundTintList = buttonColors
                submitOrderButton.textColor = buttonTextColor

                val tabAccentColor = account.currency.colorAccent(activity!!)
                tradeTypeTabLayout.setSelectedTabIndicatorColor(tabAccentColor)

                titleText.text = resources.getString(R.string.trade_title_for_currency, account.currency.toString())

                val fiatAccount = Account.fiatAccount
                fiatBalanceText.text = fiatAccount?.availableBalance?.fiatFormat()
                val fiatCurrency = Account.fiatAccount?.currency ?: Prefs(context).preferredFiat
                fiatBalanceLabelText.text = resources.getString(R.string.trade_balance_label, fiatCurrency.toString())
                cryptoBalanceLabelText.text = resources.getString(R.string.trade_balance_label, account.currency)

                cryptoBalanceText.text = account.availableBalance.btcFormat()

                currentPriceLabelText.text = resources.getString(R.string.trade_last_trade_price_label, account.currency)
                currentPriceText.text = account.product.price.fiatFormat()
            }
        }
        updateTotalText()

        switchTradeType()
    }

    private fun confirmPopup(updatedTicker: Double, amount: Double, limit: Double, devFee: Double, timeInForce: CBProApi.TimeInForce?, cancelAfter: String?,
                             cryptoTotal: Double, dollarTotal: Double, feeEstimate: Double) {
        val currencyString = account?.currency?.toString() ?: ""
        val feeEstimateString = if (feeEstimate > 0 && feeEstimate < 0.01) {
            "less than $0.01"
        } else {
            feeEstimate.fiatFormat()
        }
        val buySell = if (tradeSide == TradeSide.BUY) { "buy" } else { "sell" }
        alert {
            title = resources.getString(R.string.trade_confirm_popup_title)
            customView {
                linearLayout {
                    verticalLayout {
                        if (tradeType == TradeType.MARKET) {
                            horizontalLayout(resources.getString(R.string.trade_confirm_popup_price_label, currencyString), "≈${updatedTicker.fiatFormat()}").lparams(width = matchParent) {}
                        }
                        horizontalLayout(resources.getString(R.string.trade_confirm_popup_currency_label, currencyString, buySell), cryptoTotal.btcFormat()).lparams(width = matchParent) {}
                        horizontalLayout(resources.getString(R.string.trade_confirm_popup_estimated_fees_label), feeEstimateString).lparams(width = matchParent) {}
                        horizontalLayout(resources.getString(R.string.trade_confirm_popup_total_label), dollarTotal.fiatFormat()).lparams(width = matchParent) {}
                    }.lparams(width = matchParent) {leftMargin = dip(10) }
                }
            }
            positiveButton(R.string.trade_confirm_popup_confirm_btn) {
                submitOrder(amount, limit, devFee, timeInForce, cancelAfter)
            }
            negativeButton(R.string.trade_confirm_popup_cancel_btn) { }
        }.show()
    }

    private fun tradeAmountSizeError(errorMessage: CBProApi.ErrorMessage) : String {
        val currency: Currency = when (errorMessage) {
            ErrorMessage.BuyAmountTooSmallBtc, ErrorMessage.BuyAmountTooLargeBtc -> Currency.BTC
            ErrorMessage.BuyAmountTooSmallEth, ErrorMessage.BuyAmountTooLargeEth -> Currency.ETH
            ErrorMessage.BuyAmountTooSmallBch, ErrorMessage.BuyAmountTooLargeBch -> Currency.BCH
            ErrorMessage.BuyAmountTooSmallLtc, ErrorMessage.BuyAmountTooLargeLtc -> Currency.LTC
            else -> Currency.USD
        }
        val limit = when (errorMessage) {
            ErrorMessage.BuyAmountTooSmallBtc -> "0.001"
            ErrorMessage.BuyAmountTooSmallEth -> "0.01"
            ErrorMessage.BuyAmountTooSmallBch -> "0.01"
            ErrorMessage.BuyAmountTooSmallLtc -> "0.1"
            ErrorMessage.BuyAmountTooLargeBtc -> "70"
            ErrorMessage.BuyAmountTooLargeEth -> "700"
            ErrorMessage.BuyAmountTooLargeBch -> "350"
            ErrorMessage.BuyAmountTooLargeLtc -> "4000"
            else -> "0"
        }
        return when (errorMessage) {
            ErrorMessage.BuyAmountTooSmallBtc,
            ErrorMessage.BuyAmountTooSmallEth,
            ErrorMessage.BuyAmountTooSmallBch,
            ErrorMessage.BuyAmountTooSmallLtc -> resources.getString(R.string.trade_amount_buy_min_error_message, currency, limit)
            ErrorMessage.BuyAmountTooLargeBtc,
            ErrorMessage.BuyAmountTooLargeEth,
            ErrorMessage.BuyAmountTooLargeBch,
            ErrorMessage.BuyAmountTooLargeLtc -> resources.getString(R.string.trade_amount_buy_max_error_message, currency, limit)
            else -> ""
        }
    }

    private fun submitOrder(amount: Double, limitPrice: Double, devFee: Double, timeInForce: CBProApi.TimeInForce? = null, cancelAfter: String?) {
        fun onFailure(result: Result.Failure<ByteArray, FuelError>) {
            val errorMessage = CBProApi.ErrorMessage.forString(result.errorMessage)
            when (errorMessage) {
                ErrorMessage.BuyAmountTooSmallBtc,
                ErrorMessage.BuyAmountTooSmallEth,
                ErrorMessage.BuyAmountTooSmallBch,
                ErrorMessage.BuyAmountTooSmallLtc,
                ErrorMessage.BuyAmountTooLargeBtc,
                ErrorMessage.BuyAmountTooLargeEth,
                ErrorMessage.BuyAmountTooLargeBch,
                ErrorMessage.BuyAmountTooLargeLtc -> showPopup(tradeAmountSizeError(errorMessage), { })

                ErrorMessage.PriceTooAccurate,
                ErrorMessage.InsufficientFunds -> showPopup(resources.getString(R.string.error_generic_message, result.errorMessage), { })
                else -> showPopup(resources.getString(R.string.error_generic_message, result.errorMessage), { })
            }
        }

        fun onComplete(result: Result<ByteArray, FuelError>) {
            toast(R.string.toast_success)
            activity!!.onBackPressed()
            account?.let { account ->
                val currency = account.currency
                if (devFee > 0.0) {
                    val prefs = Prefs(context!!)
                    val unpaidFees = prefs.addUnpaidFee(devFee, currency)
                    if (unpaidFees > currency.minSendAmount) {
                        payFee(unpaidFees)
                        prefs.wipeUnpaidFees(currency)
                    }
                }
            }
        }

        val productId = account?.product?.id
        if (productId == null) {
            toast(R.string.error_message)
        } else {
            when(tradeType) {
                TradeType.MARKET -> {
                    when (tradeSide) {
                        TradeSide.BUY ->  CBProApi.orderMarket(tradeSide, productId, size = null, funds = amount).executePost({ onFailure(it) }, { onComplete(it) })
                        TradeSide.SELL -> CBProApi.orderMarket(tradeSide, productId, size = amount, funds = null).executePost({ onFailure(it) }, { onComplete(it) })
                    }
                }
                TradeType.LIMIT -> {
                    CBProApi.orderLimit(tradeSide, productId, limitPrice, amount, timeInForce = timeInForce, cancelAfter = cancelAfter).executePost({ onFailure(it) }, { onComplete(it) })
                }
                TradeType.STOP -> {
                    val stopPrice = limitPrice
                    when (tradeSide) {
                        TradeSide.BUY ->  CBProApi.orderStop(tradeSide, productId, stopPrice, size = null, funds = amount).executePost({ onFailure(it) }, { onComplete(it) })
                        TradeSide.SELL -> CBProApi.orderStop(tradeSide, productId, stopPrice, size = amount, funds = null).executePost({ onFailure(it) }, { onComplete(it) })
                    }
                }
            }
        }
    }

    private fun payFee(amount: Double) {
        account?.currency?.let { currency ->
            val destination = currency.developerAddress
            CBProApi.sendCrypto(amount, currency, destination).executePost(
                    { _ ->
                        /*  fail silently   */
                        println("failure")
                    },
                    { _ ->
                        /* succeed silently */
                        println("success")
                    })
        }
    }

    private fun updateTotalText(amount: Double = amountEditText.text.toString().toDoubleOrZero(), limitPrice: Double = limitEditText.text.toString().toDoubleOrZero()) {
        totalText.text = when (tradeSide) {
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
                TradeType.MARKET -> amount * (account?.product?.price ?: 0.0)
                TradeType.LIMIT -> amount * limitPrice
                TradeType.STOP -> amount * limitPrice
            }
        }
    }

    private fun totalInCrypto(amount: Double = amountEditText.text.toString().toDoubleOrZero(), limitPrice: Double = limitEditText.text.toString().toDoubleOrZero()) : Double {
        return when (tradeSide) {
            TradeSide.BUY -> when (tradeType) {
                TradeType.MARKET -> amount / (account?.product?.price ?: 0.0)
                TradeType.LIMIT -> amount
                TradeType.STOP -> if (limitPrice > 0.0) {
                    amount / limitPrice
                } else {
                    0.00
                }
            }
            TradeSide.SELL -> amount
        }
    }

    private fun feeEstimate(amount: Double, limit: Double) : Double {
        val price = account?.product?.price

        if (price != null) {
            when (tradeSide) {
                TradeSide.BUY -> {
                    when (tradeType) {
                        TradeType.MARKET -> { }
                        TradeType.LIMIT -> if (limit <= price) {
                            return 0.0
                        }
                        TradeType.STOP -> if (limit >= price) {
                            return 0.0
                        }
                    }
                }
                TradeSide.SELL -> {
                    when (tradeType) {
                        TradeType.MARKET -> { }
                        TradeType.LIMIT -> if (limit >= price) {
                            return 0.0
                        }
                        TradeType.STOP -> if (limit <= price) {
                            return 0.0
                        }
                    }
                }
            }
        }
        val cbproFee = amount * (account?.currency?.feePercentage ?: 0.005)
        val devFee  = amount * DEV_FEE_PERCENTAGE
        return (devFee + cbproFee)
    }


    private fun switchTradeType(newTradeSide: TradeSide? = null, newTradeType: TradeType? = null) {
        if (newTradeSide != null) {
            this.tradeSide = newTradeSide
        }
        if (newTradeType != null) {
            this.tradeType = newTradeType
        }

        updateTotalText()

        if (advancedOptionsCheckBox.isChecked && tradeType == TradeType.LIMIT) {
            advancedOptionsLayout.visibility = View.VISIBLE
        } else {
            advancedOptionsLayout.visibility = View.GONE
        }
        when (tradeType) {
            TradeType.MARKET -> {
                val marketTab = tradeTypeTabLayout.getTabAt(0)
                amountEditText.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(10, 8))

                marketTab?.select()
                limitLayout.visibility = View.INVISIBLE
            }
            TradeType.LIMIT -> {
                val limitTab = tradeTypeTabLayout.getTabAt(1)
                amountEditText.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(4, 8))
                limitTab?.select()
                limitUnitText.text = fiatCurrency.toString()
                limitLayout.visibility = View.VISIBLE
                limitLabelText.text = resources.getString(R.string.trade_limit_label)

                advancedOptionsCheckBox.visibility = View.VISIBLE

                val timeInForceList = CBProApi.TimeInForce.values()
                val spinnerList = timeInForceList.map { t -> t.label() }
                //TODO: don't use simple_spinner_item
                val arrayAdapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, spinnerList)
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                advancedOptionTimeInForceSpinner.adapter = arrayAdapter
                advancedOptionTimeInForceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                        val selectedItem = timeInForceList[position]
                        if (selectedItem == CBProApi.TimeInForce.GoodTilTime) {
                            advancedOptionEndTimeSpinner.visibility = View.VISIBLE
                        } else {
                            advancedOptionEndTimeSpinner.visibility = View.INVISIBLE
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {
                        advancedOptionEndTimeSpinner.visibility = View.INVISIBLE
                    }
                }

                val endTimeList = listOf("min", "hour", "day")
                val endTimeArrayAdapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, endTimeList)
                endTimeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                advancedOptionEndTimeSpinner.adapter = endTimeArrayAdapter
            }
            TradeType.STOP -> {
                amountEditText.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(4, 8))
                val stopTab = tradeTypeTabLayout.getTabAt(2)
                stopTab?.select()
                limitUnitText.text = fiatCurrency.toString()
                limitLayout.visibility = View.VISIBLE
                limitLabelText.text = resources.getString(R.string.trade_stop_label)
                advancedOptionsCheckBox.visibility = View.INVISIBLE
            }
        }

        account?.let { account ->
            when (tradeSide) {
                TradeSide.BUY -> {
                    submitOrderButton.text = resources.getString(R.string.trade_buy_order_btn)
                    tradeSideBuyRadioButton.isChecked = true
                    when (tradeType) {
                        TradeType.MARKET, TradeType.STOP -> {
                            amountUnitText.text = fiatCurrency.toString()
                            totalLabelText.text = resources.getString(R.string.trade_total_label, account.currency)
                        }
                        TradeType.LIMIT -> {
                            amountUnitText.text = account.currency.toString()
                            totalLabelText.text = resources.getString(R.string.trade_total_label, fiatCurrency)
                        }
                    }
                }
                TradeSide.SELL -> {
                    submitOrderButton.text =  resources.getString(R.string.trade_sell_order_btn)
                    tradeSideSellRadioButton.isChecked = true
                    amountUnitText.text = account.currency.toString()
                    totalLabelText.text = resources.getString(R.string.trade_total_label, fiatCurrency)
                }
            }
        }
    }

}
