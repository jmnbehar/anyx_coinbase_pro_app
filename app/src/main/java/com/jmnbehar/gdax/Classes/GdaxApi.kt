package com.jmnbehar.gdax.Classes

import android.os.Handler
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.util.FuelRouting
import com.github.kittinunf.result.Result
import org.json.JSONObject
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Created by jmnbehar on 12/18/2017.
 */


sealed class GdaxApi: FuelRouting {


    companion object {
        lateinit var credentials: ApiCredentials
        val basePath = "https://api.gdax.com"

        init {
            FuelManager.instance.basePath = basePath
        }

    }

    override val basePath = Companion.basePath


    class accounts() : GdaxApi()
    class account(val accountId: String) : GdaxApi()
    class products() : GdaxApi()
    class ticker(val productId: String) : GdaxApi()
    class candles(val productId: String, val time: Int = TimeInSeconds.oneDay, var granularity: Int? = null) : GdaxApi()
    class orderLimit(val tradeType: TradeType, val productId: String, val price: Double, val size: Double) : GdaxApi()
    class orderMarket(val tradeType: TradeType, val productId: String, val size: Double? = null, val funds: Double? = null) : GdaxApi()
    class orderStop(val tradeType: TradeType, val productId: String, val price: Double, val size: Double? = null, val funds: Double? = null) : GdaxApi()
    class cancelOrder(val orderId: String) : GdaxApi()
    class cancelAllOrders() : GdaxApi()
    class listOrders(val status: String = "all", val productId: String?) : GdaxApi()
    class getOrder(val orderId: String) : GdaxApi()
    class fills(val orderId: String = "all", val productId: String = "all") : GdaxApi()
    //add position?
    //add deposit and withdrawal
    class send(val amount: Double, val currency: Currency, val cryptoAddress: String) : GdaxApi()
    //add payment methods
    //look into reports

    var timeLock = 0


    //TODO: make status enum
    fun executeRequest(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (result: Result.Success<String, FuelError>) -> Unit) {
        Fuel.request(this).responseString { request, _, result ->
            when (result) {
                is Result.Failure -> {
                    if ((result is Result.Failure) && (result.error.response.statusCode == 429)) {
                        timeLock++
                        val handler = Handler()
                        var retry = Runnable {  }
                        retry = Runnable {
                            timeLock--
                            if (timeLock <= 0) {
                                timeLock = 0
                                executeRequest(onFailure, onSuccess)
                            } else {
                                handler.postDelayed(retry, 1000.toLong())
                            }
                        }
                        handler.postDelayed(retry, 5000.toLong())
                    } else {
                        onFailure(result)
                    }
                }
                is Result.Success -> {
                    onSuccess(result)
                }
            }
        }
    }

    //TODO: consider combining functions
    fun executePost(onComplete: (result: Result<ByteArray, FuelError>) -> Unit) {
       // Fuel.post(this.request.url.toString()).body(paramsToBody()).header(headers).response  { request, _, result ->
        Fuel.post(this.request.url.toString())
                .header(headers)
                .body(body)
                .response  { request, _, result ->
            println(request.url)
            println("body callback = " + request.bodyCallback.toString())
            onComplete(result)
        }

    }

    override val method: Method
        get() {
            return when (this) {
                is accounts -> Method.GET
                is account -> Method.GET
                is products -> Method.GET
                is ticker -> Method.GET
                is candles -> Method.GET
                is orderLimit -> Method.POST
                is orderMarket -> Method.POST
                is orderStop -> Method.POST
                is cancelOrder -> Method.DELETE
                is cancelAllOrders -> Method.DELETE
                is listOrders -> Method.GET
                is getOrder -> Method.GET
                is fills -> Method.GET
                is send -> Method.POST
            }
        }


    override val path: String
        get() {
            return when (this) {
                is accounts -> "/accounts"
                is account -> "/accounts/$accountId"
                is products -> "/products"
                is ticker -> "/products/$productId/ticker"
                is candles -> "/products/$productId/candles"
                is orderLimit -> "/orders"
                is orderMarket -> "/orders"
                is orderStop -> "/orders"
                is cancelOrder -> "/orders/$orderId"
                is cancelAllOrders -> "/orders"
                is listOrders -> "/orders"
                is getOrder -> "/orders/$orderId"
                is fills -> "/fills"
                is send -> "/withdrawals/crypto"
            }
        }

    override val params: List<Pair<String, Any?>>?
        get() {
            var paramList = mutableListOf<Pair<String, String>>()
            when (this) {
                is candles -> {
                    var now: LocalDateTime = LocalDateTime.now(Clock.systemUTC())
                    var startInt = now.atZone(ZoneId.systemDefault()).toEpochSecond() - time
                    //var start = now.minusDays(1)
                    var start = Instant.ofEpochSecond(startInt).atZone(ZoneId.systemDefault()).toLocalDateTime()

                    paramList.add(Pair("start", start.toString()))

                    paramList.add(Pair("end", now.toString()))

                    if (granularity == null) {
                        granularity = when (time) {
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
                    paramList.add(Pair("granularity", granularity.toString()))
                    return paramList.toList()
                }
                is fills -> {
                    paramList.add(Pair("order_id", orderId))
                    paramList.add(Pair("product_id", productId))
                    return listOf()
                }
                is listOrders -> {
                    paramList.add(Pair("status", status))
                    if (productId != null) {
                        paramList.add(Pair("product_id", productId))
                    }
                    return listOf()
                }
                else -> return null
            }
        }

    private fun basicOrderParams(tradeType: TradeType, tradeSubType: TradeSubType, productId: String): JSONObject {
        val json = JSONObject()
        json.put("side", tradeType.toString())
        json.put("type", tradeSubType.toString())
        json.put("product_id", productId)
        return json
    }

    private val body: String
        get() {
            when (this) {
                is orderLimit -> {
                    var json = basicOrderParams(tradeType, TradeSubType.LIMIT, productId)

                    json.put("price", "$price")
                    json.put("size", "$size")

                    return json.toString()
                }
                is orderMarket -> {
                    //can add either size or funds, for now lets do funds
                    var json = basicOrderParams(tradeType, TradeSubType.MARKET, productId)

                    if (funds != null) {
                        json.put("funds", "$funds")
                    }
                    if (size != null) {
                          json.put("size", "$size")
                    }

                    return json.toString()
                }
                is orderStop -> {
                    //can add either size or funds, for now lets do funds
                    var json = basicOrderParams(tradeType, TradeSubType.STOP, productId)

                    json.put("price", "$price")
                    if (funds != null) {
                        json.put("funds", "$funds")
                    }
                    if (size != null) {
                        json.put("size", "$size")
                    }


                    return json.toString()
                }
                is send -> {
                    val json = JSONObject()

                    json.put("amount", amount)
                    json.put("currency", currency.toString())
                    json.put("crypto_address", cryptoAddress)
                    return json.toString()
                }
                else -> return ""
            }
        }

    override val headers: Map<String, String>?
        get() {
            var timestamp = Date().toInstant().epochSecond.toString()
            var message = timestamp + method + path + body
            println("timestamp:")
            println(timestamp)

            val secretDecoded = Base64.getDecoder().decode(credentials.secret)

            val sha256HMAC = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(secretDecoded, "HmacSHA256")
            sha256HMAC.init(secretKey)

            val hash = Base64.getEncoder().encodeToString(sha256HMAC.doFinal(message.toByteArray()))
            println("hash:")
            println(hash)

            var headers: MutableMap<String, String> = mutableMapOf(Pair("CB-ACCESS-KEY", credentials.apiKey), Pair("CB-ACCESS-PASSPHRASE", credentials.passPhrase), Pair("CB-ACCESS-SIGN", hash), Pair("CB-ACCESS-TIMESTAMP", timestamp))


            if (method == Method.POST) {
                headers.put("Content-Type", "application/json")
            }

            return headers
        }
}