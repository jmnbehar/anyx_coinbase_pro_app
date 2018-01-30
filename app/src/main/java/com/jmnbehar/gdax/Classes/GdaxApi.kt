package com.jmnbehar.gdax.Classes

import android.os.Handler
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.util.Base64
import com.github.kittinunf.fuel.util.FuelRouting
import com.github.kittinunf.result.Result
import com.jmnbehar.gdax.Activities.MainActivity
import org.jetbrains.anko.support.v4.toast
import org.json.JSONObject
import java.text.SimpleDateFormat
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

        fun defaultPostFailure(result: Result.Failure<ByteArray, FuelError>) : String {
            val errorCode = GdaxApi.ErrorCode.withCode(result.error.response.statusCode)
//            val fragment = MainActivity.currentFragment!!
//            when (errorCode) {
//                GdaxApi.ErrorCode.BadRequest -> { fragment.toast("400 Error: Missing something from the request")}
//                GdaxApi.ErrorCode.Unauthorized -> { fragment.toast("401 Error: You don't have permission to do that")}
//                GdaxApi.ErrorCode.Forbidden -> { fragment.toast("403 Error: You don't have permission to do that")}
//                GdaxApi.ErrorCode.NotFound -> { fragment.toast("404 Error: Content not found")}
//                GdaxApi.ErrorCode.TooManyRequests -> { fragment.toast("Error! Too many requests in a row")}
//                GdaxApi.ErrorCode.ServerError -> { fragment.toast("Error! Sorry, Gdax Servers are encountering problems right now")}
//                GdaxApi.ErrorCode.UnknownError -> { fragment.toast("Error!: ${result.error}")}
//            }

            return when (errorCode) {
                GdaxApi.ErrorCode.BadRequest -> { "400 Error: Missing something from the request" }
                GdaxApi.ErrorCode.Unauthorized -> { "401 Error: You don't have permission to do that" }
                GdaxApi.ErrorCode.Forbidden -> { "403 Error: You don't have permission to do that" }
                GdaxApi.ErrorCode.NotFound -> { "404 Error: Content not found" }
                GdaxApi.ErrorCode.TooManyRequests -> { "Error! Too many requests in a row" }
                GdaxApi.ErrorCode.ServerError -> { "Error! Sorry, Gdax Servers are encountering problems right now" }
                GdaxApi.ErrorCode.UnknownError -> { "Error!: ${result.error}" }
                else -> ""
            }
        }

        fun developerAddress(currency: Currency) : String {
            return when (currency) {
                Currency.BTC -> "1xxxxx"
                Currency.ETH -> "0x3333"
                Currency.BCH -> "1xxxxx"
                Currency.LTC -> "0xabcd"
                Currency.USD -> "my irl address?"
            }
        }

    }

    enum class ErrorCode(val code: Int) {
        BadRequest(400), //Invalid request format
        Unauthorized(401),
        Forbidden(403),
        NotFound(404),
        TooManyRequests(429),
        ServerError(500), //Problem with our server
        UnknownError(999);

        companion object {
            fun withCode(code: Int): ErrorCode {
                return when (code) {
                    400 -> BadRequest//Invalid request format
                    401 -> Unauthorized
                    403 -> Forbidden
                    404 -> NotFound
                    429 -> TooManyRequests
                    500 -> ServerError //Problem with our server
                    else -> UnknownError
                }
            }
        }
    }

    override val basePath = Companion.basePath


    class accounts() : GdaxApi()
    class account(val accountId: String) : GdaxApi()
    class products() : GdaxApi()
    class ticker(val productId: String) : GdaxApi()
    class candles(val productId: String, val time: Int = TimeInSeconds.oneDay, var granularity: Int? = null) : GdaxApi()
    class orderLimit(val tradeSide: TradeSide, val productId: String, val price: Double, val size: Double, val timeInForce: TimeInForce?, val cancelAfter: String?) : GdaxApi()
    class orderMarket(val tradeSide: TradeSide, val productId: String, val size: Double? = null, val funds: Double? = null) : GdaxApi()
    class orderStop(val tradeSide: TradeSide, val productId: String, val price: Double, val size: Double? = null, val funds: Double? = null) : GdaxApi()
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

    private var timeLock = 0

    fun executeRequest(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (result: Result.Success<String, FuelError>) -> Unit) {
       // MainActivity.progressDialog?.show()
        Fuel.request(this).responseString { _, _, result ->
            when (result) {
                is Result.Failure -> {
                    if (result.error.response.statusCode == ErrorCode.TooManyRequests.code) {
                        timeLock++
                        val handler = Handler()
                        var retry = Runnable {  }
                        retry = Runnable {
                            timeLock--
                            if (timeLock <= 0) {
                                timeLock = 0
                                executeRequest(onFailure, onSuccess)
                            } else {
                                handler.postDelayed(retry, 200.toLong())
                            }
                        }
                        handler.postDelayed(retry, 1000.toLong())
                    } else {
                        onFailure(result)
                   //     MainActivity.progressDialog?.dismiss()
                    }
                }
                is Result.Success -> {
                    onSuccess(result)
                 //   MainActivity.progressDialog?.dismiss()
                }
            }
        }
    }

    fun executePost(onFailure: (result: Result.Failure<ByteArray, FuelError>) -> Unit, onSuccess: (result: Result<ByteArray, FuelError>) -> Unit) {
       // Fuel.post(this.request.url.toString()).body(paramsToBody()).header(headers).response  { request, _, result ->
        Fuel.post(this.request.url.toString())
                .header(headers)
                .body(body)
                .response  { _, _, result ->
                    when (result) {
                        is Result.Failure -> onFailure(result)
                        is Result.Success -> onSuccess(result)
                    }
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
                    val utcTimeZone = TimeZone.getTimeZone("UTC")
                    var now = Calendar.getInstance(utcTimeZone)
                    var startInt = now.timeInSeconds() - time

                    var start = Date(startInt.toLong() * 1000)

                    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                    formatter.timeZone = utcTimeZone

                    paramList.add(Pair("start", formatter.format(start)))
                    paramList.add(Pair("end", formatter.format(now.time)))

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

    private fun basicOrderParams(tradeSide: TradeSide, tradeType: TradeType, productId: String): JSONObject {
        val json = JSONObject()
        json.put("side", tradeSide.toString())
        json.put("type", tradeType.toString())
        json.put("product_id", productId)
        return json
    }

    private val body: String
        get() {
            when (this) {
                is orderLimit -> {
                    var json = basicOrderParams(tradeSide, TradeType.LIMIT, productId)

                    json.put("price", "$price")
                    json.put("size", "$size")

                    if (timeInForce != null) {
                        json.put("time_in_force", timeInForce.toString())
                        if (timeInForce == TimeInForce.GoodTilTime && cancelAfter != null) {
                            json.put("cancel_after", cancelAfter)
                        }
                    }
                    return json.toString()
                }
                is orderMarket -> {
                    //can add either size or funds, for now lets do funds
                    var json = basicOrderParams(tradeSide, TradeType.MARKET, productId)

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
                    var json = basicOrderParams(tradeSide, TradeType.STOP, productId)

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
            var timestamp = (Date().timeInSeconds()).toString()
            var message = timestamp + method + path + body
            println("timestamp:")
            println(timestamp)

            val secretDecoded = Base64.decode(credentials.secret, 0)

            val sha256HMAC = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(secretDecoded, "HmacSHA256")
            sha256HMAC.init(secretKey)

            val hash = Base64.encodeToString(sha256HMAC.doFinal(message.toByteArray()), 0)
            println("hash:")
            println(hash)

            var headers: MutableMap<String, String> = mutableMapOf(Pair("CB-ACCESS-KEY", credentials.apiKey), Pair("CB-ACCESS-PASSPHRASE", credentials.passPhrase), Pair("CB-ACCESS-SIGN", hash), Pair("CB-ACCESS-TIMESTAMP", timestamp))


            if (method == Method.POST) {
                headers.put("Content-Type", "application/json")
            }

            return headers
        }


    enum class TimeInForce {
        GoodTilCancelled,
        GoodTilTime,
        ImmediateOrCancel,
        FirstOrKill;

        override fun toString(): String {
            return when (this) {
                GoodTilCancelled -> "GTC"
                GoodTilTime -> "GTT"
                ImmediateOrCancel -> "IOC"
                FirstOrKill -> "FOK"
            }
        }
        fun label(): String {
            return when (this) {
                GoodTilCancelled -> "Good Til Cancelled"
                GoodTilTime -> "Good Til Time"
                ImmediateOrCancel -> "Immediate Or Cancel"
                FirstOrKill -> "First Or Kill"
            }
        }
    }

}