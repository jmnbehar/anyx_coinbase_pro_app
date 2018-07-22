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

class Product(var currency: Currency, var id: String,  candles: List<Candle>) {
    constructor(apiProduct: ApiProduct, candles: List<Candle>)
            : this(Currency.forString(apiProduct.base_currency) ?: Currency.USD, apiProduct.id, candles)

    var price = candles.lastOrNull()?.close ?: 0.0

    private var hourCandles = listOf<Candle>()
    var dayCandles = candles
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

    override fun toString(): String {
        var alertString = currency.toString() + '\n'
        alertString += id + '\n'
//        alertString += price.toString() + '\n'
        return alertString
    }


    fun toJson() : JSONObject {
        val json = JSONObject()
        json.put("currency", currency.toString())
        json.put("id", id)

        val gson = Gson()

        //TODO: delete some stuff here
        val hourCandlesElement = gson.toJsonTree(hourCandles, object : TypeToken<List<Candle>>() { }.type)
        val hourCandlesJsonArray = hourCandlesElement

        val dayCandlesElement = gson.toJsonTree(dayCandles, object : TypeToken<List<Candle>>() { }.type)
        val dayCandlesJsonArray = dayCandlesElement

        val weekCandlesElement = gson.toJsonTree(weekCandles, object : TypeToken<List<Candle>>() { }.type)
        val weekCandlesJsonArray = weekCandlesElement

        val monthCandlesElement = gson.toJsonTree(monthCandles, object : TypeToken<List<Candle>>() { }.type)
        val monthCandlesJsonArray = monthCandlesElement

        val yearCandlesElement = gson.toJsonTree(yearCandles, object : TypeToken<List<Candle>>() { }.type)
        val yearCandlesJsonArray = yearCandlesElement

//        val allCandlesElement = gson.toJsonTree(allTimeCandles, object : TypeToken<List<Candle>>() { }.type)
//        val allCandlesJsonArray = allCandlesElement

        if (hourCandles.isNotEmpty()) {
            json.put("hourCandles", hourCandlesJsonArray)
        }
        if (dayCandles.isNotEmpty()) {
            json.put("dayCandles", dayCandlesJsonArray)
        }
        if (weekCandles.isNotEmpty()) {
            json.put("weekCandles", weekCandlesJsonArray)
        }
        if (monthCandles.isNotEmpty()) {
            json.put("monthCandles", monthCandlesJsonArray)
        }
        if (yearCandles.isNotEmpty()) {
            json.put("yearCandles", yearCandlesJsonArray)
        }
//        if (allTimeCandles.isNotEmpty()) {
//            json.put("allCandles", allCandlesJsonArray)
//        }
        return json
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
    companion object {
        fun fromString(string: String): Product {
            val splitString = string.split('\n')
            val currency = Currency.forString(splitString[0]) ?: Currency.USD
            val id = splitString[1]
//            val price = splitString[1].toDoubleOrZero()
            return Product(currency, id, listOf())
        }

        fun fiatProduct(currency: String) = Product(Currency.forString(currency) ?: Currency.USD, currency, listOf(fiatCandle))

        private val fiatCandle  = Candle(0.0, 1.0, 1.0, 1.0, 1.0, 0.0)
        //private val blankCandle = Candle(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }

}
