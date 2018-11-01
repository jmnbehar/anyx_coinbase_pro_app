package com.anyexchange.anyx.fragments.main

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
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
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.classes.api.CBProApi.ErrorMessage
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.fragment_trade.view.*
import org.jetbrains.anko.*
import android.text.InputFilter
import com.anyexchange.anyx.adapters.spinnerAdapters.*
import com.anyexchange.anyx.classes.Currency
import com.anyexchange.anyx.classes.api.AnyApi
import com.anyexchange.anyx.classes.api.CBProApi

/**
 * Created by anyexchange on 11/5/2017.
 */
class TradeFragment : RefreshFragment(), LifecycleOwner {

    private var inflater: LayoutInflater? = null
    private var titleText: TextView? = null

    private var tradingPairSpinner: Spinner? = null

    private var quoteBalanceText: TextView? = null
    private var quoteBalanceLabelText: TextView? = null
    private var cryptoBalanceText: TextView? = null
    private var cryptoBalanceLabelText: TextView? = null
    private var currentPriceLabelText: TextView? = null
    private var currentPriceText: TextView? = null

    private var tradeTypeTabLayout: TabLayout? = null

    private var tradeSideBuyRadioButton : RadioButton? = null
    private var tradeSideSellRadioButton: RadioButton? = null

    private var amountEditText: EditText? = null
    private var amountLabelText: TextView? = null
    private var amountUnitText: TextView? = null
    private var amountUnitSpinner: Spinner? = null

    private var limitLayout: LinearLayout? = null
    private var limitEditText: EditText? = null
    private var limitLabelText: TextView? = null
    private var limitUnitText: TextView? = null

    private var advancedOptionsCheckBox: CheckBox? = null
    private var advancedOptionsLayout: LinearLayout? = null
    private lateinit var summaryText: TextView


    private var advancedOptionTimeInForceSpinner: Spinner? = null
    private var advancedOptionEndTimeSpinner: Spinner? = null

    private var submitOrderButton: Button? = null

    private var tradeType: TradeType = TradeType.MARKET

    val product: Product
        get() {
            return ChartFragment.product
        }

    val currency: Currency
        get() {
            return product.currency
        }

    private var tradingPair: TradingPair
        get() = viewModel.tradingPair ?: product.defaultTradingPair ?: product.tradingPairs.firstOrNull() ?: TradingPair(Exchange.CBPro, currency, Account.defaultFiatCurrency)
        set(value) {
            viewModel.tradingPair = value
        }

    private lateinit var viewModel: TradeViewModel
    class TradeViewModel : ViewModel() {
        var tradingPair: TradingPair? = null
    }

    private val relevantAccount: Account?
        get() {
            return product.accounts[tradingPair.exchange]
        }

    private val amountUnitCurrency: Currency?
        get() {
            return amountUnitSpinner?.selectedItem as? Currency
        }

    private val isAmountFunds: Boolean
        get() {
            return amountUnitCurrency == tradingPair.quoteCurrency
        }

    companion object {
        var tradeSide = TradeSide.BUY

        fun newInstance(tradeSide: TradeSide): TradeFragment {
            this.tradeSide = tradeSide
            return TradeFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_trade, container, false)

        viewModel = ViewModelProviders.of(this).get(TradeViewModel::class.java)

        this.inflater = inflater

        setupSwipeRefresh(rootView.swipe_refresh_layout)

        titleText = rootView.txt_trade_name

        amountLabelText = rootView.txt_trade_amount_label
        amountEditText = rootView.etxt_trade_amount
        amountUnitText = rootView.txt_trade_amount_unit

        limitLayout = rootView.layout_trade_limit
        limitLabelText = rootView.txt_trade_limit_label
        limitEditText = rootView.etxt_trade_limit
        limitUnitText = rootView.txt_trade_limit_unit

        amountUnitSpinner = rootView.spinner_trade_amount_unit

        tradingPairSpinner = rootView.spinner_trade_trading_pair

        quoteBalanceText = rootView.txt_trade_fiat_balance
        quoteBalanceLabelText = rootView.txt_trade_fiat_balance_label
        cryptoBalanceText = rootView.txt_trade_crypto_balance
        cryptoBalanceLabelText = rootView.txt_trade_crypto_balance_label
        currentPriceLabelText = rootView.txt_trade_crypto_current_price_label
        currentPriceText = rootView.txt_trade_crypto_current_price

        advancedOptionsCheckBox = rootView.cb_trade_advanced
        advancedOptionsLayout = rootView.layout_trade_advanced

        advancedOptionTimeInForceSpinner = rootView.spinner_trade_time_in_force
        advancedOptionEndTimeSpinner = rootView.spinner_trade_good_til_time

        summaryText = rootView.txt_trade_summary

        submitOrderButton = rootView.btn_place_order

        amountEditText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                val amount = p0.toString().toDoubleOrZero()
                updateTotalText(amount, null)
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        amountEditText?.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(10, 8))

        limitEditText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                val limitPrice = p0.toString().toDoubleOrZero()
                updateTotalText(null, limitPrice)
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        limitEditText?.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(10, 8))


        tradeSideBuyRadioButton = rootView.rbtn_trade_buy
        tradeSideSellRadioButton = rootView.rbtn_trade_sell

        tradeSideBuyRadioButton?.setOnClickListener {
            switchTradeSide(TradeSide.BUY)
            if (tradeType != TradeType.LIMIT) {
                amountEditText?.setText("")
            }
        }
        tradeSideSellRadioButton?.setOnClickListener {
            switchTradeSide(TradeSide.SELL)
            if (tradeType != TradeType.LIMIT) {
                amountEditText?.setText("")
            }
        }


        tradeTypeTabLayout = rootView.tabl_trade_type
        tradeTypeTabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when(tab.position) {
                    0 -> switchTradeType(newTradeType =  TradeType.MARKET)
                    1 -> switchTradeType(newTradeType =  TradeType.LIMIT)
                    2 -> switchTradeType(newTradeType =  TradeType.STOP)
                    else -> switchTradeType(newTradeType =  TradeType.MARKET)
                }
                amountEditText?.setText("")
                limitEditText?.setText("")
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        advancedOptionsCheckBox?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                advancedOptionsLayout?.visibility = View.VISIBLE
            } else {
                advancedOptionsLayout?.visibility = View.GONE
            }
        }

        context?.let { context ->

            val relevantCurrencies = listOf(tradingPair.quoteCurrency, tradingPair.baseCurrency)

            amountUnitSpinner?.adapter = CurrencySpinnerAdapter(context, relevantCurrencies)
            amountUnitSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    amountEditText?.setText("")
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            submitOrderButton?.setOnClickListener { submitOrder() }
        }

        dismissProgressSpinner()

        return rootView
    }

    override fun onResume() {
        super.onResume()

        switchCurrency(currency)
        val currencyList = Product.map.keys.map { Currency(it) }
        showNavSpinner(ChartFragment.currency, currencyList) { selectedCurrency ->
            switchCurrency(selectedCurrency)
        }

        updateButtonsAndText()
        refresh { endRefresh() }
    }

    private fun switchCurrency(newCurrency: Currency) {
        ChartFragment.currency = newCurrency

        val tradingPairs = product.tradingPairs.sortTradingPairs()
        val relevantTradingPair = tradingPairs.find { it.quoteCurrency == tradingPair.quoteCurrency }
        if (relevantTradingPair != null) {
//            val index = tradingPairs.indexOf(relevantTradingPair)
//            spinner_chart_trading_pair.setSelection(index)
            tradingPair = relevantTradingPair
        } else if (tradingPairs.isNotEmpty()){
            tradingPair = tradingPairs.first()
        } else {
            //TODO: something smarter here
            assert(false)
        }
        updateCurrencySpinner()

        amountEditText?.setText("")
        limitEditText?.setText("")
        updateButtonsAndText()
    }

    private fun updateCurrencySpinner() {
        val relevantCurrencies = listOf(tradingPair.quoteCurrency, tradingPair.baseCurrency)
        (amountUnitSpinner?.adapter as CurrencySpinnerAdapter).currencyList = relevantCurrencies
        (amountUnitSpinner?.adapter as CurrencySpinnerAdapter).notifyDataSetChanged()
    }

    override fun refresh(onComplete: (Boolean) -> Unit) {
        val onFailure: (result: Result.Failure<String, FuelError>) -> Unit = { result ->
            if (context != null) {
                toast(resources.getString(R.string.error_generic_message, result.errorMessage))
            }
        }

        val account = relevantAccount
        account?.update(apiInitData, onFailure) {
            if (lifecycle.isCreatedOrResumed) {
                updateButtonsAndText()
                onComplete(false)
            }
        }
        AnyApi.ticker(apiInitData, tradingPair, onFailure) {
            updateButtonsAndText()
            onComplete(true)
        }
    }

    private fun updateButtonsAndText() {
        context?.let { context ->
                tradingPairSpinner?.adapter = TradingPairSpinnerAdapter(context, product.tradingPairs, TradingPairSpinnerAdapter.ExchangeDisplayType.FullName)
                tradingPairSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        switchTradingPair(product.tradingPairs[position])
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) { }
                }


            relevantAccount?.let { account ->
                val buttonColors = account.currency.colorStateList(context)
                val buttonTextColor = account.currency.buttonTextColor(context)
                submitOrderButton?.backgroundTintList = buttonColors
                submitOrderButton?.textColor = buttonTextColor

                val tabAccentColor = account.currency.colorPrimary(context)
                tradeTypeTabLayout?.setSelectedTabIndicatorColor(tabAccentColor)

                titleText?.text = resources.getString(R.string.trade_title_for_currency, account.currency.fullName)

                val quoteCurrency = tradingPair.quoteCurrency
                val quoteBalance: String? = when {
                    quoteCurrency.isFiat -> Account.fiatAccounts.find { it.currency == quoteCurrency }?.availableBalance?.format(quoteCurrency)
                    else -> Product.map[quoteCurrency.id]?.accounts?.get(tradingPair.exchange)?.availableBalance?.format(currency) + " " + quoteCurrency.id
                }

                quoteBalanceText?.visibility = View.GONE
                quoteBalanceLabelText?.visibility = View.GONE

                quoteBalanceText?.text = quoteBalance ?: "0.0"
                quoteBalanceLabelText?.text = resources.getString(R.string.trade_balance_label, quoteCurrency.toString())

                cryptoBalanceLabelText?.text = resources.getString(R.string.trade_balance_label, account.currency)

                cryptoBalanceText?.text = account.availableBalance.format(currency)

                currentPriceLabelText?.text = resources.getString(R.string.trade_last_trade_price_label, account.currency)

                currentPriceText?.text = when {
                    quoteCurrency.isFiat -> product.priceForQuoteCurrency(quoteCurrency).format(quoteCurrency)
                    else -> product.priceForQuoteCurrency(quoteCurrency).format(currency) + " " + quoteCurrency.id
                }
            }
        }
        updateTotalText(null, null)

        switchTradeInfo(null, null, null)
    }

    private fun submitOrder() {
        val onFailure: (result: Result.Failure<String, FuelError>) -> Unit = { result ->  toast("Error: ${result.errorMessage}") }


        var timeInForce: TimeInForce? = null
        var cancelAfter: TimeInForce.CancelAfter? = null

        if (advancedOptionsCheckBox?.isChecked == true) {
            when (tradeType) {
                TradeType.LIMIT -> {
                    val tifIndex = advancedOptionTimeInForceSpinner?.selectedItemPosition ?: 0
                    timeInForce = TimeInForce.values()[tifIndex]
                    if (timeInForce == TimeInForce.GoodTilTime) {
                        cancelAfter = advancedOptionEndTimeSpinner?.selectedItem as TimeInForce.CancelAfter
                    }
                }
                TradeType.STOP -> { /* consider adding stop limit if that becomes possible */ }
                TradeType.MARKET -> { /* do nothing */ }
            }
        }

        val limit = limitEditText?.text.toString().toDoubleOrNull()

        val amount = amountEditText?.text.toString().toDoubleOrZero()

        val newOrder = if (isAmountFunds) {
            NewOrder(tradingPair, limit, null, amount, tradeType, tradeSide, timeInForce, cancelAfter, null)
        } else {
            NewOrder(tradingPair, limit, amount, null, tradeType, tradeSide, timeInForce, cancelAfter, null)
        }

        if (amount <= 0) {
            toast(R.string.trade_invalid_amount)
        } else if ((tradeType == TradeType.LIMIT) && (limit == null || limit <= 0.0)) {
            toast(R.string.trade_invalid_limit)
        } else if ((tradeType == TradeType.STOP)  && (limit == null || limit <= 0.0)) {
            toast(R.string.trade_invalid_stop)
        } else if (context != null && !Prefs(context!!).shouldShowTradeConfirmModal) {
            submitOrder(newOrder)
        } else {

            AnyApi.ticker(apiInitData, tradingPair, onFailure) { price ->
                if (price == null) {
                    onFailure(Result.Failure(FuelError(Exception())))
                } else {
                    showDialog(price, newOrder)
                }
            }
        }
    }

    private fun showDialog(updatedTicker: Double, newOrder: NewOrder) {
        val confirmDialogFragment = TradeConfirmFragment()
        confirmDialogFragment.setInfo(updatedTicker, newOrder)
        confirmDialogFragment.showNow(fragmentManager, "confirmDialog")
    }

    private fun tradeAmountSizeError(errorMessage: CBProApi.ErrorMessage) : String {
        val currency: Currency = when (errorMessage) {
            //TODO: \make this smarter so it doesn't explicitly specify currencies
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

    private fun submitOrder(newOrder: NewOrder) {
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
                ErrorMessage.BuyAmountTooLargeLtc -> showPopup(tradeAmountSizeError(errorMessage)) { }

                ErrorMessage.PriceTooAccurate,
                ErrorMessage.InsufficientFunds -> showPopup(resources.getString(R.string.error_generic_message, result.errorMessage)) { }
                else -> showPopup(resources.getString(R.string.error_generic_message, result.errorMessage)) { }
            }
        }

        @Suppress("UNUSED_PARAMETER")
        fun onComplete(result: Result<ByteArray, FuelError>) {
            toast(R.string.toast_success)
            activity?.let { activity ->
                activity.onBackPressed()
                relevantAccount?.let { account ->
                    val currency = account.currency
                    val currentPrice = product.priceForQuoteCurrency(newOrder.tradingPair.quoteCurrency)
                    val devFee = newOrder.devFee(currentPrice)
                    if (devFee > 0.0) {
                        val prefs = Prefs(activity)
                        val unpaidFees = prefs.addUnpaidFee(devFee, currency)
                        if (unpaidFees > currency.minSendAmount) {
                            payFee(unpaidFees)
                        }
                    }
                }
            }
        }
        newOrder.submit(apiInitData, { onFailure(it) }) { result ->
            AnyApi.getAndStashOrderList(apiInitData, newOrder.tradingPair.exchange, null, { }, { })
            onComplete(result)
        }
    }

    private fun payFee(amount: Double) {
        currency.developerAddress?.let { developerAddress ->
            //TODO: make and use AnyApi call

            //TODO: only count fees as paid if they are successfully paid
            CBProApi.sendCrypto(apiInitData, amount, currency, developerAddress).executePost(
                    {  /*  fail silently   */ },
                    { _ ->
                        context?.let {
                            Prefs(it).wipeUnpaidFees(currency)
                        }
                    })
        }
    }

    private fun updateTotalText(amountIn: Double?, limitPriceIn: Double?) {
        val amount = amountIn ?: amountEditText?.text.toString().toDoubleOrZero()
        val limitPrice = limitPriceIn ?: limitEditText?.text.toString().toDoubleOrZero()
        val sideString = tradeSide.toString().capitalize()

        summaryText.text = when (tradeType) {
            TradeType.MARKET -> {
                when (amountUnitCurrency) {
                    tradingPair.baseCurrency -> resources.getString(R.string.trade_summary_market_fixed_base,
                            sideString, amount.format(tradingPair.baseCurrency))
                    tradingPair.quoteCurrency -> resources.getString(R.string.trade_summary_market_fixed_quote,
                            sideString, amount.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
                    else -> ""
                }
            }
            TradeType.LIMIT -> when (tradeSide) {
                TradeSide.BUY -> resources.getString(R.string.trade_summary_limit_buy,
                        amount.format(tradingPair.baseCurrency), limitPrice.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
                TradeSide.SELL -> resources.getString(R.string.trade_summary_limit_sell,
                        amount.format(tradingPair.baseCurrency), limitPrice.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
            }
            TradeType.STOP -> when (tradeSide) {
                TradeSide.BUY -> resources.getString(R.string.trade_summary_stop_buy,
                        amount.format(tradingPair.baseCurrency), limitPrice.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
                TradeSide.SELL -> resources.getString(R.string.trade_summary_stop_sell,
                        amount.format(tradingPair.baseCurrency), limitPrice.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
            }
        }
    }

    private fun switchTradingPair(newTradingPair: TradingPair) {
        switchTradeInfo(newTradingPair, null, null)
        updateCurrencySpinner()
    }

    private fun switchTradeSide(newTradeSide: TradeSide) {
        switchTradeInfo(null, newTradeSide, null)
    }

    private fun switchTradeType(newTradeType: TradeType) {
        switchTradeInfo(null, null, newTradeType)
    }

    private fun switchTradeInfo(newTradingPair: TradingPair?, newTradeSide: TradeSide?, newTradeType: TradeType?) {
        if  (newTradingPair != null) {
            tradingPair = newTradingPair
        }
        if (newTradeSide != null) {
            tradeSide = newTradeSide
        }
        if (newTradeType != null) {
            this.tradeType = newTradeType
        }

        updateTotalText(null, null)

        if (advancedOptionsCheckBox?.isChecked == true && tradeType == TradeType.LIMIT) {
            advancedOptionsLayout?.visibility = View.VISIBLE
        } else {
            advancedOptionsLayout?.visibility = View.GONE
        }

        //Trading pair cannot be null at this point
        val quoteCurrency = tradingPair.quoteCurrency
        when (tradeType) {
            TradeType.MARKET -> {
                val marketTab = tradeTypeTabLayout?.getTabAt(0)
                amountEditText?.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(10, 8))
                marketTab?.select()
                limitLayout?.visibility = View.INVISIBLE

                context?.let {
                    amountUnitText?.visibility = View.GONE
                    amountUnitSpinner?.visibility = View.VISIBLE
                }
            }
            TradeType.LIMIT -> {
                amountUnitText?.visibility = View.VISIBLE
                amountUnitSpinner?.visibility = View.GONE

                val limitTab = tradeTypeTabLayout?.getTabAt(1)
                amountEditText?.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(10, 8))
                limitTab?.select()
                limitUnitText?.text = quoteCurrency.toString()
                limitLayout?.visibility = View.VISIBLE
                limitLabelText?.text = resources.getString(R.string.trade_limit_label)

                advancedOptionsCheckBox?.visibility = View.VISIBLE

                val timeInForceList = TimeInForce.values().toList()
                context?.let {
                    advancedOptionTimeInForceSpinner?.adapter = TimeInForceSpinnerAdapter(it, timeInForceList)
                    advancedOptionTimeInForceSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            val selectedItem = timeInForceList[position]
                            if (selectedItem == TimeInForce.GoodTilTime) {
                                advancedOptionEndTimeSpinner?.visibility = View.VISIBLE
                            } else {
                                advancedOptionEndTimeSpinner?.visibility = View.INVISIBLE
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {
                            advancedOptionEndTimeSpinner?.visibility = View.INVISIBLE
                        }
                    }

                    //TODO: make this an enum
                    val endTimeList = TimeInForce.CancelAfter.values().toList()
                    advancedOptionEndTimeSpinner?.adapter = TifCancelAfterSpinnerAdapter(it, endTimeList)
                }
            }
            TradeType.STOP -> {
                amountUnitText?.visibility = View.VISIBLE
                amountUnitSpinner?.visibility = View.GONE
                amountEditText?.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(10, 8))
                val stopTab = tradeTypeTabLayout?.getTabAt(2)
                stopTab?.select()
                limitUnitText?.text = quoteCurrency.toString()
                limitLayout?.visibility = View.VISIBLE
                limitLabelText?.text = resources.getString(R.string.trade_stop_label)
                advancedOptionsCheckBox?.visibility = View.INVISIBLE
            }
        }

        if (context != null) {
            when (tradeSide) {
                TradeSide.BUY -> {
                    submitOrderButton?.text = resources.getString(R.string.trade_buy_order_btn)
                    tradeSideBuyRadioButton?.isChecked = true
                    when (tradeType) {
                        TradeType.MARKET, TradeType.STOP -> {
                            amountUnitText?.text = quoteCurrency.toString()
                        }
                        TradeType.LIMIT -> {
                            amountUnitText?.text = currency.toString()
                        }
                    }
                }
                TradeSide.SELL -> {
                    submitOrderButton?.text = resources.getString(R.string.trade_sell_order_btn)
                    tradeSideSellRadioButton?.isChecked = true
                    amountUnitText?.text = currency.toString()
                }
            }
        }
    }

}
