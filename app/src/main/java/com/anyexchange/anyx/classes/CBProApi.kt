package com.anyexchange.anyx.classes

import android.content.Context
import android.os.Handler
import com.anyexchange.anyx.R
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.util.Base64
import com.github.kittinunf.fuel.util.FuelRouting
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Created by anyexchange on 12/18/2017.
 */

@Suppress("ClassName")
sealed class CBProApi : FuelRouting {
    class ApiCredentials(val apiKey: String, val apiSecret: String, val apiPassPhrase: String, var isValidated: Boolean?)

    companion object {
        //TODO: delete creds if api key becomes invalid
        var credentials: ApiCredentials? = null

        const val basePath = "https://api.pro.coinbase.com"

        fun defaultPostFailure(context: Context?, result: Result.Failure<ByteArray, FuelError>) : String {
            val errorCode = CBProApi.ErrorCode.withCode(result.error.response.statusCode)
            return if (context!= null) {
                when (errorCode) {
                    CBProApi.ErrorCode.BadRequest -> context.resources.getString(R.string.error_400)
                    CBProApi.ErrorCode.Unauthorized -> context.resources.getString(R.string.error_401)
                    CBProApi.ErrorCode.Forbidden -> context.resources.getString(R.string.error_403)
                    CBProApi.ErrorCode.NotFound -> context.resources.getString(R.string.error_404)
                    CBProApi.ErrorCode.TooManyRequests -> context.resources.getString(R.string.error_too_many)
                    CBProApi.ErrorCode.ServerError -> context.resources.getString(R.string.error_server)
                    CBProApi.ErrorCode.UnknownError -> context.resources.getString(R.string.error_generic_message, result.errorMessage)
                    else -> ""
                }
            } else {
                ""
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
        UnknownError(999),
        NoInternet(-1);

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
    private var timeLock = 0

    fun executeRequest(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (result: Result.Success<String, FuelError>) -> Unit) {
        FuelManager.instance.basePath = basePath
        Fuel.request(this).responseString { _, _, result ->
            when (result) {
                is Result.Failure -> {
                    when (result.error.response.statusCode) {
                        ErrorCode.TooManyRequests.code -> {
                            timeLock++
                            val handler = Handler()
                            var retry = Runnable { }
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
                        }
                        ErrorCode.BadRequest.code -> {
                            credentials = null
                            onFailure(result)
                        }
                        else -> onFailure(result)
                    }
                }
                is Result.Success -> {
                    onSuccess(result)
                }
            }
        }
    }

    fun executePost(onFailure: (result: Result.Failure<ByteArray, FuelError>) -> Unit, onSuccess: (result: Result<ByteArray, FuelError>) -> Unit) {
        FuelManager.instance.basePath = basePath
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

    class candles(val productId: String, val timespan: Long = Timespan.DAY.value(), var granularity: Long, var timeOffset: Long) : CBProApi() {
        fun getCandles(onFailure: (Result.Failure<String, FuelError>) -> Unit, onComplete: (List<Candle>) -> Unit) {
            var currentTimespan: Long
            var coveredTimespan: Long
            var nextCoveredTimespan: Long = 0
            var remainingTimespan: Long = timespan
            var pages = 1
            var pagesReceived = 0

            var allCandles = mutableListOf<Candle>()
            while (remainingTimespan > 0) {
                coveredTimespan = nextCoveredTimespan
                if ((remainingTimespan / granularity) > 300) {
                    //split into 2 requests
                    currentTimespan = granularity * 300
                    remainingTimespan -= currentTimespan
                    nextCoveredTimespan = coveredTimespan + currentTimespan
                    pages++
                } else {
                    currentTimespan = remainingTimespan
                    remainingTimespan = 0
                }

                CBProApi.candles(productId, currentTimespan, granularity, coveredTimespan).executeRequest(onFailure) { result ->
                    pagesReceived ++
                    val gson = Gson()
                    val apiCandles = result.value
                    val tradingPair = TradingPair(productId)
                    try {
                        val candleDoubleList: List<List<Double>> = gson.fromJson(apiCandles, object : TypeToken<List<List<Double>>>() {}.type)
                        var candles = candleDoubleList.map {
                            val time = (it[0] as? Double) ?: 0.0
                            val low = (it[1] as? Double) ?: 0.0
                            val high = (it[2] as? Double) ?: 0.0
                            val open = (it[3] as? Double) ?: 0.0
                            val close = (it[4] as? Double) ?: 0.0
                            val volume = (it[5] as? Double) ?: 0.0
                            Candle(time, low, high, open, close, volume, tradingPair)
                        }
                        val now = Calendar.getInstance()

                        val start = now.timeInSeconds() - timespan - 30

                        candles = candles.filter { it.time >= start }

                        //TODO: edit chart library so it doesn't show below 0
                        candles = candles.reversed()
                        allCandles = allCandles.addingCandles(candles)
                    } catch (exception: Exception) {
                        //Do nothing
                    }

                    if (pagesReceived == pages && allCandles.isNotEmpty()) {
                        if (pages > 1) {
                            allCandles = allCandles.sorted()
                        }
                        onComplete(allCandles)
                    }

                }
            }
        }
    }

    class accounts : CBProApi() {

        fun get(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (List<ApiAccount>) -> Unit) {
            this.executeRequest(onFailure) { result ->
                try {
                    val apiAccountList: List<ApiAccount> = Gson().fromJson(result.value, object : TypeToken<List<ApiAccount>>() {}.type)
                    onComplete(apiAccountList)
                } catch (e: JsonSyntaxException) {
                    onComplete(listOf())
                }
            }
        }

        fun getAllAccountInfo(context: Context, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
            Account.list.clear()
            val prefs = Prefs(context)

            val productList: MutableList<Product> = mutableListOf()
            val stashedProductList = prefs.stashedProducts
            if (stashedProductList.isNotEmpty()) {
                getAccountsWithProductList(stashedProductList, onFailure, onComplete)
            } else {
                CBProApi.products().get(onFailure) { apiProductList ->
                    for (apiProduct in apiProductList) {
                        val baseCurrency = apiProduct.base_currency
                        val relevantProducts = apiProductList.filter { it.base_currency == baseCurrency }.map { TradingPair(it.id) }
                        val newProduct = Product(apiProduct, relevantProducts)
                        productList.add(newProduct)
                    }
                    getAccountsWithProductList(productList, onFailure, onComplete)
                }
            }
        }

        private fun getAccountsWithProductList(productList: List<Product>, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
            if (CBProApi.credentials == null) {
                val fiatCurrency = Currency.USD
                val filteredProductList = productList.filter {
                    it.quoteCurrency == fiatCurrency
                }
                val fiatAccount = ApiAccount("", fiatCurrency.toString(), "0.0", "", "0.0", "")
                Account.fiatAccount = Account(Product.fiatProduct(fiatCurrency), fiatAccount)
                for (product in filteredProductList) {
                    val apiAccount = ApiAccount("", product.currency.toString(), "0.0", "", "0.0", "")
                    Account.list.add(Account(product, apiAccount))
                }
                Account.updateAllAccountsCandles(onFailure, onComplete)
            } else {
                this.get(onFailure) { apiAccountList ->
                    for (apiAccount in apiAccountList) {
                        val currency = Currency.forString(apiAccount.currency)
                        val relevantProduct = productList.find { p -> p.currency == currency }
                        if (relevantProduct != null) {
                            Account.list.add(Account(relevantProduct, apiAccount))
                        } else if (currency?.isFiat == true) {
                            Account.fiatAccount = Account(Product.fiatProduct(currency), apiAccount)
                        }
                    }
                    Account.updateAllAccountsCandles(onFailure, onComplete)
                }
            }
        }

        fun updateAllAccounts(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
            this.get(onFailure) { apiAccountList ->
                for (account in Account.list.plus(Account.fiatAccount)) {
                    val apiAccount = apiAccountList.find { a -> a.currency == account?.currency.toString() }
                    apiAccount?.let {
                        account?.apiAccount = it
                    }
                }
                onComplete()
            }
        }

    }

    class account(val accountId: String) : CBProApi() {
        fun get(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (ApiAccount?) -> Unit) {
            this.executeRequest(onFailure) { result ->
                //TODO: why does this sometimes get a jsonArray instead of a JSON?
                val gson = Gson()
                try {
                    val apiAccount: ApiAccount = gson.fromJson(result.value, object : TypeToken<ApiAccount>() {}.type)
                    onComplete(apiAccount)
                } catch (e: JsonSyntaxException) {
                    try {
                        val apiAccountList: List<ApiAccount> = gson.fromJson(result.value, object : TypeToken<List<ApiAccount>>() {}.type)
                        onComplete(apiAccountList.firstOrNull())
                    } catch (e: JsonSyntaxException) {
                        onComplete(null)
                    }
                    onComplete(null)
                }
            }
        }
    }

    class accountHistory(val accountId: String) : CBProApi()
    class products : CBProApi() {
        fun get(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (List<ApiProduct>) -> Unit) {
            this.executeRequest(onFailure) { result ->
                try {
                    val productList: List<ApiProduct> = Gson().fromJson(result.value, object : TypeToken<List<ApiProduct>>() {}.type)
                    onComplete(productList)
                } catch (e: JsonSyntaxException) {
                    onComplete(listOf())
                }
            }
        }
    }
    class ticker(accountId: String) : CBProApi() {
        val tradingPair = TradingPair(accountId)
        constructor(tradingPair: TradingPair): this(tradingPair.id)
        fun get(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (ApiTicker?) -> Unit) {
            this.executeRequest(onFailure) { result ->
                try {
                    val ticker: ApiTicker = Gson().fromJson(result.value, object : TypeToken<ApiTicker>() {}.type)
                    val price = ticker.price.toDoubleOrNull()
                    val account = Account.forCurrency(tradingPair.baseCurrency)
                    if (price != null) {
                        account?.product?.setPriceForTradingPair(price, tradingPair)
                    }
                    onComplete(ticker)
                } catch (e: JsonSyntaxException) {
                    onComplete(null)
                }
            }
        }
    }
    class orderLimit(val tradeSide: TradeSide, val productId: String, val price: Double, val size: Double, val timeInForce: TimeInForce?, val cancelAfter: String?) : CBProApi()
    class orderMarket(val tradeSide: TradeSide, val productId: String, val size: Double? = null, val funds: Double? = null) : CBProApi()
    class orderStop(val tradeSide: TradeSide, val productId: String, val price: Double, val size: Double? = null, val funds: Double? = null) : CBProApi()
    class cancelOrder(val orderId: String) : CBProApi()
    class cancelAllOrders : CBProApi()
    class listOrders(val status: String = "all", val productId: String?) : CBProApi() {
        fun getAndStash(context: Context, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (List<ApiOrder>) -> Unit) {
            this.executeRequest(onFailure) {result ->
                try {
                    val apiOrderList: List<ApiOrder> = Gson().fromJson(result.value, object : TypeToken<List<ApiOrder>>() {}.type)
                    Prefs(context).stashOrders(result.value)
                    onComplete(apiOrderList)
                } catch (e: JsonSyntaxException) {
                    onComplete(listOf())
                }
            }
        }
    }
    class getOrder(val orderId: String) : CBProApi()
    class fills(val orderId: String = "all", val productId: String = "all") : CBProApi() {
        fun getAndStash(context: Context, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (List<ApiFill>) -> Unit) {
            this.executeRequest(onFailure) {result ->
                try {
                    val apiFillList: List<ApiFill> = Gson().fromJson(result.value, object : TypeToken<List<ApiFill>>() {}.type)
                    Prefs(context).stashFills(result.value)
                    onComplete(apiFillList)
                } catch (e: JsonSyntaxException) {
                    onComplete(listOf())
                }
            }
        }
    }
    //add position?
    class sendCrypto(val amount: Double, val currency: Currency, val cryptoAddress: String) : CBProApi()
    class coinbaseAccounts : CBProApi() {
        fun get(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (List<ApiCoinbaseAccount>) -> Unit) {
            this.executeRequest(onFailure) {result ->
                try {
                    val apiCBAccountList: List<ApiCoinbaseAccount> = Gson().fromJson(result.value, object : TypeToken<List<ApiCoinbaseAccount>>() {}.type)
                    onComplete(apiCBAccountList)
                } catch (e: JsonSyntaxException) {
                    onComplete(listOf())
                }
            }
        }

        fun linkToAccounts(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
           this.get(onFailure) { coinbaseAccounts ->
                for (cbAccount in coinbaseAccounts) {
                    val currency = Currency.forString(cbAccount.currency)
                    if (currency != null && cbAccount.active) {
                        val account = Account.forCurrency(currency)
                        account?.coinbaseAccount = Account.CoinbaseAccount(cbAccount)
                    }
                }
                onComplete()
            }
        }
    }
    class paymentMethods : CBProApi() {
        fun get(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (List<Account.PaymentMethod>) -> Unit) {
            this.executeRequest(onFailure) {result ->
                try {
                    var apiPaymentMethodsList: List<ApiPaymentMethod> = Gson().fromJson(result.value, object : TypeToken<List<ApiPaymentMethod>>() {}.type)
                    apiPaymentMethodsList = apiPaymentMethodsList.filter { apiPaymentMethod -> apiPaymentMethod.type != "fiat_account" }
                    val paymentMethodsList = apiPaymentMethodsList.map { apiPaymentMethod -> Account.PaymentMethod(apiPaymentMethod) }
                    onComplete(paymentMethodsList)
                } catch (e: JsonSyntaxException) {
                    onComplete(listOf())
                }
            }
        }
    }
    class getFromCoinbase(val amount: Double, val currency: Currency, val accountId: String) : CBProApi()
    class getFromPayment(val amount: Double, val currency: Currency, val paymentMethodId: String) : CBProApi()
    class sendToCoinbase(val amount: Double, val currency: Currency, val accountId: String) : CBProApi()
    class sendToPayment(val amount: Double, val currency: Currency, val paymentMethodId: String) : CBProApi()
    class createReport(val type: String, val startDate: Date, val endDate: Date, val productId: String?, val accountId: String?) : CBProApi() {
        fun createAndGetInfo(onComplete: (Boolean) -> Unit) {
            this.executePost({ result ->
                println(result)
                onComplete(false)
            }, { reportInfo ->
                val byteArray = reportInfo.component1()
                val responseString = if (byteArray != null) {
                    String(byteArray)
                } else {
                    ""
                }
                try {
                    val apiReportInfo: ApiReportInfo = Gson().fromJson(responseString, object : TypeToken<ApiReportInfo>() {}.type)
                    CBProApi.getReport(apiReportInfo.id).executeRequest({ result ->
                        println(result)
                    }, { result ->
                        println(result.value)
                    })
                } catch (e: Exception) {
                    println("nah")
                }
            })
        }
    }
    class getReport(val reportId: String) : CBProApi()
    //add deposits
    //look into reports


    override val method: Method
        get() {
            return when (this) {
                is accounts -> Method.GET
                is account -> Method.GET
                is accountHistory -> Method.GET
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
                is sendCrypto -> Method.POST
                is coinbaseAccounts -> Method.GET
                is paymentMethods -> Method.GET
                is getFromCoinbase -> Method.POST
                is getFromPayment -> Method.POST
                is sendToCoinbase -> Method.POST
                is sendToPayment -> Method.POST
                is createReport -> Method.POST
                is getReport -> Method.GET
            }
        }

    override val path: String
        get() {
            return when (this) {
                is accounts -> "/accounts"
                is account -> "/accounts/$accountId"
                is accountHistory -> "/accounts/$accountId/ledger"
                is products -> "/products"
                is ticker -> "/products/${tradingPair.id}/ticker"
                is candles -> "/products/$productId/candles"
                is orderLimit -> "/orders"
                is orderMarket -> "/orders"
                is orderStop -> "/orders"
                is cancelOrder -> "/orders/$orderId"
                is cancelAllOrders -> "/orders"
                is listOrders -> "/orders"
                is getOrder -> "/orders/$orderId"
                is fills -> "/fills"
                is sendCrypto -> "/withdrawals/crypto"
                is coinbaseAccounts -> "/coinbase-accounts"
                is paymentMethods -> "/payment-methods"
                is getFromCoinbase -> "/deposits/coinbase-account"
                is getFromPayment -> "/deposits/payment-method"
                is sendToCoinbase -> "/withdrawals/coinbase-account"
                is sendToPayment -> "/withdrawals/payment-method"
                is createReport -> "/reports"
                is getReport -> "/reports/$reportId"
            }
        }

    override val params: List<Pair<String, Any?>>?
        get() {
            val paramList = mutableListOf<Pair<String, String>>()
            when (this) {
                is candles -> {
                    val utcTimeZone = TimeZone.getTimeZone("UTC")
                    val now = Calendar.getInstance(utcTimeZone)
                    val nowLong = now.timeInSeconds() - timeOffset
                    val startInt = nowLong - timespan

                    val start = Date(startInt * 1000)

                    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    formatter.timeZone = utcTimeZone

                    paramList.add(Pair("start", formatter.format(start)))
                    paramList.add(Pair("end", formatter.format(nowLong * 1000)))
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
                    val json = basicOrderParams(tradeSide, TradeType.LIMIT, productId)

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
                    val json = basicOrderParams(tradeSide, TradeType.MARKET, productId)

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
                    val json = basicOrderParams(tradeSide, TradeType.STOP, productId)

                    json.put("price", "$price")
                    if (funds != null) {
                        json.put("funds", "$funds")
                    }
                    if (size != null) {
                        json.put("size", "$size")
                    }

                    return json.toString()
                }
                is sendCrypto -> {
                    val json = JSONObject()
                    json.put("amount", amount.btcFormat())
                    json.put("currency", currency.toString())
                    json.put("crypto_address", cryptoAddress)
                    return json.toString()
                }
                is getFromCoinbase -> {
                    val json = JSONObject()

                    json.put("amount", amount.btcFormat())
                    json.put("currency", currency.toString())
                    json.put("coinbase_account_id", accountId)
                    return json.toString()
                }
                is getFromPayment -> {
                    val json = JSONObject()

                    json.put("amount", amount.btcFormat())
                    json.put("currency", currency.toString())
                    json.put("payment_method_id", paymentMethodId)
                    return json.toString()
                }
                is sendToCoinbase -> {
                    val json = JSONObject()
                    json.put("amount", amount.btcFormat())
                    json.put("currency", currency.toString())
                    json.put("coinbase_account_id", accountId)
                    return json.toString()
                }
                is sendToPayment -> {
                    val json = JSONObject()
                    json.put("amount", amount.btcFormat())
                    json.put("currency", currency.toString())
                    json.put("payment_method_id", paymentMethodId)
                    return json.toString()
                }
                is createReport -> {
                    val json = JSONObject()

                    val utcTimeZone = TimeZone.getTimeZone("UTC")
                    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    formatter.timeZone = utcTimeZone

                    json.put("type", type)
                    json.put("start_date", formatter.format(startDate))
                    json.put("end_date", formatter.format(endDate))
                    if (type == "fills") {
                        json.put("product_id", productId)
                    } else if (type == "account") {
                        json.put("account_id", accountId)
                    }
//                    json.put("currency", currency.toString())
//                    json.put("payment_method_id", paymentMethodId)
                    json.put("format", "csv")
                    return json.toString()
                }
                else -> return ""
            }
        }

    override val headers: Map<String, String>?
        get() {
            var headers: MutableMap<String, String> = mutableMapOf()
            val credentials = credentials
            if (credentials != null) {
                val timestamp = (Date().timeInSeconds()).toString()
                val message = timestamp + method + path + body
                println("timestamp:")
                println(timestamp)


                var hash = ""
                try {
                    val secretDecoded: ByteArray? = Base64.decode(credentials.apiSecret, 0)

                    val sha256HMAC = Mac.getInstance("HmacSHA256")
                    val secretKey = SecretKeySpec(secretDecoded, "HmacSHA256")
                    sha256HMAC.init(secretKey)
                    hash = Base64.encodeToString(sha256HMAC.doFinal(message.toByteArray()), 0)

                } catch (e: Exception) {
                    println("api secret error")
                }

                println("hash:")
                println(hash)

                headers = mutableMapOf(Pair("CB-ACCESS-KEY", credentials.apiKey), Pair("CB-ACCESS-PASSPHRASE", credentials.apiPassPhrase), Pair("CB-ACCESS-SIGN", hash), Pair("CB-ACCESS-TIMESTAMP", timestamp))

            }

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

    enum class ErrorMessage {
        //Log in:
        Forbidden,
        InvalidApiKey,
        InvalidPassphrase,
        InvalidApiSignature,
        MissingApiSignature,

        //Permissions:

        //Trade:
        BuyAmountTooSmallBtc,
        BuyAmountTooSmallEth,
        BuyAmountTooSmallBch,
        BuyAmountTooSmallLtc,

        BuyAmountTooLargeBtc,
        BuyAmountTooLargeEth,
        BuyAmountTooLargeBch,
        BuyAmountTooLargeLtc,

        PriceTooAccurate,
        InsufficientFunds,

        //Transfer:
        TransferAmountTooLow,
        InvalidCryptoAddress;

        override fun toString(): String {
            //TODO: refactor
            return when (this) {
                Forbidden -> "Forbidden"
                InvalidApiKey -> "Invalid API Key"
                InvalidPassphrase -> "Invalid Passphrase"
                InvalidApiSignature -> "invalid signature"
                MissingApiSignature -> "CB-ACCESS-SIGN header is required"

                BuyAmountTooSmallBtc -> "size is too small. Minimum size is 0.001"
                BuyAmountTooSmallEth -> "size is too small. Minimum size is 0.01"
                BuyAmountTooSmallBch -> "size is too small. Minimum size is 0.01"
                BuyAmountTooSmallLtc -> "size is too small. Minimum size is 0.1"
                BuyAmountTooLargeBtc -> "size is too large. Maximum size is 70"
                BuyAmountTooLargeEth -> "size is too large. Maximum size is 700"
                BuyAmountTooLargeBch -> "size is too large. Maximum size is 350"
                BuyAmountTooLargeLtc -> "size is too large. Maximum size is 4000"

                PriceTooAccurate  -> "price is too accurate. Smallest unit is 0.01000000"
                InsufficientFunds -> "Insufficient funds"

                TransferAmountTooLow -> "amount must be a positive number"
                InvalidCryptoAddress -> "invalid crypto_address"
            }
        }

        companion object {
            fun forString(string: String) : ErrorMessage? {
                val errorMessage = ErrorMessage.values().find { errorMessage -> errorMessage.toString() == string }
                if (errorMessage == null) {
                    if (string.contains("is below the minimum", true) && string.contains("required to send on-blockchain.", true)) {
                        return TransferAmountTooLow
                    }
                }
                return errorMessage
            }
        }
    }

}