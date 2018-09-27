package com.anyexchange.anyx.classes

import com.anyexchange.anyx.classes.api.AnyApi
import com.anyexchange.anyx.classes.api.ApiInitData
import com.anyexchange.anyx.classes.api.BinanceAccountFill
import com.anyexchange.anyx.classes.api.CBProFill
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
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
            this(Exchange.Binance, TradingPair(Exchange.Binance, binanceFill.symbol), binanceFill.id, binanceFill.orderId.toString(), binanceFill.price, binanceFill.qty,
                    Date(binanceFill.time), if (binanceFill.isBuyer) { TradeSide.BUY } else { TradeSide.SELL }, binanceFill.commission) {
        commissionAsset = Currency(binanceFill.commissionAsset)
        isBestMatch = binanceFill.isBestMatch

        isMaker = binanceFill.isMaker
    }
    constructor(cbProFill: CBProFill) :
            //TODO: check back in on time
            this(Exchange.CBPro, TradingPair(Exchange.CBPro, cbProFill.product_id), cbProFill.trade_id.toString(), cbProFill.order_id, cbProFill.price.toDouble(), cbProFill.size.toDouble(),
                    cbProFill.created_at.dateFromCBProApiDateString() ?: Date(), TradeSide.forString(cbProFill.side), cbProFill.fee.toDoubleOrZero()) {
        isSettled = cbProFill.settled
        liquidity = cbProFill.liquidity

        isMaker = (fee == 0.0)
    }


    companion object {
        fun getAndStashList(apiInitData: ApiInitData?, exchange: Exchange, tradingPair: TradingPair?, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (List<Fill>) -> Unit) {
            AnyApi.getAndStashFillList(apiInitData, exchange, tradingPair, onFailure, onSuccess)
        }
    }
}