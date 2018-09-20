package com.anyexchange.anyx.classes

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
    private var specified_funds: String? = null
    private var post_only: Boolean? = null
    private var done_at: String? = null
    private var done_reason: String? = null
    var fill_fees: String? = null
    var filled_size: String? = null
    private var executed_value: String? = null
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
            this(Exchange.Binance, cbProOrder.id, TradingPair(cbProOrder.product_id), cbProOrder.price.toDouble(), cbProOrder.size.toDoubleOrZero(), cbProOrder.filled_size.toDouble(),
                    cbProOrder.type, TradeSide.forString(cbProOrder.side), cbProOrder.created_at.dateFromCBProApiDateString() ?: Date(), cbProOrder.time_in_force) {
        status = cbProOrder.status

        stp = cbProOrder.stp
        funds = cbProOrder.funds
        specified_funds = cbProOrder.specified_funds
        post_only = cbProOrder.post_only
        done_at = cbProOrder.done_at
        done_reason = cbProOrder.done_reason
        fill_fees = cbProOrder.fill_fees
        filled_size = cbProOrder.filled_size
        executed_value = cbProOrder.executed_value
        settled = cbProOrder.settled
    }
}