package com.jmnbehar.gdax.Classes

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.util.FuelRouting
import com.github.kittinunf.result.Result
import org.json.JSONObject
import java.time.Clock
import java.time.LocalDateTime
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Created by jmnbehar on 12/18/2017.
 */

class ApiCredentials(val passPhrase: String, val apiKey: String, val secret: String)


object Granularity {
    val oneMinute = 60
    val fiveMinutes = 300
    val fifteenMinutes = 900
    val oneHour = 3600
    val sixHours = 21600
    val oneDay = 86400
}

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
    class candles(val productId: String, val time: Int = Granularity.oneDay, val granularity: Int = Granularity.fifteenMinutes) : GdaxApi()
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
    class send(val amount: Double, val productId: String, val cryptoAddress: String) : GdaxApi()
    //add payment methods
    //look into reports


    //TOOD: consider making productId enum
    //TODO: make status enum

    fun executeRequest(onComplete: (result: Result<String, FuelError>) -> Unit) {
        Fuel.request(this).responseString { request, _, result ->
            println(request.url)
            onComplete(result)
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
                    var start = now.minusDays(1)

                    paramList.add(Pair("start", start.toString()))
                    paramList.add(Pair("end", now.toString()))
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
                is send -> {
                    paramList.add(Pair("size", "$amount"))
                    paramList.add(Pair("currency", productId))
                    paramList.add(Pair("cryptoAddress", cryptoAddress))

                    return paramList.toList()
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
//                    var paramList = mutableListOf<Pair<String, String>>()
//                    paramList.add(Pair("size", "$size"))
//                    paramList.add(Pair("currency", productId))
//                    paramList.add(Pair("cryptoAddress", cryptoAddress))
//
//                    return paramList.toList()
                    return ""
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