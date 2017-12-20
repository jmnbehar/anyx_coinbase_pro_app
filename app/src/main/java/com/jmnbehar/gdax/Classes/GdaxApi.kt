package com.jmnbehar.gdax.Classes

import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.util.FuelRouting
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Created by jmnbehar on 12/18/2017.
 */

class ApiCredentials(val passPhrase: String, val apiKey: String, val secret: String) {

}

sealed class GdaxApi: FuelRouting {


    companion object {
        lateinit var credentials: ApiCredentials
    }


    override val basePath = "https://api.gdax.com"

    class accounts(): GdaxApi() {}
    class products(): GdaxApi() {}
    class ticker(val productId: String): GdaxApi() {}
    class candles(val productId: String, val time: Int = 86400, val granularity: Int = 432): GdaxApi() {}

    override val method: Method
        get() {
            when(this) {
                is accounts -> return Method.GET
                is products -> return Method.GET
                is ticker -> return Method.GET
                is candles -> return Method.GET
            }
        }


    override val path: String
        get() {
            return when(this) {
                is accounts -> "/accounts"
                is products -> "/products"
                is ticker -> "/products/$productId/ticker"
                is candles -> "/products/$productId/candles"
            }
        }

    override val params: List<Pair<String, Any?>>?
        get() {
            when(this) {
                is candles -> {

                    var now: LocalDateTime = LocalDateTime.now()
                    var start = now.minusDays(1)
                    return listOf(Pair("start", start), Pair("end", now), Pair("granularity", granularity.toString()))
                }
                else  -> return null
            }
        }

    fun toISO8601UTC(date: Date): String {
        val tz = TimeZone.getTimeZone("UTC")
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
        df.setTimeZone(tz)
        return df.format(date)
    }


    override val headers: Map<String, String>?
        get() {
            val body = ""
            var timestamp = Date().toInstant().epochSecond.toString()
            var message = timestamp + method + path + body
            println("timestamp:")
            println(timestamp)

            val secretDecoded = Base64.getDecoder().decode(credentials.secret)

            val sha256_HMAC = Mac.getInstance("HmacSHA256")
            val secret_key = SecretKeySpec(secretDecoded, "HmacSHA256")
            sha256_HMAC.init(secret_key)

            val hash = Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(message.toByteArray()))
            println("hash:")
            println(hash)

            var headers: Map<String, String> = mapOf(Pair("CB-ACCESS-KEY", credentials.apiKey), Pair("CB-ACCESS-PASSPHRASE", credentials.passPhrase), Pair("CB-ACCESS-SIGN", hash), Pair("CB-ACCESS-TIMESTAMP", timestamp))
            return headers
        }
}