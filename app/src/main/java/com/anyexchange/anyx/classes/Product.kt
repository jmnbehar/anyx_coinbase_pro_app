package com.anyexchange.anyx.classes

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import java.util.*
import com.google.gson.reflect.TypeToken


/**
 * Created by anyexchange on 12/20/2017.
 */

class Product(var currency: Currency, var id: String, var quoteCurrency: Currency?, var tradingPairs: List<TradingPair>) {
    constructor(apiProduct: ApiProduct, tradingPairs: List<TradingPair>)
            : this(Currency.forString(apiProduct.base_currency) ?: Currency.USD, apiProduct.id,
            Currency.forString(apiProduct.quote_currency) ?: Currency.USD, tradingPairs)

    init {
        tradingPairs = tradingPairs.sortedBy { it.quoteCurrency != Account.fiatCurrency }
    }

    var price = 0.0

    private var hourCandles = Array<List<Candle>>(tradingPairs.size) { listOf() }
    var dayCandles = Array<List<Candle>>(tradingPairs.size) { listOf() }
    private var weekCandles = Array<List<Candle>>(tradingPairs.size) { listOf() }
    private var monthCandles = Array<List<Candle>>(tradingPairs.size) { listOf() }
    private var yearCandles = Array<List<Candle>>(tradingPairs.size) { listOf() }

    private var candlesTimespan = Timespan.DAY

    fun percentChange(timespan: Timespan, tradingPair: TradingPair?) : Double {
        val currentPrice = price
        val candles = candlesForTimespan(timespan, tradingPair)
        val open = if (candles.isNotEmpty()) {
            candles.first().close
        } else {
            0.0
        }
        hourCandles[0]
        val change = currentPrice - open

        val weightedChange: Double = (change / open)
        return weightedChange * 100.0
    }

    fun candlesForTimespan(timespan: Timespan, tradingPair: TradingPair?): List<Candle> {
        //null trading pair will simply select the default fiat pair
        var tradingPairIndex: Int = tradingPairs.indexOf(tradingPair)
        if (tradingPairIndex == -1) { tradingPairIndex = 0 }
        return when (timespan) {
            Timespan.HOUR -> hourCandles[tradingPairIndex]
            Timespan.DAY -> dayCandles[tradingPairIndex]
            Timespan.WEEK -> weekCandles[tradingPairIndex]
            Timespan.MONTH -> monthCandles[tradingPairIndex]
            Timespan.YEAR -> yearCandles[tradingPairIndex]
        }
    }

    fun updateCandles(timespan: Timespan, tradingPair: TradingPair?, onFailure: (Result.Failure<String, FuelError>) -> Unit, onComplete: (didUpdate: Boolean) -> Unit) {
        val now = Calendar.getInstance()
        val longAgo = Calendar.getInstance()
        longAgo.add(Calendar.YEAR, -2)
        val longAgoInSeconds = longAgo.timeInSeconds()
        val nowInSeconds = now.timeInSeconds()

        var candles = candlesForTimespan(timespan, tradingPair).toMutableList()

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
            CBProApi.candles(tradingPair?.id ?: id, missingTime, granularity, 0).getCandles(onFailure) { candleList ->
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

                        var tradingPairIndex: Int = tradingPairs.indexOf(tradingPair)
                        if (tradingPairIndex == -1) { tradingPairIndex = 0 }
                        when (timespan) {
                            Timespan.HOUR -> hourCandles[tradingPairIndex] = candles
                            Timespan.DAY -> dayCandles[tradingPairIndex] = candles
                            Timespan.WEEK -> weekCandles[tradingPairIndex] = candles
                            Timespan.MONTH -> monthCandles[tradingPairIndex] = candles
                            Timespan.YEAR -> yearCandles[tradingPairIndex] = candles
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
            alertString += tradingPair.toString() + '\n'
        }
        return alertString
    }

    fun setAllBasicCandles(basicHourCandles: List<Candle>, basicDayCandles: List<Candle>, basicWeekCandles: List<Candle>, basicMonthCandles: List<Candle>, basicYearCandles: List<Candle>) {
        hourCandles[0] = basicHourCandles
        dayCandles[0] = basicDayCandles
        weekCandles[0] = basicWeekCandles
        monthCandles[0] = basicMonthCandles
        yearCandles[0] = basicYearCandles
    }

    companion object {
        fun fromString(string: String): Product {
            val splitString = string.split('\n')
            val currency = Currency.forString(splitString[0]) ?: Currency.USD
            val id = splitString[1]
            val quoteCurrency = if (splitString.size > 2) { Currency.forString(splitString[2]) ?: Currency.USD } else { Currency.USD }
            val tradingPairStrings = if (splitString.size > 3) { splitString.subList(3, splitString.size - 1) } else { listOf() }
            val tradingPairs = tradingPairStrings.map { TradingPair(it) }
            return Product(currency, id, quoteCurrency, tradingPairs)
        }

        fun fiatProduct(currency: Currency): Product {
            val fiatProduct = Product(currency, currency.productId, null, listOf())
            fiatProduct.price = 1.0
            return fiatProduct
        }


        fun updateAllProducts(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
            CBProApi.products().executeRequest(onFailure) { result ->
                val fiatCurrency = Account.fiatCurrency
                val gson = Gson()
                val unfilteredApiProductList: List<ApiProduct> = gson.fromJson(result.value, object : TypeToken<List<ApiProduct>>() {}.type)
                val apiProductList = unfilteredApiProductList.filter { s ->
                    s.quote_currency == fiatCurrency.toString()
                }
                for (apiProduct in apiProductList) {
                    val baseCurrency = apiProduct.base_currency
                    val relevantProducts = unfilteredApiProductList.filter { it.base_currency == baseCurrency }.map { it.id }
                    val newProduct = Product(apiProduct, relevantProducts.map { TradingPair(it) })
                    val currency = Currency.forString(baseCurrency) ?: Currency.USD
                    val relevantAccount = Account.forCurrency(currency)
                    relevantAccount?.product?.currency = newProduct.currency
                    relevantAccount?.product?.id = newProduct.id
                    relevantAccount?.product?.quoteCurrency = newProduct.quoteCurrency
                    relevantAccount?.product?.tradingPairs = newProduct.tradingPairs
                }
                onComplete()
            }
        }


    }

}
