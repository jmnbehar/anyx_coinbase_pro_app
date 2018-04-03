package com.jmnbehar.anyx.Classes

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.util.FuelRouting
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.util.*

/**
 * Created by jmnbehar on 12/18/2017.
 */


sealed class AnyxApi : FuelRouting {
    private var retryAttempt = 0
    private var maxRetries = 0

    companion object {
        val basePath = "http://192.168.1.239/api/v1"
//        val basePath = "http://35.230.105.241/api/v1"

        fun checkApiKey(apiKey: String, context: Context, onComplete: (result: Boolean?, isPending: Boolean) -> Unit) {
            val prefs = Prefs(context)
            val isApiKeyValid = prefs.isApiKeyValid(apiKey)
            if (isApiKeyValid != null) {
                onComplete(isApiKeyValid, false)
            } else {
                IsVerfied(apiKey).executeRequest({ onComplete(null, false) }, {result ->
                    val isVerifiedObject: AnyXIsVerified = Gson().fromJson(result.value, object : TypeToken<AnyXIsVerified>() {}.type)
                    val isPending = isVerifiedObject.status != null
                    onComplete(isVerifiedObject.verified, isPending)
                })
            }
        }
    }

    fun executeRequest(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (result: Result.Success<String, FuelError>) -> Unit) {
        FuelManager.instance.basePath = Companion.basePath
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

    fun executePost(onFailure: (result: Result.Failure<ByteArray, FuelError>) -> Unit, onSuccess: (result: Result<ByteArray, FuelError>) -> Unit, setMaxRetries: Int? = null) {
        FuelManager.instance.basePath = Companion.basePath
        if (setMaxRetries != null) {
            maxRetries = setMaxRetries
        }
        Fuel.post(this.request.url.toString())
                .header(headers)
                .body(body)
                .response  { _, _, result ->
                    when (result) {
                        is Result.Failure -> {
                            if (retryAttempt < maxRetries) {
                                retryAttempt++
                                executePost(onFailure, onSuccess)
                            } else {
                                onFailure(result)
                            }
                        }
                        is Result.Success -> onSuccess(result)
                    }
                }
    }

    override val basePath = Companion.basePath

    class IsVerfied(val apiKey: String) : AnyxApi()
    class Verify(val apiKey: String, val currency: Currency) : AnyxApi()
    class VerificationSent(val apiKey: String, val email: String, val amountConfirm: String) : AnyxApi()

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
                is VerificationSent -> "/sent"
            }
        }

    override val params: List<Pair<String, Any?>>?
        get() {
            val paramList = mutableListOf<Pair<String, String>>()
            when (this) {
                is IsVerfied -> {
                    paramList.add(Pair("api_key", apiKey))
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

                    json.put("api_key", apiKey)
                    json.put("coin", currency.toString().toLowerCase())
                    return json.toString()
                }
                is VerificationSent -> {
                    val json = JSONObject()

                    json.put("api_key", apiKey)
                    json.put("email", email)
                    json.put("amount_confirm", amountConfirm)

                    var timestamp = (Date().timeInSeconds()).toString()
                    json.put("timestamp", timestamp)

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
}