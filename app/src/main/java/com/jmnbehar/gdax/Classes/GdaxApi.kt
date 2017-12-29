package com.jmnbehar.gdax.Classes

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.util.FuelRouting
import com.github.kittinunf.result.Result
import com.jmnbehar.gdax.Fragments.TradeFragment
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
    class candles(val productId: String, val time: Int = 86400, val granularity: Int = Granularity.fifteenMinutes) : GdaxApi()
    class orderLimit(val tradeType: TradeType, val productId: String, val price: Double, val amount: Double) : GdaxApi()
    class orderMarket(val tradeType: TradeType, val productId: String, val amount: Double? = null, val size: Double? = null) : GdaxApi()
    class orderStop(val tradeType: TradeType, val productId: String, val price: Double, val amount: Double? = null, val size: Double? = null) : GdaxApi()
    class cancelOrder(val orderId: String) : GdaxApi()
    class cancelAllOrders() : GdaxApi()
    class listOrders(val status: String) : GdaxApi()
    class getOrder(val orderId: String) : GdaxApi()
    class fills(val orderId: String? = null, val productId: String? = null) : GdaxApi()
    //add position?
    //add deposit and withdrawal
    class send(val amount: Double, val productId: String, val cryptoAddress: String) : GdaxApi()
    //add payment methods
    //look into reports


    //TOOD: consider making productId enum
    //TODO: make status enum

    fun executeRequest(onComplete: (result: Result<String, FuelError>) -> Unit) {
        Fuel.request(this).responseString { _, _, result ->
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

    private fun basicOrderParams(tradeType: TradeType, tradeSubType: TradeSubType, productId: String): MutableList<Pair<String, String>> {
        val orderId = Pair("client_oid", "GdaxAppTrade")
        val side = Pair("side", tradeType.toString())
        val type = Pair("type", tradeSubType.toString())
        val productId = Pair("product_id", productId)

        return mutableListOf(orderId, side, type, productId)
    }

    override val params: List<Pair<String, Any?>>?
        get() {
            when (this) {
                is candles -> {
                    var now: LocalDateTime = LocalDateTime.now(Clock.systemUTC())
                    var start = now.minusDays(1)

                    return listOf(Pair("start", start), Pair("end", now), Pair("granularity", granularity.toString()))
                }
                is orderLimit -> {
                    var paramList = basicOrderParams(tradeType, TradeSubType.LIMIT, productId)

                    paramList.add(Pair("price", "$price"))
                    paramList.add(Pair("size", "$amount"))

                    return paramList
                }
                is orderMarket -> {
                    var paramList = basicOrderParams(tradeType, TradeSubType.LIMIT, productId)

                    //can add either size or funds, for now lets do funds
                    if (size != null) {
                        paramList.add(Pair("size", "$size"))
                    } else if (amount != null) {
                        paramList.add(Pair("funds", "$amount"))
                    } else {
                        //Throw an error here?
                    }
                    return paramList
                }
                is orderStop -> {
                    var paramList = basicOrderParams(tradeType, TradeSubType.LIMIT, productId)

                    paramList.add(Pair("price", "$price"))
                    //can add either size or funds, for now lets do funds
                    if (size != null) {
                        paramList.add(Pair("size", "$size"))
                    } else if (amount != null) {
                        paramList.add(Pair("funds", "$amount"))
                    } else {
                        //Throw an error here?
                    }
                    return paramList
                }
                is fills -> {
                    var paramList = mutableListOf<Pair<String, String>>()
                    if (orderId != null) {
                        paramList.add(Pair("order_id", orderId))
                    }
                    if (productId != null) {
                        paramList.add(Pair("product_id", productId))
                    }
                    return paramList
                }
                is send -> {
                    var paramList = mutableListOf<Pair<String, String>>()
                    paramList.add(Pair("amount", "$amount"))
                    paramList.add(Pair("currency", productId))
                    paramList.add(Pair("cryptoAddress", cryptoAddress))

                    return paramList
                }
                else -> return null
            }
        }


    override val headers: Map<String, String>?
        get() {
            val body = ""
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

            var headers: Map<String, String> = mapOf(Pair("CB-ACCESS-KEY", credentials.apiKey), Pair("CB-ACCESS-PASSPHRASE", credentials.passPhrase), Pair("CB-ACCESS-SIGN", hash), Pair("CB-ACCESS-TIMESTAMP", timestamp))
            return headers
        }
}