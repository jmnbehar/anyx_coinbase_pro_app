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
sealed class CBProApi(initData: CBProApiInitData?) : FuelRouting {
    val context = initData?.context
    val returnToLogin = initData?.returnToLogin ?:  { }

    class ApiCredentials(val apiKey: String, val apiSecret: String, val apiPassPhrase: String, var isVerified: Boolean?)

    companion object {
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

    class CBProApiInitData(val context: Context, val returnToLogin: () -> Unit)


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
                        ErrorCode.Unauthorized.code, ErrorCode.BadRequest.code -> {
                            when (result.errorMessage) {
                                ErrorMessage.InvalidApiKey.toString(), ErrorMessage.InvalidApiKey.toString() -> {
                                    credentials = null
                                    if (context != null) {
                                        Prefs(context).isLoggedIn = false
                                    }
                                    returnToLogin()
                                }
                            }
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

    class candles(private val initData: CBProApiInitData?, val productId: String, val timespan: Long = Timespan.DAY.value(), var granularity: Long, var timeOffset: Long) : CBProApi(initData) {
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

                CBProApi.candles(initData, productId, currentTimespan, granularity, coveredTimespan).executeRequest(onFailure) { result ->
                    pagesReceived ++
                    val gson = Gson()
                    val apiCandles = result.value
                    val tradingPair = TradingPair(productId)
                    try {
                        val candleDoubleList: List<List<Double>> = gson.fromJson(apiCandles, object : TypeToken<List<List<Double>>>() {}.type)
                        var candles = candleDoubleList.mapNotNull {
                            val time = (it[0] as? Double)
                            val low = (it[1] as? Double) ?: 0.0
                            val high = (it[2] as? Double) ?: 0.0
                            val open = (it[3] as? Double) ?: 0.0
                            val close = (it[4] as? Double)
                            val volume = (it[5] as? Double) ?: 0.0
                            if (close != null && time != null) {
                                Candle(time, low, high, open, close, volume, tradingPair)
                            } else { null }
                        }
                        val now = Calendar.getInstance()

                        val start = now.timeInSeconds() - timespan - 30

                        candles = candles.filter { it.time >= start }

                        //TODO: edit chart library so it doesn't show below 0
                        candles = candles.reversed()
                        allCandles = allCandles.addingCandles(candles)
                    } catch (exception: Exception) {
                        onFailure(Result.Failure(FuelError(exception)))
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

    class accounts(private val initData: CBProApiInitData?) : CBProApi(initData) {

        fun get(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (List<ApiAccount>) -> Unit) {
            this.executeRequest(onFailure) { result ->
                try {
                    val apiAccountList: List<ApiAccount> = Gson().fromJson(result.value, object : TypeToken<List<ApiAccount>>() {}.type)
                    onComplete(apiAccountList)
                } catch (e: Exception) {
                    onFailure(Result.Failure(FuelError(e)))
                }
            }
        }

        fun getAllAccountInfo(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
            Account.cryptoAccounts = listOf()
            val productList: MutableList<Product> = mutableListOf()
            if (!Account.areAccountsOutOfDate && context != null && Prefs(context).stashedProducts.isNotEmpty()) {
                val stashedProductList = Prefs(context).stashedProducts
                getAccountsWithProductList(stashedProductList, onFailure, onComplete)
            } else {

                CBProApi.products(initData).get(onFailure) { apiProductList ->
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
                val fiatCurrency = Account.defaultFiatCurrency
                val filteredProductList = productList.filter { it.quoteCurrency == fiatCurrency }
                val fiatAccount = ApiAccount("", fiatCurrency.toString(), "0.0", "", "0.0", "")
                Account.fiatAccounts = listOf(Account(Product.fiatProduct(fiatCurrency), fiatAccount))
                val tempCryptoAccounts = filteredProductList.map {
                    val apiAccount = ApiAccount("", it.currency.toString(), "0.0", "", "0.0", "")
                    Account(it, apiAccount)
                }
                Account.cryptoAccounts = tempCryptoAccounts
                Account.updateAllAccountsCandles(initData, onFailure, onComplete)
            } else {
                this.get(onFailure) { apiAccountList ->
                    val fiatApiAccountList = apiAccountList.filter { Currency.forString(it.currency)?.isFiat == true }
                    val tempFiatAccounts = fiatApiAccountList.map { Account(Product.fiatProduct( Currency.forString(it.currency) ?: Currency.USD), it) }

                    val cryptoApiAccountList = apiAccountList.filter { Currency.forString(it.currency)?.isFiat != true }
                    val defaultFiatCurrency = Account.defaultFiatCurrency
                    val tempCryptoAccounts = cryptoApiAccountList.mapNotNull {
                        val currency = Currency.forString(it.currency)
                        val relevantProduct = productList.find { p -> p.currency == currency && p.quoteCurrency == defaultFiatCurrency }
                        if (relevantProduct != null) {
                            Account(relevantProduct, it)
                        } else {
                            null
                        }
                    }
                    Account.cryptoAccounts = tempCryptoAccounts
                    Account.fiatAccounts = tempFiatAccounts.sortedWith(compareBy({ it.defaultValue }, { it.currency.orderValue })).reversed()

                    Account.updateAllAccountsCandles(initData, onFailure, onComplete)
                }
            }
        }

        fun updateAllAccounts(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
            this.get(onFailure) { apiAccountList ->
                for (account in Account.cryptoAccounts.plus(Account.fiatAccounts)) {
                    val apiAccount = apiAccountList.find { a -> a.currency == account.currency.toString() }
                    apiAccount?.let {
                        account.apiAccount = it
                    }
                }
                onComplete()
            }
        }

    }

    class account(initData: CBProApiInitData?, val accountId: String) : CBProApi(initData) {
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
                        val apiAccountFirst = apiAccountList.firstOrNull()
                        if (apiAccountFirst != null) {
                            onComplete(apiAccountList.firstOrNull())
                        } else {
                            onFailure(Result.Failure(FuelError(Exception())))
                        }
                    } catch (e: JsonSyntaxException) {
                        onFailure(Result.Failure(FuelError(Exception())))
                    }
                }
            }
        }
    }

    class accountHistory(initData: CBProApiInitData?, val accountId: String) : CBProApi(initData)
    class products(initData: CBProApiInitData?) : CBProApi(initData) {
        fun get(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (List<ApiProduct>) -> Unit) {
            this.executeRequest(onFailure) { result ->
                try {
                    val productList: List<ApiProduct> = Gson().fromJson(result.value, object : TypeToken<List<ApiProduct>>() {}.type)
                    onComplete(productList)
                } catch (e: JsonSyntaxException) {
                    onFailure(Result.Failure(FuelError(e)))
                } catch (e: IllegalStateException) {
                    onFailure(Result.Failure(FuelError(e)))
                }
            }
        }
    }
    class ticker(initData: CBProApiInitData?, accountId: String) : CBProApi(initData) {
        val tradingPair = TradingPair(accountId)
        constructor(initData: CBProApiInitData?, tradingPair: TradingPair): this(initData, tradingPair.id)
        fun get(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (ApiTicker) -> Unit) {
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
                    onFailure(Result.Failure(FuelError(e)))
                } catch (e: IllegalStateException) {
                    onFailure(Result.Failure(FuelError(e)))
                }
            }
        }
    }
    class orderLimit(initData: CBProApiInitData?, val tradeSide: TradeSide, val productId: String, val price: Double, val size: Double, val timeInForce: TimeInForce?, val cancelAfter: String?) : CBProApi(initData)
    class orderMarket(initData: CBProApiInitData?, val tradeSide: TradeSide, val productId: String, val size: Double? = null, val funds: Double? = null) : CBProApi(initData)
    class orderStop(initData: CBProApiInitData?, val tradeSide: TradeSide, val productId: String, val price: Double, val size: Double? = null, val funds: Double? = null) : CBProApi(initData)
    class cancelOrder(initData: CBProApiInitData?, val orderId: String) : CBProApi(initData)
    class cancelAllOrders(initData: CBProApiInitData) : CBProApi(initData)
    class listOrders(initData: CBProApiInitData?, val status: String? = null) : CBProApi(initData) {
        //For now don't use product ID, always get ALL orders
        val productId: String? = null
        fun getAndStash(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (List<ApiOrder>) -> Unit) {
            this.executeRequest(onFailure) {result ->
                try {
                    val apiOrderList: List<ApiOrder> = Gson().fromJson(result.value, object : TypeToken<List<ApiOrder>>() {}.type)
                    if (context != null) {
                        Prefs(context).stashOrders(result.value)
                    }
                    onComplete(apiOrderList)
                } catch (e: JsonSyntaxException) {
                    onFailure(Result.Failure(FuelError(Exception())))
                }
            }
        }
    }
    class getOrder(initData: CBProApiInitData?, val orderId: String) : CBProApi(initData)
    class fills(initData: CBProApiInitData?, val orderId: String? = null, val productId: String? = null) : CBProApi(initData) {
        fun getAndStash(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (List<ApiFill>) -> Unit) {
            this.executeRequest(onFailure) {result ->
                context?.let { context ->
                    try {
                        val prefs = Prefs(context)
                        val apiFillList: List<ApiFill> = Gson().fromJson(result.value, object : TypeToken<List<ApiFill>>() {}.type)
                        if (productId != null) {
                            if (prefs.areAlertFillsActive) {
                                checkForFillAlerts(apiFillList, productId)
                            }
                            prefs.stashFills(result.value, productId)
                        }
                        onComplete(apiFillList)
                    } catch (e: JsonSyntaxException) {
                        onFailure(Result.Failure(FuelError(e)))
                    }
                } ?: run {
                    onFailure(Result.Failure(FuelError(Exception())))
                }
            }
        }
        private fun checkForFillAlerts(apiFillList: List<ApiFill>, productId: String) {
            context?.let {
                val stashedFills = Prefs(context).getStashedFills(productId)

                if (apiFillList.size > stashedFills.size) {
                    val stashedFillsDate = stashedFills.firstOrNull()?.created_at?.dateFromApiDateString()?.time ?: 0
                    for (fill in apiFillList) {
                        val fillDate = fill.created_at.dateFromApiDateString()
                        val fillDateTime = fillDate?.time ?: 0
                        if (fillDateTime > stashedFillsDate && fillDateTime + TimeInMillis.sixHours > Date().time) {
                            AlertHub.triggerFillAlert(fill, context)
                        } else if (fillDateTime <= stashedFillsDate) {
                            break
                        }
                    }
                }
            }
        }
    }
    //add position?
    class depositAddress(initData: CBProApiInitData?, val cbAccountId: String) : CBProApi(initData) {
        fun get(onFailure: (result: Result.Failure<ByteArray, FuelError>) -> Unit, onComplete: (ApiDepositAddress) -> Unit) {
            this.executePost(onFailure) {
                val byteArray = it.component1()
                try {
                    if (byteArray != null) {
                        val apiDepositAddress: ApiDepositAddress = Gson().fromJson(String(byteArray), object : TypeToken<ApiDepositAddress>() {}.type)
                        onComplete(apiDepositAddress)
                    } else {
                        onFailure(Result.Failure(FuelError(Exception())))
                    }
                } catch (e: Exception) {
                    onFailure(Result.Failure(FuelError(e)))
                }
            }
        }
    }

    class sendCrypto(initData: CBProApiInitData?, val amount: Double, val currency: Currency, val cryptoAddress: String) : CBProApi(initData)
    class coinbaseAccounts(initData: CBProApiInitData?) : CBProApi(initData) {
        fun get(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (List<ApiCoinbaseAccount>) -> Unit) {
            this.executeRequest(onFailure) {result ->
                try {
                    val apiCBAccountList: List<ApiCoinbaseAccount> = Gson().fromJson(result.value, object : TypeToken<List<ApiCoinbaseAccount>>() {}.type)
                    onComplete(apiCBAccountList)
                } catch (e: JsonSyntaxException) {
                    onFailure(Result.Failure(FuelError(e)))
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
    class paymentMethods(initData: CBProApiInitData?) : CBProApi(initData) {
        fun get(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (List<Account.PaymentMethod>) -> Unit) {
            this.executeRequest(onFailure) {result ->
                try {
                    var apiPaymentMethodsList: List<ApiPaymentMethod> = Gson().fromJson(result.value, object : TypeToken<List<ApiPaymentMethod>>() {}.type)
                    apiPaymentMethodsList = apiPaymentMethodsList.filter { apiPaymentMethod -> apiPaymentMethod.type != "fiat_account" }
                    val paymentMethodsList = apiPaymentMethodsList.map { apiPaymentMethod -> Account.PaymentMethod(apiPaymentMethod) }
                    onComplete(paymentMethodsList)
                } catch (e: JsonSyntaxException) {
                    onFailure(Result.Failure(FuelError(e)))
                }
            }
        }
    }
    class getFromCoinbase(initData: CBProApiInitData?, val amount: Double, val currency: Currency, val accountId: String) : CBProApi(initData)
    class getFromPayment(initData: CBProApiInitData?, val amount: Double, val currency: Currency, val paymentMethodId: String) : CBProApi(initData)
    class sendToCoinbase(initData: CBProApiInitData?, val amount: Double, val currency: Currency, val accountId: String) : CBProApi(initData)
    class sendToPayment(initData: CBProApiInitData?, val amount: Double, val currency: Currency, val paymentMethodId: String) : CBProApi(initData)
    class createReport(private val initData: CBProApiInitData, val type: String, val startDate: Date, val endDate: Date, val productId: String?, val accountId: String?) : CBProApi(initData) {
        fun createAndGetInfo(onComplete: (Boolean) -> Unit) {
            this.executePost({ onComplete(false) },
                    { reportInfo -> //OnSuccess
                val byteArray = reportInfo.component1()
                val responseString = if (byteArray != null) {
                    String(byteArray)
                } else {
                    ""
                }
                try {
                    val apiReportInfo: ApiReportInfo = Gson().fromJson(responseString, object : TypeToken<ApiReportInfo>() {}.type)
                    CBProApi.getReport(initData, apiReportInfo.id).executeRequest({ result ->
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
    class getReport(initData: CBProApiInitData?, val reportId: String) : CBProApi(initData)
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
                is depositAddress -> Method.POST
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
                is depositAddress -> "/coinbase-accounts/$cbAccountId/addresses"
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
                    if (orderId != null) {
                        paramList.add(Pair("order_id", orderId))
                    }
                    if (productId != null) {
                        paramList.add(Pair("product_id", productId))
                    }
                    return paramList.toList()
                }
                is listOrders -> {
                    if (status != null) {
                        paramList.add(Pair("status", status))
                    }
                    if (productId != null) {
                        paramList.add(Pair("product_id", productId))
                    }
                    return paramList.toList()
                }
                else -> return null
            }
        }
    private val fullPath: String
        get() {
            params?.let { params ->
                return if (params.isEmpty()) {
                    path
                } else {
                    var fullPath = "$path?"
                    for ((index, param) in params.withIndex()) {
                        if (index > 0) {
                            fullPath += "&"
                        }
                        fullPath += "${param.first}=${param.second}"
                    }
                    fullPath
                }
            } ?: run {
                return path
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
                    json.put("amount", amount.btcFormatShortened())
                    json.put("currency", currency.toString())
                    json.put("crypto_address", cryptoAddress)
                    return json.toString()
                }
                is getFromCoinbase -> {
                    val json = JSONObject()

                    json.put("amount", amount.btcFormatShortened())
                    json.put("currency", currency.toString())
                    json.put("coinbase_account_id", accountId)
                    return json.toString()
                }
                is getFromPayment -> {
                    val json = JSONObject()

                    json.put("amount", amount.btcFormatShortened())
                    json.put("currency", currency.toString())
                    json.put("payment_method_id", paymentMethodId)
                    return json.toString()
                }
                is sendToCoinbase -> {
                    val json = JSONObject()
                    json.put("amount", amount.btcFormatShortened())
                    json.put("currency", currency.toString())
                    json.put("coinbase_account_id", accountId)
                    return json.toString()
                }
                is sendToPayment -> {
                    val json = JSONObject()
                    json.put("amount", amount.btcFormatShortened())
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
                val message = if (this is fills || this is listOrders) {
                    timestamp + method + fullPath + body
                } else {
                    timestamp + method + path + body
                }
                var hash = ""
                try {
                    val secretDecoded: ByteArray? = Base64.decode(credentials.apiSecret, 0)

                    val sha256HMAC = Mac.getInstance("HmacSHA256")
                    val secretKey = SecretKeySpec(secretDecoded, "HmacSHA256")
                    sha256HMAC.init(secretKey)
                    hash = Base64.encodeToString(sha256HMAC.doFinal(message.toByteArray()), 0)

                } catch (e: Exception) {
                    println("API Secret Hashing Error")
                }

                headers = mutableMapOf(Pair("CB-ACCESS-KEY", credentials.apiKey), Pair("CB-ACCESS-PASSPHRASE", credentials.apiPassPhrase), Pair("CB-ACCESS-SIGN", hash), Pair("CB-ACCESS-TIMESTAMP", timestamp))

            }

            if (method == Method.POST) {
                headers["Content-Type"] = "application/json"
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
        MissingApiKey,

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
                MissingApiKey -> "CB-ACCESS-KEY header is required"

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