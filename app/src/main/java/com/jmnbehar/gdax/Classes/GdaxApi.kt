package com.jmnbehar.gdax.Classes

import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.util.FuelRouting
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Created by jmnbehar on 12/18/2017.
 */

class ApiCredentials(val passPhrase: String, val apiKey: String, val secret: String) {

}

sealed class GdaxApi: FuelRouting {


    override val basePath = "https://api.gdax.com"

    class accounts(val apiCredentials: ApiCredentials): GdaxApi() {}

    override val method: Method
        get() {
            when(this) {
                is accounts -> return Method.GET
            }
        }


    override val path: String
        get() {
            return when(this) {
                is accounts -> "/accounts"
            }
        }

    override val params: List<Pair<String, Any?>>?
        get() {
            return when(this) {
                is accounts -> null
            }
        }

    override val headers: Map<String, String>?
        get() {
            var apiCredentials: ApiCredentials? = null
            when(this) {
                is accounts -> apiCredentials = this.apiCredentials
            }
            if (apiCredentials != null) {
                val body = ""
                var timestamp = Date().toInstant().epochSecond.toString()
                var message = timestamp + method + path + body
                println("timestamp:")
                println(timestamp)

                val secretDecoded = Base64.getDecoder().decode(apiCredentials.secret)

                val sha256_HMAC = Mac.getInstance("HmacSHA256")
                val secret_key = SecretKeySpec(secretDecoded, "HmacSHA256")
                sha256_HMAC.init(secret_key)

                val hash = Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(message.toByteArray()))
                println("hash:")
                println(hash)

                var headers: Map<String, String> = mapOf(Pair("CB-ACCESS-KEY", apiCredentials.apiKey), Pair("CB-ACCESS-PASSPHRASE", apiCredentials.passPhrase), Pair("CB-ACCESS-SIGN", hash), Pair("CB-ACCESS-TIMESTAMP", timestamp))
                return headers
            } else {
                return null
            }
        }

}