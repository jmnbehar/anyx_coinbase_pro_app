package com.anyexchange.anyx.classes

import com.anyexchange.anyx.classes.api.AnyApi
import com.anyexchange.anyx.classes.api.ApiInitData
import com.anyexchange.anyx.classes.api.BinanceAccountFill
import com.anyexchange.anyx.classes.api.CBProFill
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import java.util.*

open class Fill(val exchange: Exchange, val tradingPair: TradingPair, val id: String, val orderId: String, val price: Double, open val amount: Double, val time: Date, val side: TradeSide, open val fee: Double) {
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
//        fun getAndStashList(apiInitData: ApiInitData?, exchange: Exchange, tradingPair: TradingPair?, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (List<Fill>) -> Unit) {
//            AnyApi.getAndStashFillList(apiInitData, exchange, tradingPair, onFailure, onSuccess)
//        }
        fun getAndStashList(apiInitData: ApiInitData?, currency: Currency, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (List<Fill>) -> Unit) {
            val tradingPairs = Product.map[currency.id]?.tradingPairs ?: listOf()
            var tradingPairsChecked = 0
            val fullFillList = mutableListOf<Fill>()
            for (tradingPair in tradingPairs) {
                AnyApi.getAndStashFillList(apiInitData, tradingPair.exchange, tradingPair, onFailure) {
                    tradingPairsChecked++
                    val combinedList = it.combineFills()
                    fullFillList.addAll(combinedList)
                    if (tradingPairsChecked == tradingPairs.size) {
                        onSuccess(fullFillList)
                    }
                }
            }
        }
    }


}

class CombinedFill(exchange: Exchange, tradingPair: TradingPair, orderId: String, price: Double, side: TradeSide,
                   override var amount: Double, override var fee: Double, val timeList: MutableList<Date> = mutableListOf(),
                   val idList: MutableList<String> = mutableListOf()) : Fill(exchange, tradingPair, "", orderId, price, amount, Date(), side, fee)


fun List<Fill>.combineFills() : List<CombinedFill> {
    val mutableCopy = this.toMutableList()
    val combinedList = mutableListOf<CombinedFill>()
    while (mutableCopy.isNotEmpty()) {
        val fill = mutableCopy.first()
        val similarFills = mutableCopy.filter { it.orderId == fill.orderId && it.price == fill.price }
        val combinedFill = CombinedFill(fill.exchange, fill.tradingPair, fill.orderId, fill.price, fill.side, 0.0, 0.0)
        for (similarFill in similarFills) {
            combinedFill.amount += similarFill.amount
            combinedFill.fee += similarFill.fee
            combinedFill.timeList.add(similarFill.time)
            combinedFill.idList.add(similarFill.id)
        }
        combinedList.add(combinedFill)
        mutableCopy.removeAll(similarFills)
    }
    return combinedList
}