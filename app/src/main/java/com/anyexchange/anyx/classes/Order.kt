package com.anyexchange.anyx.classes

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.util.*

class Order(val exchange: Exchange, val id: String, val tradingPair: TradingPair, val price: Double, val amount: Double, val filledAmount: Double,
            val type: String, val side: TradeSide, val time: Date, val timeInForce: String) {
    var status: String? = null

    //Binance extras:
    private var cumulativeQuoteQty: Double? = null
    private var stopPrice: Double? = null
    private var icebergQty: Double? = null
    private var updateTime: Long? = null
    private var isWorking: Boolean? = null

    //CbPro extras:
    private var stp: String? = null
    private var funds: String? = null
    private var specifiedFunds: String? = null
    private var postOnly: Boolean? = null
    private var doneAt: String? = null
    private var doneReason: String? = null
    var fillFees: String? = null
    var filledSize: String? = null
    private var executedValue: String? = null
    private var settled: Boolean? = null

    constructor(binanceOrder: BinanceOrder) :
            this(Exchange.Binance, binanceOrder.orderId.toString(), TradingPair(binanceOrder.symbol), binanceOrder.price, binanceOrder.origQty, binanceOrder.executedQty,
                    binanceOrder.type, TradeSide.forString(binanceOrder.side), Date(binanceOrder.time), binanceOrder.timeInForce) {
        cumulativeQuoteQty = binanceOrder.cummulativeQuoteQty
        status = binanceOrder.status
        stopPrice = binanceOrder.stopPrice
        icebergQty = binanceOrder.icebergQty
        updateTime = binanceOrder.updateTime
        isWorking = binanceOrder.isWorking
    }
    constructor(cbProOrder: CBProOrder) :
            this(Exchange.CBPro, cbProOrder.id, TradingPair(cbProOrder.product_id), cbProOrder.price.toDouble(), cbProOrder.size.toDoubleOrZero(), cbProOrder.filled_size.toDouble(),
                    cbProOrder.type, TradeSide.forString(cbProOrder.side), cbProOrder.created_at.dateFromCBProApiDateString() ?: Date(), cbProOrder.time_in_force) {
        status = cbProOrder.status

        stp = cbProOrder.stp
        funds = cbProOrder.funds
        specifiedFunds = cbProOrder.specified_funds
        postOnly = cbProOrder.post_only
        doneAt = cbProOrder.done_at
        doneReason = cbProOrder.done_reason
        fillFees = cbProOrder.fill_fees
        filledSize = cbProOrder.filled_size
        executedValue = cbProOrder.executed_value
        settled = cbProOrder.settled
    }


    fun getList(exchange: Exchange, tradingPair: TradingPair, apiInitData: ApiInitData, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (List<Order>) -> Unit) {
        when (exchange) {
            Exchange.CBPro -> {
                CBProApi.listOrders(apiInitData, tradingPair).getAndStash(onFailure, onComplete)
            }
            Exchange.Binance -> {
                BinanceApi.listOrders(apiInitData, tradingPair).getAndStash(onFailure, onComplete)
            }
        }
    }
}