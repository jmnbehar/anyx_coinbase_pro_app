package com.anyexchange.anyx.classes

import android.content.Context
import com.anyexchange.anyx.R
import com.anyexchange.anyx.classes.api.*
import com.anyexchange.anyx.fragments.main.TradeFragment
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import java.util.*

class Order(val exchange: Exchange, val id: String, val tradingPair: TradingPair, val price: Double, val amount: Double, val filledAmount: Double,
            val type: TradeType, val side: TradeSide, val time: Date, val timeInForce: String) {
    var status: String? = null

    //Binance extras:
    private var cumulativeQuoteQty: Double? = null
    private var stopPrice: Double? = null
    private var icebergQty: Double? = null
    private var updateTime: Long? = null
    private var isWorking: Boolean? = null

    //CbPro extras:
    private var stp: String? = null //self trade prevention
    var funds: String? = null
    var specifiedFunds: String? = null
    private var postOnly: Boolean? = null
    private var doneAt: Date? = null
    var expireTime: Date? = null
    private var doneReason: String? = null
    var fillFees: String? = null
    var filledSize: String? = null
    private var executedValue: String? = null
    private var settled: Boolean? = null

    var showExtraInfo = false


    constructor(binanceOrder: BinanceOrder) :
            this(Exchange.Binance, binanceOrder.orderId.toString(), TradingPair(Exchange.Binance, binanceOrder.symbol), binanceOrder.price, binanceOrder.origQty, binanceOrder.executedQty,
                    TradeType.forString(binanceOrder.type), TradeSide.forString(binanceOrder.side), Date(binanceOrder.time), binanceOrder.timeInForce) {
        cumulativeQuoteQty = binanceOrder.cummulativeQuoteQty
        status = binanceOrder.status
        stopPrice = binanceOrder.stopPrice
        icebergQty = binanceOrder.icebergQty
        updateTime = binanceOrder.updateTime
        isWorking = binanceOrder.isWorking
    }
    constructor(cbProOrder: CBProOrder) :
            this(Exchange.CBPro, cbProOrder.id, TradingPair(Exchange.CBPro, cbProOrder.product_id), cbProOrder.price.toDouble(), cbProOrder.size.toDoubleOrZero(), cbProOrder.filled_size.toDouble(),
                    TradeType.forString(cbProOrder.type), TradeSide.forString(cbProOrder.side), cbProOrder.created_at.dateFromCBProApiDateString() ?: Date(), cbProOrder.time_in_force) {
        status = cbProOrder.status

        stp = cbProOrder.stp
        funds = cbProOrder.funds
        specifiedFunds = cbProOrder.specified_funds
        postOnly = cbProOrder.post_only
        doneAt = cbProOrder.done_at?.dateFromCBProApiDateString()
        expireTime = cbProOrder.expire_time?.dateFromCBProApiDateString()
        doneReason = cbProOrder.done_reason
        fillFees = cbProOrder.fill_fees
        filledSize = cbProOrder.filled_size
        executedValue = cbProOrder.executed_value
        settled = cbProOrder.settled
    }

//    fun summary(context: Context) : String {
//            return when (type) {
//                TradeType.MARKET -> {
//                    val amountCurrency = amountUnitSpinner?.selectedItem as? Currency
//                    when (amountCurrency) {
//                        tradingPair.baseCurrency -> resources.getString(R.string.trade_summary_market_fixed_base,
//                                sideString, amount.format(tradingPair.baseCurrency))
//                        tradingPair.quoteCurrency -> resources.getString(R.string.trade_summary_market_fixed_quote,
//                                sideString, amount.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
//                        else -> ""
//                    }
//                }
//                TradeType.LIMIT -> when (TradeFragment.tradeSide) {
//                    TradeSide.BUY -> resources.getString(R.string.trade_summary_limit_buy,
//                            amount.format(tradingPair.baseCurrency), limitPrice.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
//                    TradeSide.SELL -> resources.getString(R.string.trade_summary_limit_sell,
//                            amount.format(tradingPair.baseCurrency), limitPrice.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
//                }
//                TradeType.STOP -> when (TradeFragment.tradeSide) {
//                    TradeSide.BUY -> resources.getString(R.string.trade_summary_stop_buy,
//                            amount.format(tradingPair.baseCurrency), limitPrice.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
//                    TradeSide.SELL -> resources.getString(R.string.trade_summary_stop_sell,
//                            amount.format(tradingPair.baseCurrency), limitPrice.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
//                }
//            }
//        }

    companion object {

//        fun getAndStashList(apiInitData: ApiInitData?, exchange: Exchange, currency: Currency?, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (List<Order>) -> Unit) {
//            AnyApi.getAndStashOrderList(apiInitData, exchange, currency, onFailure, onSuccess)
//        }
        fun getAndStashList(apiInitData: ApiInitData?, currency: Currency, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (List<Order>) -> Unit) {
            var exchangesChecked = 0
            val fullOrderList = mutableListOf<Order>()

            //TODO: use Exchange.values()
            val exchangeList = listOf(Exchange.CBPro)
            for (exchange in exchangeList) {
                AnyApi.getAndStashOrderList(apiInitData, exchange, currency, onFailure) {
                    exchangesChecked++
                    fullOrderList.addAll(it)
                    if (exchangesChecked == exchangeList.size) {
                        onSuccess(fullOrderList)
                    }
                }
            }
        }
    }

    fun cancel(apiInitData: ApiInitData?, onFailure: (Result.Failure<String, FuelError>) -> Unit, onSuccess: (Result.Success<String, FuelError>) -> Unit) {
        AnyApi.cancelOrder(apiInitData, this, onFailure, onSuccess)
    }
}