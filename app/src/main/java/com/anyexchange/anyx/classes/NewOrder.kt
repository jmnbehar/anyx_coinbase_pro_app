package com.anyexchange.anyx.classes

import android.content.Context
import com.anyexchange.anyx.R
import com.anyexchange.anyx.classes.api.AnyApi
import com.anyexchange.anyx.classes.api.ApiInitData
import com.anyexchange.anyx.fragments.main.TradeFragment
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result

class NewOrder(val tradingPair: TradingPair, val priceLimit: Double?, val amount: Double?,
               val funds: Double?, val type: TradeType, val side: TradeSide,
               private val timeInForce: TimeInForce?, private val cancelAfter: TimeInForce.CancelAfter?, private val iceBergQty: String?) {

    private fun willIncurFee(currentPrice: Double): Boolean {
        //TODO: use new version of willIncurFee with new stop logic
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
                        funds!! / priceLimit
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
            TradeType.STOP -> funds ?: amount!! * priceLimit!!
        }
    }

    fun summaryText(context: Context) : String {
        return when (type) {
            TradeType.MARKET -> when (amount) {
                null -> context.getString(R.string.trade_summary_market_fixed_quote, side.name, funds!!.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
                else -> context.getString(R.string.trade_summary_market_fixed_base, side.name, amount.format(tradingPair.baseCurrency))
            }
            TradeType.LIMIT -> when (side) {
                TradeSide.BUY -> context.getString(R.string.trade_summary_limit_buy, amount!!.format(tradingPair.baseCurrency),
                        priceLimit!!.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
                TradeSide.SELL -> context.getString(R.string.trade_summary_limit_sell, amount!!.format(tradingPair.baseCurrency),
                        priceLimit!!.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
            }
            TradeType.STOP -> when (side) {
                TradeSide.BUY -> context.getString(R.string.trade_summary_stop_buy, funds!!.format(tradingPair.quoteCurrency), tradingPair.baseCurrency,
                        priceLimit!!.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
                TradeSide.SELL -> context.getString(R.string.trade_summary_stop_sell, amount!!.format(tradingPair.baseCurrency),
                        priceLimit!!.format(tradingPair.quoteCurrency), tradingPair.baseCurrency)
            }
        }
    }

    fun submit(apiInitData: ApiInitData?, onFailure: (result: Result.Failure<ByteArray, FuelError>) -> Unit, onSuccess: (Result<ByteArray, FuelError>) -> Unit) {
        val anyApi = AnyApi(apiInitData)
        when(type) {
            TradeType.MARKET -> anyApi.orderMarket(tradingPair.exchange, side, tradingPair, amount, funds,
                    { onFailure(it) }, { onSuccess(it) })
            TradeType.LIMIT -> {
                priceLimit?.let { limitPrice ->
                    anyApi.orderLimit(tradingPair.exchange, side, tradingPair, limitPrice, amount!!, timeInForce, cancelAfter, null,
                            { onFailure(it) }, { onSuccess(it) })
                } ?: run {
                    //TODO: call onFailure
                }
            }
            TradeType.STOP -> {
                val fundsOrAmount = amount ?: funds
                priceLimit?.let { stopPrice ->
                    anyApi.orderStop(tradingPair.exchange, TradeFragment.tradeSide, tradingPair, stopPrice, fundsOrAmount!!, null,
                            { onFailure(it) }, { onSuccess(it) })
                } ?: run {
                    //TODO: call onFailure
                }
            }
        }
    }
}