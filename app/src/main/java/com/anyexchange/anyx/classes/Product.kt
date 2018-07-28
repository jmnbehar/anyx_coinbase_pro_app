package com.anyexchange.anyx.classes

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import org.json.JSONObject
import java.util.*
import com.google.gson.reflect.TypeToken


/**
 * Created by anyexchange on 12/20/2017.
 */

class Product(var currency: Currency, var id: String, val quoteCurrency: Currency?, val tradingPairs: List<String>) {
    constructor(apiProduct: ApiProduct, tradingPairs: List<String>)
            : this(Currency.forString(apiProduct.base_currency) ?: Currency.USD, apiProduct.id,
            Currency.forString(apiProduct.quote_currency) ?: Currency.USD, tradingPairs)

    var price = 0.0

    private var hourCandles = listOf<Candle>()
    var dayCandles = listOf<Candle>()
    private var weekCandles = listOf<Candle>()
    private var monthCandles = listOf<Candle>()
    private var yearCandles = listOf<Candle>()
//    private var allTimeCandles = listOf<Candle>()

    private var candlesTimespan = Timespan.DAY

    fun percentChange(timespan: Timespan) : Double {
        val currentPrice = price
        val candles = candlesForTimespan(timespan)
        val open = if (candles.isNotEmpty()) {
            candles.first().close
        } else {
            0.0
        }
        val change = currentPrice - open

        val weightedChange: Double = (change / open)
        return weightedChange * 100.0
    }

    fun candlesForTimespan(timespan: Timespan): List<Candle> {
        return when (timespan) {
            Timespan.HOUR -> hourCandles
            Timespan.DAY -> dayCandles
            Timespan.WEEK -> weekCandles
            Timespan.MONTH -> monthCandles
            Timespan.YEAR -> yearCandles
//            Timespan.ALL -> allTimeCandles
        }
    }

    fun updateCandles(timespan: Timespan, onFailure: (Result.Failure<String, FuelError>) -> Unit, onComplete: (didUpdate: Boolean) -> Unit) {
        val now = Calendar.getInstance()
        val longAgo = Calendar.getInstance()
        longAgo.add(Calendar.YEAR, -2)
        val longAgoInSeconds = longAgo.timeInSeconds()
        val nowInSeconds = now.timeInSeconds()

        var candles = candlesForTimespan(timespan).toMutableList()

        val lastCandleTime = candles.lastOrNull()?.time?.toLong() ?: longAgoInSeconds
        val nextCandleTime: Long = lastCandleTime + Candle.granularityForTimespan(timespan)

//        if (timespan != product.candlesTimespan) {
//            nextCandleTime = longAgoInSeconds
//        }
        candlesTimespan = timespan

        if (nextCandleTime < nowInSeconds) {
            var missingTime = nowInSeconds - lastCandleTime

            val timespanLong = timespan.value()
            if (missingTime > timespanLong) {
                missingTime = timespanLong
            }

            val granularity = Candle.granularityForTimespan(timespan)
            CBProApi.candles(id, missingTime, granularity, 0).getCandles(onFailure) { candleList ->
                var didGetNewCandle = false
                if (candleList.isNotEmpty()) {
                    val newLastCandleTime = candleList.lastOrNull()?.time?.toInt() ?: 0.0
                    didGetNewCandle = (lastCandleTime != newLastCandleTime)
                    if (didGetNewCandle) {
                        val timespanStart = nowInSeconds - timespanLong

                        if (candles.isNotEmpty()) {
                            val firstInTimespan = candles.indexOfFirst { candle -> candle.time >= timespanStart }
                            candles = if (firstInTimespan >= 0) {
                                candles.subList(firstInTimespan, candles.lastIndex).toMutableList()
                            } else {
                                mutableListOf()
                            }
                            candles.addAll(candleList)
                        } else {
                            candles = candleList.toMutableList()
                        }

                        when (timespan) {
                            Timespan.HOUR -> hourCandles = candles
                            Timespan.DAY ->  dayCandles = candles
                            Timespan.WEEK -> weekCandles = candles
                            Timespan.MONTH -> monthCandles = candles
                            Timespan.YEAR -> yearCandles = candles
//                            Timespan.ALL -> allTimeCandles = candles
                        }
                        price = candles.last().close
                    }
                }
                onComplete(didGetNewCandle)
            }
        } else {
            onComplete(false)
        }
    }

    override fun toString(): String {
        var alertString = currency.toString() + '\n'
        alertString += id + '\n'
        alertString += quoteCurrency.toString() + '\n'
        for (tradingPair in tradingPairs) {
            alertString += tradingPair + '\n'
        }
        return alertString
    }

    companion object {
        fun fromString(string: String): Product {
            val splitString = string.split('\n')
            val currency = Currency.forString(splitString[0]) ?: Currency.USD
            val id = splitString[1]
            val quoteCurrency = if (splitString.size > 2) { Currency.forString(splitString[2]) ?: Currency.USD } else { Currency.USD }
            val tradingPairs = if (splitString.size > 3) { splitString.subList(3, splitString.size - 1) } else { listOf() }
            return Product(currency, id, quoteCurrency, tradingPairs)
        }

        fun fiatProduct(currency: Currency) = Product(currency, currency.productId, null, listOf())
    }

}
