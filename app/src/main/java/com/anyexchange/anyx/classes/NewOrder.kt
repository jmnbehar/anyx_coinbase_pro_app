package com.anyexchange.anyx.classes

import com.anyexchange.anyx.fragments.main.TradeFragment

class NewOrder(val tradingPair: TradingPair, val priceLimit: Double?, val amount: Double?,
               val funds: Double?, val type: TradeType, val side: TradeSide,
               val timeInForce: TimeInForce?, val cancelAfter: TimeInForce.CancelAfter?, val iceBergQty: String?) {

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

    fun devFee(currentPrice: Double) : Pair<Double, Currency> {
        return if (willIncurFee(currentPrice)) {
            Pair(baseTotal * DEV_FEE_PERCENTAGE, tradingPair.baseCurrency)
        } else {
            Pair(0.0, tradingPair.baseCurrency)
        }
    }

    fun exchangeFee(currentPrice: Double) : Pair<Double, Currency> {
        return if (willIncurFee(currentPrice)) {
            //TODO: decide on whether this is in
            if (amount != null) {
                val fee = amount * tradingPair.baseCurrency.feePercentage
                Pair(fee, tradingPair.baseCurrency)
            } else {
                val fee = funds!! * tradingPair.baseCurrency.feePercentage
                Pair(fee, tradingPair.quoteCurrency)
            }
        } else {
            Pair(0.0, tradingPair.quoteCurrency)
        }
    }

    fun totalFees(currentPrice: Double) : Pair<Double, Currency> {
        if (willIncurFee(currentPrice)) {
            //TODO: change this to check exchange feePercentage
            val exchangeFee = amount * tradingPair.baseCurrency.feePercentage
            val devFee  = amount * DEV_FEE_PERCENTAGE
            return (devFee + exchangeFee)
        } else {
            return 0.0
        }
    }



    val baseTotal: Double
        get() {
            return 0.0
        }


    private fun totalBase(currentPrice: Double) : Double {
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

    private fun totalQuote(currentPrice: Double) : Double {
            return when (side) {
                TradeSide.BUY -> when (type) {
                    TradeType.MARKET -> amount ?: (funds!! / currentPrice)
                    TradeType.LIMIT -> amount!! * priceLimit!!
                    TradeType.STOP -> amount!!
                }
                TradeSide.SELL -> when (type) {
                    TradeType.MARKET -> funds ?: (amount!! * currentPrice)
                    TradeType.LIMIT -> amount!! * priceLimit!!
                    TradeType.STOP -> amount!! * priceLimit!!
                }
            }
        }


    fun pricePlusFeeTotal(currentPrice: Double, totalFees: Double) : Double {
        //TODO: include dev fee here
        return when (TradeFragment.tradeSide) {
            TradeSide.BUY ->  totalQuote(currentPrice) + totalFees
            TradeSide.SELL -> totalQuote(currentPrice) - totalFees
        }
    }
}