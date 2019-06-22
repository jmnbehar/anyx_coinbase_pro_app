package com.anyexchange.anyx.classes

import android.content.res.Resources
import com.anyexchange.anyx.R
import com.anyexchange.anyx.api.*
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import java.util.*

class Order(val exchange: Exchange, val id: String, val tradingPair: TradingPair, val price: Double?, val amount: Double, val filledAmount: Double,
            var type: TradeType, val side: TradeSide, val time: Date, val timeInForce: String?, private val stopPrice: Double?) {
    var status: String? = null

    //Binance extras:
    private var cumulativeQuoteQty: Double? = null
    private var icebergQty: Double? = null
    private var updateTime: Long? = null
    private var isWorking: Boolean? = null

    //CbPro extras:
    private var stp: String? = null //self trade prevention
    private var funds: Double? = null
    private var specifiedFunds: Double? = null
    private var postOnly: Boolean? = null
    private var doneAt: Date? = null
    var expireTime: Date? = null
    private var doneReason: String? = null
    private var fillFees: String? = null
    private var filledSize: String? = null
    private var executedValue: String? = null
    private var settled: Boolean? = null
    private var stopType: String? = null

    var showExtraInfo = false

    fun summary(resources: Resources) : String {
            return when (type) {
                TradeType.MARKET -> specifiedFunds?.let {
                    resources.getString(R.string.order_summary_market_fixed_quote,
                            side.toString().capitalize(), it.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
                } ?: run {
                    resources.getString(R.string.order_summary_market_fixed_base,
                            side.toString().capitalize(), amount.format(tradingPair.baseCurrency))
                }
                TradeType.LIMIT -> when (side) {
                    TradeSide.BUY -> resources.getString(R.string.order_summary_limit_buy,
                            amount.format(tradingPair.baseCurrency), price?.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
                    TradeSide.SELL -> resources.getString(R.string.order_summary_limit_sell,
                            amount.format(tradingPair.baseCurrency), price?.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
                }
                TradeType.STOP -> when (side) {
                    TradeSide.BUY -> {
                        //TODO: find out why specifiedFunds AND funds are null
                        val formattedFunds = (specifiedFunds ?: funds ?: 0.0).format(tradingPair.baseCurrency)
                        resources.getString(R.string.order_summary_stop_buy,
                                formattedFunds, stopPrice?.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
                    }
                    TradeSide.SELL -> resources.getString(R.string.order_summary_stop_sell,
                            amount.format(tradingPair.baseCurrency), stopPrice?.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
                }
            }
        }

    companion object {

        fun fromBinanceOrder(binanceOrder: BinanceOrder) : Order? {
            val tradingPair = TradingPair.tradingPairFromId(Exchange.Binance, binanceOrder.symbol)
            return if (tradingPair != null) {
                val type = TradeType.forString(binanceOrder.type)
                val side = TradeSide.forString(binanceOrder.side)
                val id = binanceOrder.orderId.toString()
                val tempOrder = Order(Exchange.Binance, id, tradingPair, binanceOrder.price, binanceOrder.origQty, binanceOrder.executedQty,
                        type, side, Date(binanceOrder.time), binanceOrder.timeInForce, binanceOrder.stopPrice)
                tempOrder.cumulativeQuoteQty = binanceOrder.cummulativeQuoteQty
                tempOrder.status = binanceOrder.status
                tempOrder.icebergQty = binanceOrder.icebergQty
                tempOrder.updateTime = binanceOrder.updateTime
                tempOrder.isWorking = binanceOrder.isWorking

                tempOrder
            } else {
                null
            }
        }


        fun fromCbProOrder(cbProOrder: CBProOrder) : Order? {
            val tradingPair = TradingPair.tradingPairFromId(Exchange.CBPro, cbProOrder.product_id)

            return if (tradingPair != null) {
                val price = cbProOrder.price?.toDoubleOrNull()
                val size = cbProOrder.size.toDoubleOrZero()
                val filledSize = cbProOrder.filled_size.toDoubleOrZero()
                val type = if (cbProOrder.stop_price != null) {
                    TradeType.STOP
                } else {
                    TradeType.forString(cbProOrder.type)
                }
                val side = TradeSide.forString(cbProOrder.side)
                val date = cbProOrder.created_at.dateFromCBProApiDateString() ?: Date()
                val tempOrder = Order(Exchange.CBPro, cbProOrder.id, tradingPair, price, size, filledSize, type,
                        side, date, cbProOrder.time_in_force, cbProOrder.stop_price?.toDoubleOrNull())

                tempOrder.status = cbProOrder.status

                tempOrder.stp = cbProOrder.stp
                tempOrder.funds = cbProOrder.funds?.toDoubleOrNull()
                tempOrder.specifiedFunds = cbProOrder.specified_funds?.toDoubleOrNull()
                tempOrder.postOnly = cbProOrder.post_only
                tempOrder.doneAt = cbProOrder.done_at?.dateFromCBProApiDateString()
                tempOrder.expireTime = cbProOrder.expire_time?.dateFromCBProApiDateString()
                tempOrder.doneReason = cbProOrder.done_reason
                tempOrder.fillFees = cbProOrder.fill_fees
                tempOrder.filledSize = cbProOrder.filled_size
                tempOrder.executedValue = cbProOrder.executed_value
                tempOrder.settled = cbProOrder.settled
                tempOrder.stopType = cbProOrder.stop

                tempOrder
            } else {
                null
            }
            }

//        fun getAndStashList(apiInitData: ApiInitData?, exchange: Exchange, currency: Currency?, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (List<Order>) -> Unit) {
//            AnyApi.getAndStashOrderList(apiInitData, exchange, currency, onFailure, onSuccess)
//        }
        fun getAndStashList(apiInitData: ApiInitData?, currency: Currency, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (List<Order>) -> Unit) {
            var exchangesChecked = 0
            val fullOrderList = mutableListOf<Order>()

            //TODO: use Exchange.values()
            val exchangeList = listOf(Exchange.CBPro)
            for (exchange in exchangeList) {
                AnyApi(apiInitData).getAndStashOrderList(exchange, currency, onFailure) {
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
        AnyApi(apiInitData).cancelOrder(this, onFailure, onSuccess)
    }
}