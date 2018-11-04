package com.anyexchange.anyx.classes

import com.anyexchange.anyx.classes.api.AnyApi
import com.anyexchange.anyx.classes.api.ApiInitData
import com.anyexchange.anyx.fragments.main.TradeFragment
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result

class NewOrder(val tradingPair: TradingPair, val priceLimit: Double?, val amount: Double?,
               val funds: Double?, val type: TradeType, val side: TradeSide,
               private val timeInForce: TimeInForce?, private val cancelAfter: TimeInForce.CancelAfter?, private val iceBergQty: String?) {

    private fun willIncurFee(currentPrice: Double): Boolean {
        if (priceLimit != null) {
            if (priceLimit <= currentPrice &&
                    ((type == TradeType.LIMIT && side == TradeSide.BUY) ||
                            (type == TradeType.STOP  && side == TradeSide.SELL))) {
                return false
            } else if (priceLimit >= currentPrice &&
                    ((type == TradeType.STOP  && side == TradeSide.BUY) ||
                            (type == TradeType.LIMIT && side == TradeSide.SELL))) {
                return false
            }
        }
        return true
    }

    fun devFee(currentPrice: Double) : Double {
        // Dev fee is always in base currency
        return if (willIncurFee(currentPrice)) {
            totalBase(currentPrice) * DEV_FEE_PERCENTAGE
        } else {
            0.0
        }
    }

    fun exchangeFee(currentPrice: Double) : Pair<Double, Currency> {
        return if (willIncurFee(currentPrice)) {
            //TODO: decide on whether this is in
            val fee = totalQuote(currentPrice) * tradingPair.baseCurrency.feePercentage
            Pair(fee, tradingPair.quoteCurrency)
        } else {
            Pair(0.0, tradingPair.baseCurrency)
        }
    }

    fun totalBase(currentPrice: Double) : Double {
        return when (side) {
            TradeSide.BUY -> when (type) {
                TradeType.MARKET -> if (funds != null) { //fixed quote
                    funds / currentPrice
                } else { //fixed base
                    amount!!
                }
                TradeType.LIMIT -> amount!!
                TradeType.STOP ->
                    if (priceLimit!! > 0) {
                        amount!! / priceLimit
                    } else {
                        0.00
                    }
            }
            TradeSide.SELL -> if (type == TradeType.MARKET && funds != null) {
                funds / currentPrice
            } else {
                amount!!
            }
        }
    }

    fun totalQuote(currentPrice: Double) : Double {
        return when (type) {
            TradeType.MARKET -> funds ?: (amount!! * currentPrice)
            TradeType.LIMIT -> amount!! * priceLimit!!
            TradeType.STOP -> amount!! * priceLimit!!
        }
    }

    fun submit(apiInitData: ApiInitData?, onFailure: (result: Result.Failure<ByteArray, FuelError>) -> Unit, onSuccess: (Result<ByteArray, FuelError>) -> Unit) {
        when(type) {
            TradeType.MARKET -> AnyApi.orderMarket(apiInitData, tradingPair.exchange, side, tradingPair, amount, funds,
                    { onFailure(it) }, { onSuccess(it) })
            TradeType.LIMIT -> {
                priceLimit?.let { limitPrice ->
                    AnyApi.orderLimit(apiInitData, tradingPair.exchange, side, tradingPair, limitPrice, amount!!, timeInForce, cancelAfter, null,
                            { onFailure(it) }, { onSuccess(it) })
                } ?: run {
                    //TODO: call onFailure
                }
            }
            TradeType.STOP -> {
                priceLimit?.let { stopPrice ->
                    AnyApi.orderStop(apiInitData, tradingPair.exchange, TradeFragment.tradeSide, tradingPair, stopPrice, amount!!, null,
                            { onFailure(it) }, { onSuccess(it) })
                } ?: run {
                    //TODO: call onFailure
                }
            }
        }
    }
}