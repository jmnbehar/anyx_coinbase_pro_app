package com.jmnbehar.anyx.Classes

import android.content.Context
import android.os.Handler
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.util.Base64
import com.github.kittinunf.fuel.util.FuelRouting
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Created by jmnbehar on 12/18/2017.
 */


sealed class AnyxApi : FuelRouting {
    class ApiCredentials(val apiKey: String, val apiSecret: String, val apiPassPhrase: String, var isValidated: Boolean?)

    companion object {
        //TODO: delete creds if api key becomes invalid
        val basePath = "https://any-x.com"

        init {
            FuelManager.instance.basePath = basePath
        }

        fun defaultPostFailure(result: Result.Failure<ByteArray, FuelError>) : String {
            val errorCode = AnyxApi.ErrorCode.withCode(result.error.response.statusCode)

            return when (errorCode) {
                AnyxApi.ErrorCode.BadRequest -> { "400 Error: Missing something from the request" }
                AnyxApi.ErrorCode.Unauthorized -> { "401 Error: You don't have permission to do that" }
                AnyxApi.ErrorCode.Forbidden -> { "403 Error: You don't have permission to do that" }
                AnyxApi.ErrorCode.NotFound -> { "404 Error: Content not found" }
                AnyxApi.ErrorCode.TooManyRequests -> { "Error! Too many requests in a row" }
                AnyxApi.ErrorCode.ServerError -> { "Sorry, Gdax Servers are encountering problems right now" }
                AnyxApi.ErrorCode.UnknownError -> { "Error!: ${result.error}" }
                else -> ""
            }
        }


        fun testApiKey(email: String, context: Context, onComplete: (Boolean) -> Unit) {
            val credentials = GdaxApi.credentials
            if (credentials != null) {
                val prefs = Prefs(context)
                if (Account.list.isNotEmpty()) {
//                    val nonEmptyAccount = Account.list.find { account -> account.balance >= account.currency.minSendAmount }
                    val nonEmptyAccount = Account.list.find { account -> account.currency == Currency.BTC }
                    if (nonEmptyAccount == null) {
                        //Buy the smallest amount possible over the min send amount and send it
                        onComplete(false)
                    } else {
                        val currency = nonEmptyAccount.currency
                        AnyxApi.Verify(credentials.apiKey, currency).executeRequest({
                            onComplete(false)
                        }, {
                            GdaxApi.sendCrypto(currency.minSendAmount, currency, GdaxApi.developerAddress(currency)).executeRequest({
                                onComplete(false)
                        }, {
                                onComplete(true)
                            })
                        })
                    }
                } else {
                    onComplete(false)
                }
            }
        }
    }

    fun executeRequest(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (result: Result.Success<String, FuelError>) -> Unit) {
        // MainActivity.progressDialog?.show()
        Fuel.request(this).responseString { _, _, result ->
            when (result) {
                is Result.Failure -> {
                    onFailure(result)
                }
                is Result.Success -> {
                    onSuccess(result)
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

    enum class ErrorCode(val code: Int) {
        BadRequest(400), //Invalid request format
        Unauthorized(401),
        Forbidden(403),
        NotFound(404),
        TooManyRequests(429),
        ServerError(500), //Problem with our server
        ServiceUnavailable(503), //Problem with our server
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
                    503 -> ServiceUnavailable //Problem with our server
                    else -> UnknownError
                }
            }
        }
    }

    override val basePath = Companion.basePath

    class IsVerfied(val apiKey: String) : AnyxApi()
    class Verify(val apiKey: String, val currency: Currency) : AnyxApi()
    class VerificationSent(val apiKey: String, val email: String) : AnyxApi()


    override val method: Method
        get() {
            return when (this) {
                is IsVerfied -> Method.GET
                is Verify -> Method.POST
                is VerificationSent -> Method.POST
            }
        }

    override val path: String
        get() {
            return when (this) {
                is IsVerfied -> "/verify"
                is Verify -> "/verify"
                is VerificationSent -> "/verify/sent"
            }
        }

    override val params: List<Pair<String, Any?>>?
        get() {
            val paramList = mutableListOf<Pair<String, String>>()
            when (this) {
                is IsVerfied -> {
                    paramList.add(Pair("apiKey", apiKey))
                    return paramList.toList()
                }
                else -> return null
            }
        }

    private val body: String
        get() {
            when (this) {
                is Verify -> {
                    val json = JSONObject()

                    json.put("apiKey", apiKey)
                    json.put("currency", currency.toString())
                    return json.toString()
                }
                is VerificationSent -> {
                    val json = JSONObject()

                    json.put("apiKey", apiKey)
                    json.put("email", email)
                    return json.toString()
                }
                else -> return ""
            }
        }

    override val headers: Map<String, String>?
        get() {
            var headers: MutableMap<String, String> = mutableMapOf()

            if (method == Method.POST) {
                headers.put("Content-Type", "application/json")
            }
            return headers
        }


}