package com.jmnbehar.gdax.Classes

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

/**
 * Created by jmnbehar on 12/19/2017.
 */

enum class TradeSide {
    BUY,
    SELL;

    override fun toString() : String {
        return when (this) {
            BUY -> "buy"
            SELL -> "sell"
        }
    }
}

enum class TradeType {
    MARKET,
    LIMIT,
    STOP;

    override fun toString() : String {
        return when (this) {
            MARKET -> "market"
            LIMIT -> "limit"
            STOP -> "stop"
        }
    }
    companion object {
        fun fromString(string: String) : TradeType {
            return when (string) {
                "market"  ->  MARKET
                "limit"  -> LIMIT
                "stop" -> STOP
                else -> MARKET
            }
        }
    }
}

data class ApiProduct(
        val id: String,
        val base_currency: String,
        val quote_currency: String,
        val base_min_size: String,
        val base_max_size: String,
        val quote_increment: String,
        val display_name: String,
        val status: String,
        val margin_enabled: Boolean,
        val status_message: String?)

data class ApiOrder(
        val id: String,
        val price: String,
        val size: String?,
        val product_id: String,
        val side: String,
        val stp: String,
        val funds: String?,
        val specified_funds: String?,
        val type: String,
        val time_in_force: String,
        val post_only: Boolean,
        val created_at: String,
        val done_at: String,
        val done_reason: String,
        val fill_fees: String,
        val filled_size: String,
        val executed_value: String,
        val status: String,
        val settled: Boolean)

data class ApiFill(
        val trade_id: Int,
        val product_id: String,
        val price: String,
        val size: String,
        val order_id: String,
        val created_at: String,
        val liquidity: String,
        val fee: String,
        val settled: Boolean,
        val side: String)

data class ApiTicker(
        val trade_id: Int,
        val price: String,
        val size: String,
        val volume: String,
        val time: String)

data class ApiCurrencies(
        val id: String,
        val name: String,
        val min_size: String)

data class ApiStats(
        val open: String,
        val high: String,
        val low: String,
        val volume: String)

data class ApiTime(
        val iso: String,
        val epoch: String)

data class Candle(
        val time: Double,
        val low: Double,
        val high: Double,
        val open: Double,
        val close: Double,
        val volume: Double) {

     companion object {

         //TODO: consider moving this code elsewhere
         fun getCandles(productId: String, timespan: Int, granularity: Int?, onFailure: (Result.Failure<String, FuelError>) -> Unit, onComplete: (List<Candle>) -> Unit) {
             //TODO: fix for a year or more
             var granularity = granularity
             if (granularity == null) {
                 granularity = Candle.granularityForTimespan(timespan)
             }
             var currentTimespan = timespan
             var remainingTimespan = 0
             if ((timespan / granularity) > 300) {
                 //split into 2 requests
                 currentTimespan = granularity * 300
                 remainingTimespan = timespan - currentTimespan
             }
             GdaxApi.candles(productId, currentTimespan, granularity).executeRequest(onFailure) { result ->
                 val gson = Gson()
                 val apiCandles = result.value
                 val candleDoubleList: List<List<Double>> = gson.fromJson(apiCandles, object : TypeToken<List<List<Double>>>() {}.type)
                 var candles = candleDoubleList.map { Candle(it[0], it[1], it[2], it[3], it[4], it[5]) }
                 var now = Calendar.getInstance()

                 var start = now.timeInSeconds() - timespan - 30

                 candles = candles.filter { it.time >=  start }

                 candles = candles.reversed()
                 onComplete(candles)
             }
         }

         fun granularityForTimespan(timespan: Int) : Int {
             return when (timespan) {
                 TimeInSeconds.halfHour -> TimeInSeconds.oneMinute
                 TimeInSeconds.oneHour -> TimeInSeconds.oneMinute
                 TimeInSeconds.sixHours -> TimeInSeconds.fiveMinutes
                 TimeInSeconds.oneDay -> TimeInSeconds.fiveMinutes
                 TimeInSeconds.oneWeek -> TimeInSeconds.oneHour
                 TimeInSeconds.twoWeeks -> TimeInSeconds.oneHour
                 TimeInSeconds.oneMonth -> TimeInSeconds.sixHours
                 else -> TimeInSeconds.fiveMinutes
             }
         }

     }
}

data class ApiAccount(
        val id: String,
        val currency: String,
        val balance: String,
        val holds: String,
        val available: String,
        val profile_id: String)