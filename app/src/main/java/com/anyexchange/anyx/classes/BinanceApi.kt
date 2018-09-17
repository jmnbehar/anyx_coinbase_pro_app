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
sealed class BinanceApi(initData: ApiInitData?) : FuelRouting {
    val context = initData?.context
    val returnToLogin = initData?.returnToLogin ?:  { }

    class ApiCredentials(val apiKey: String, val apiSecret: String, val apiPassPhrase: String, var isVerified: Boolean?)

    companion object {
        var credentials: ApiCredentials? = null

        const val basePath = "https://api.binance.com"

        fun defaultPostFailure(context: Context?, result: Result.Failure<ByteArray, FuelError>) : String {
            val errorCode = ErrorCode.withCode(result.error.response.statusCode)
            return if (context!= null) {
                when (errorCode) {
                    ErrorCode.BadRequest -> context.resources.getString(R.string.error_400)
                    ErrorCode.Unauthorized -> context.resources.getString(R.string.error_401)
                    ErrorCode.Forbidden -> context.resources.getString(R.string.error_403)
                    ErrorCode.NotFound -> context.resources.getString(R.string.error_404)
                    ErrorCode.TooManyRequests -> context.resources.getString(R.string.error_too_many)
                    ErrorCode.ServerError -> context.resources.getString(R.string.error_server)
                    ErrorCode.UnknownError -> context.resources.getString(R.string.error_generic_message, result.errorMessage)
                    else -> ""
                }
            } else {
                ""
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

    class candles(private val initData: ApiInitData?, val productId: String, val interval: String, var startTime: Long? = null, var endTime: Long? = null, var limit: Int? = null) : BinanceApi(initData) {
        //limit default = 500, max is 1000
        fun getCandles(onFailure: (Result.Failure<String, FuelError>) -> Unit, onComplete: (List<Candle>) -> Unit) {
            var pagesReceived = 0

            val timespan = if (endTime != null && startTime != null) {
                endTime!! - startTime!!
            } else {
                0
            }

            var allCandles = mutableListOf<Candle>()

            this.executeRequest(onFailure) { result ->
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
                    onComplete(allCandles.sorted())
                } catch (exception: Exception) {
                    onFailure(Result.Failure(FuelError(exception)))
                }
            }
        }
    }

    class accounts(private val initData: ApiInitData?) : BinanceApi(initData) {

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

                BinanceApi.products(initData).get(onFailure) { apiProductList ->
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

    class account(initData: ApiInitData?, val accountId: String) : BinanceApi(initData) {
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

    class accountHistory(initData: ApiInitData?, val accountId: String) : BinanceApi(initData)
    class products(initData: ApiInitData?) : BinanceApi(initData) {
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
    class ticker(initData: ApiInitData?, accountId: String) : BinanceApi(initData) {
        val tradingPair = TradingPair(accountId)
        constructor(initData: ApiInitData?, tradingPair: TradingPair): this(initData, tradingPair.id)
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
    class orderLimit(initData: ApiInitData?, val productId: String, val tradeSide: TradeSide, val timeInForce: TimeInForce?, val quantity: String, val price: Double, val icebergQty: Double) : BinanceApi(initData)
    class orderMarket(initData: ApiInitData?, val productId: String, val tradeSide: TradeSide, val quantity: Double? = null, val price: Double, val funds: Double? = null) : BinanceApi(initData)
    class orderStop(initData: ApiInitData?, val productId: String, val tradeSide: TradeSide, val timeInForce: TimeInForce?, val quantity: String, val price: Double, val stopPrice: Double? = null) : BinanceApi(initData)
    class cancelAllOrders(initData: ApiInitData) : BinanceApi(initData)
    class listOrders(initData: ApiInitData?, val productId: String? = null) : BinanceApi(initData) {
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
    class getOrder(initData: ApiInitData?, val productId: String, val orderId: String) : BinanceApi(initData)
    class cancelOrder(initData: ApiInitData?, val productId: String, val orderId: String) : BinanceApi(initData)

    class fills(initData: ApiInitData?, val productId: String) : BinanceApi(initData) {
        fun getAndStash(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (List<ApiFill>) -> Unit) {
            this.executeRequest(onFailure) {result ->
                context?.let { context ->
                    try {
                        val prefs = Prefs(context)
                        val apiFillList: List<ApiFill> = Gson().fromJson(result.value, object : TypeToken<List<ApiFill>>() {}.type)
                        if (prefs.areAlertFillsActive) {
                            checkForFillAlerts(apiFillList, productId)
                        }
                        prefs.stashFills(result.value, productId)
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
    class depositAddress(initData: ApiInitData?, val cbAccountId: String) : BinanceApi(initData) {
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

    class sendCrypto(initData: ApiInitData?, val amount: Double, val currency: Currency, val cryptoAddress: String) : BinanceApi(initData)
    class coinbaseAccounts(initData: ApiInitData?) : BinanceApi(initData) {
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
    class paymentMethods(initData: ApiInitData?) : BinanceApi(initData) {
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
    class getFromCoinbase(initData: ApiInitData?, val amount: Double, val currency: Currency, val accountId: String) : BinanceApi(initData)
    class getFromPayment(initData: ApiInitData?, val amount: Double, val currency: Currency, val paymentMethodId: String) : BinanceApi(initData)
    class sendToCoinbase(initData: ApiInitData?, val amount: Double, val currency: Currency, val accountId: String) : BinanceApi(initData)
    class sendToPayment(initData: ApiInitData?, val amount: Double, val currency: Currency, val paymentMethodId: String) : BinanceApi(initData)
    class ping(initData: ApiInitData?) : BinanceApi(initData)
    class time(initData: ApiInitData?) : BinanceApi(initData)

    class exchangeInfo(initData: ApiInitData?) : BinanceApi(initData)
    class orderBookDepth(initData: ApiInitData?, symbol: String, limit: Int?) : BinanceApi(initData)
    class recentTrades(initData: ApiInitData?, symbol: String, limit: Int?) : BinanceApi(initData)
    class historicalTrades(initData: ApiInitData?, symbol: String, limit: Int?, fromTradeId: Long) : BinanceApi(initData)
    class aggregatedTrades(initData: ApiInitData?) : BinanceApi(initData)
    class dayChangeStats(initData: ApiInitData?, productId: String) : BinanceApi(initData)
    class bookTicker(initData: ApiInitData?, productId: String?) : BinanceApi(initData)
    class allOrders(initData: ApiInitData?, productId: String?) : BinanceApi(initData)
    class o7(initData: ApiInitData?) : BinanceApi(initData)
    class o8(initData: ApiInitData?) : BinanceApi(initData)
    class o9(initData: ApiInitData?) : BinanceApi(initData)
    class o10(initData: ApiInitData?) : BinanceApi(initData)


    override val method: Method
        get() {
            return when (this) {
                is accounts -> Method.GET
                is account -> Method.GET
                is accountHistory -> Method.GET
                is products -> Method.GET
                is ticker -> Method.GET
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
                is ping -> Method.GET
                is time -> Method.GET

                is candles -> Method.GET
                is exchangeInfo -> Method.GET
                is orderBookDepth -> Method.GET
                is recentTrades -> Method.GET
                is historicalTrades -> Method.GET
                is aggregatedTrades -> Method.GET
                is dayChangeStats -> Method.GET
                is bookTicker -> Method.GET
                is allOrders -> Method.GET
                is o7 -> Method.GET
                is o8 -> Method.GET
                is o9 -> Method.GET
                is o10 -> Method.GET
            }
        }

    override val path: String
        get() {
            var tempPath = "/api/"
            tempPath += when (this) {
                is accountHistory -> "/accounts/$accountId/ledger"
                is products -> "/products"
                is cancelOrder -> "/orders/$orderId"
                is cancelAllOrders -> "/orders"
                is sendCrypto -> "/withdrawals/crypto"
                is depositAddress -> "/coinbase-accounts/$cbAccountId/addresses"
                is coinbaseAccounts -> "/coinbase-accounts"
                is paymentMethods -> "/payment-methods"
                is getFromCoinbase -> "/deposits/coinbase-account"
                is getFromPayment -> "/deposits/payment-method"
                is sendToCoinbase -> "/withdrawals/coinbase-account"
                is sendToPayment -> "/withdrawals/payment-method"
                is ping -> "/ping"
                is time -> "/time"


                is candles -> "v1/klines"
                is exchangeInfo -> "v1/exchangeInfo"
                is orderBookDepth -> "v1/depth"
                is recentTrades -> "v1/trades"
                is historicalTrades -> "v1/historicalTrades"
                is aggregatedTrades -> "v1/aggTrades"
                is dayChangeStats -> "v3/ticker/24hr"
                is ticker -> "v3/ticker/price"
                is bookTicker -> "v3/ticker/bookTicker"

                //TODO: take off the test
                is orderLimit -> "v3/order/test"
                is orderMarket -> "v3/order/test"
                is orderStop -> "v3/order/test"
                is getOrder -> "v3/order"
                is listOrders -> "v3/openOrders"
                is accounts -> "v3/account"
                is fills -> "v3/myTrades"


                is account -> "/accounts/$accountId"


                is allOrders -> "v3/allOrders"
                is o7 -> ""
                is o8 -> ""
                is o9 -> ""
                is o10 -> ""
            }
            return tempPath
        }

    override val params: List<Pair<String, Any?>>?
        get() {
            val paramList = mutableListOf<Pair<String, String>>()
            when (this) {
                is candles -> {
                    paramList.add(Pair("symbol", productId))
                    paramList.add(Pair("interval", interval))
                    if (startTime != null) {
                        paramList.add(Pair("startTime", startTime.toString()))
                    }
                    if (endTime != null) {
                        paramList.add(Pair("endTime", endTime.toString()))
                    }
                    if (limit != null) {
                        paramList.add(Pair("limit", limit.toString()))
                    }
                    return paramList.toList()
                }
                is fills -> {
                    paramList.add(Pair("product_id", productId))

                    return paramList.toList()
                }
                is listOrders -> {
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

                    return json.toString()
                }
                is orderMarket -> {
                    //can add either size or funds, for now lets do funds
                    val json = basicOrderParams(tradeSide, TradeType.MARKET, productId)

                    if (funds != null) {
                        json.put("funds", "$funds")
                    }
                    return json.toString()
                }
                is orderStop -> {
                    //can add either size or funds, for now lets do funds
                    val json = basicOrderParams(tradeSide, TradeType.STOP, productId)

                    json.put("price", "$price")
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