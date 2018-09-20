package com.anyexchange.anyx.classes

import java.util.*

class Fill(val exchange: Exchange, val tradingPair: TradingPair, val id: String, val orderId: String, val price: Double, val amount: Double, val time: Date, val side: TradeSide, val fee: Double) {
    var isMaker: Boolean? = null

    //binance extra info:
    var commissionAsset: Currency? = null
    var isBestMatch: Boolean? = null

    //cbpro extra info:
    var isSettled: Boolean? = null
    var liquidity: String? = null


    constructor(binanceFill: BinanceAccountFill) :
            this(Exchange.Binance, TradingPair(binanceFill.symbol), binanceFill.id, binanceFill.orderId.toString(), binanceFill.price, binanceFill.qty,
                    Date(binanceFill.time), if (binanceFill.isBuyer) { TradeSide.BUY } else { TradeSide.SELL }, binanceFill.commission) {
        commissionAsset = Currency.forString(binanceFill.commissionAsset)
        isBestMatch = binanceFill.isBestMatch

        isMaker = binanceFill.isMaker
    }
    constructor(cbProFill: CBProFill) :
            //TODO: check back in on time
            this(Exchange.CBPro, TradingPair(cbProFill.product_id), cbProFill.trade_id.toString(), cbProFill.order_id, cbProFill.price.toDouble(), cbProFill.size.toDouble(),
                    cbProFill.created_at.dateFromCBProApiDateString() ?: Date(), TradeSide.forString(cbProFill.side), cbProFill.fee.toDoubleOrZero()) {
        isSettled = cbProFill.settled
        liquidity = cbProFill.liquidity

        isMaker = (fee == 0.0)
    }


}