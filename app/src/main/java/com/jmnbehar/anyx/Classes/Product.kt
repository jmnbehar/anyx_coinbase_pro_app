package com.jmnbehar.anyx.Classes

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import java.sql.Time
import java.util.*

/**
 * Created by jmnbehar on 12/20/2017.
 */


class Product(var currency: Currency, var id: String, candles: List<Candle>) {
    constructor(apiProduct: ApiProduct, candles: List<Candle>)
            : this(Currency.fromString(apiProduct.base_currency), apiProduct.id, candles)

    var price = candles.lastOrNull()?.close ?: 0.0

    var hourCandles = candles
    var dayCandles = candles
    var weekCandles = candles
    var monthCandles = candles
    //var yearCandles = candles
    var allTimeCandles = candles
    var candlesTimespan = TimeInSeconds.oneDay

    fun candlesForTimespan(timespan: Long): List<Candle> {
        return when (timespan) {
            TimeInSeconds.oneHour -> hourCandles
            TimeInSeconds.oneWeek -> weekCandles
            TimeInSeconds.oneMonth -> monthCandles
            TimeInSeconds.oneYear -> {
                val now = Calendar.getInstance()
                val nowInSeconds = now.timeInSeconds()
                val timespanStart = nowInSeconds - timespan
                val firstInTimespan = allTimeCandles.indexOfFirst { candle -> candle.time >= timespanStart }
                allTimeCandles.subList(firstInTimespan, allTimeCandles.lastIndex).toMutableList()
            }
            currency.lifetimeInSeconds -> allTimeCandles
            TimeInSeconds.oneDay -> dayCandles
            else -> dayCandles
        }
    }

    override fun toString(): String {
        var alertString = currency.toString() + '\n'
        alertString += id + '\n'
//        alertString += price.toString() + '\n'
        return alertString
    }

    //TODO: move this code to product?
    fun updateCandles(timespan: Long, onFailure: (Result.Failure<String, FuelError>) -> Unit, onComplete: (didUpdate: Boolean) -> Unit) {
        val now = Calendar.getInstance()
        val longAgo = Calendar.getInstance()
        longAgo.add(Calendar.YEAR, -2)
        val longAgoInSeconds = longAgo.timeInSeconds()
        val nowInSeconds = now.timeInSeconds()

        var candles = when (timespan) {
            TimeInSeconds.oneHour -> hourCandles
            TimeInSeconds.oneWeek -> weekCandles
            TimeInSeconds.oneMonth -> monthCandles
            TimeInSeconds.oneYear -> allTimeCandles
            currency.lifetimeInSeconds -> allTimeCandles
            TimeInSeconds.oneDay -> dayCandles
            else -> dayCandles
        }.toMutableList()

        val lastCandleTime = candles.lastOrNull()?.time?.toLong() ?: longAgoInSeconds
        var nextCandleTime: Long = lastCandleTime + Candle.granularityForTimespan(timespan)

//        if (timespan != product.candlesTimespan) {
//            nextCandleTime = longAgoInSeconds
//        }
        candlesTimespan = timespan

        if (nextCandleTime < nowInSeconds) {
            var missingTime = nowInSeconds - lastCandleTime
            if (missingTime > timespan) {
                missingTime = timespan
            }
            GdaxApi.candles(id, missingTime, null, 0).getCandles(onFailure, { candleList ->
                var didGetNewCandle = false
                if (candleList.isNotEmpty()) {
                    val newLastCandleTime = candleList.lastOrNull()?.time?.toInt() ?: 0.0
                    didGetNewCandle = (lastCandleTime != newLastCandleTime)
                    if (didGetNewCandle) {
                        val timespanStart = nowInSeconds - timespan

                        if (candles.isNotEmpty()) {
                            if (timespan != TimeInSeconds.oneYear) {
                                val firstInTimespan = candles.indexOfFirst { candle -> candle.time >= timespanStart }
                                candles = candles.subList(firstInTimespan, candles.lastIndex).toMutableList()
                            }
                            candles.addAll(candleList)
                        } else {
                            candles = candleList.toMutableList()
                        }

                        when (timespan) {
                            TimeInSeconds.oneHour -> hourCandles = candles
                            TimeInSeconds.oneWeek -> weekCandles = candles
                            TimeInSeconds.oneMonth -> monthCandles = candles
                            TimeInSeconds.oneYear -> allTimeCandles = candles
                            currency.lifetimeInSeconds -> allTimeCandles = candles
                            TimeInSeconds.oneDay -> dayCandles = candles
                            else -> dayCandles = candles
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
