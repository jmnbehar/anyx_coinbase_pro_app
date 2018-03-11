package com.jmnbehar.anyx.Classes

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import java.util.*

/**
 * Created by jmnbehar on 12/20/2017.
 */

class Product(var currency: Currency, var id: String, candles: List<Candle>) {
    constructor(apiProduct: ApiProduct, candles: List<Candle>)
            : this(Currency.fromString(apiProduct.base_currency), apiProduct.id, candles)

    var price = candles.lastOrNull()?.close ?: 0.0

    private var hourCandles = candles
    var dayCandles = candles
    private var weekCandles = candles
    private var monthCandles = candles
    private var yearCandles = candles
    private var allTimeCandles = candles
    private var candlesTimespan = Timespan.DAY

    fun candlesForTimespan(timespan: Timespan): List<Candle> {
        return when (timespan) {
            Timespan.HOUR -> hourCandles
            Timespan.DAY -> dayCandles
            Timespan.WEEK -> weekCandles
            Timespan.MONTH -> monthCandles
            Timespan.YEAR -> yearCandles
            Timespan.ALL -> allTimeCandles
        }
    }

    override fun toString(): String {
        var alertString = currency.toString() + '\n'
        alertString += id + '\n'
//        alertString += price.toString() + '\n'
        return alertString
    }

    //TODO: move this code to product?
    fun updateCandles(timespan: Timespan, onFailure: (Result.Failure<String, FuelError>) -> Unit, onComplete: (didUpdate: Boolean) -> Unit) {
        val now = Calendar.getInstance()
        val longAgo = Calendar.getInstance()
        longAgo.add(Calendar.YEAR, -2)
        val longAgoInSeconds = longAgo.timeInSeconds()
        val nowInSeconds = now.timeInSeconds()

        var candles = candlesForTimespan(timespan).toMutableList()

        val lastCandleTime = candles.lastOrNull()?.time?.toLong() ?: longAgoInSeconds
        var nextCandleTime: Long = lastCandleTime + Candle.granularityForTimespan(timespan)

//        if (timespan != product.candlesTimespan) {
//            nextCandleTime = longAgoInSeconds
//        }
        candlesTimespan = timespan

        if (nextCandleTime < nowInSeconds) {
            var missingTime = nowInSeconds - lastCandleTime

            val timespanLong = timespan.value(currency)
            if (missingTime > timespanLong) {
                missingTime = timespanLong
            }

            val granularity = Candle.granularityForTimespan(timespan)
            GdaxApi.candles(id, missingTime, granularity, 0).getCandles(onFailure, { candleList ->
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
                            Timespan.ALL -> allTimeCandles = candles
                        }
                        price = candles.last().close
                    }
                }
                onComplete(didGetNewCandle)
            })
        } else {
            onComplete(false)
        }
    }
    companion object {
        fun fromString(string: String): Product {
            val splitString = string.split('\n')
            val currency = Currency.fromString(splitString[0])
            val id = splitString[1]
//            val price = splitString[1].toDoubleOrZero()
            return Product(currency, id, listOf())
        }

        fun fiatProduct(currency: String) = Product(Currency.fromString(currency), currency,
                listOf(Candle(0.0, 1.0, 1.0, 1.0, 1.0, 0.0)))

    }

}
