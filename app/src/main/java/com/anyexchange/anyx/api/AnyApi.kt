package com.anyexchange.anyx.api

import android.content.Context
import com.anyexchange.anyx.classes.*
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import java.util.*
import com.anyexchange.anyx.classes.Currency
import se.simbio.encryption.Encryption

class AnyApi(val apiInitData: ApiInitData?) {
    companion object {
        val defaultFailure: Result.Failure<String, FuelError> = Result.Failure(FuelError(Exception()))

        fun isExchangeLoggedIn(exchange: Exchange): Boolean {
            return exchange.isLoggedIn()
        }

        fun restoreCredentials(prefs: Prefs) {
            if ( CBProApi.credentials == null || CBProApi.credentials?.apiKey?.isEmpty() == true) {
                val apiKey = prefs.cbProApiKey
                val apiSecret = prefs.cbProApiSecret
                val passphraseEncrypted = prefs.cbProPassphrase
                val iv = ByteArray(16)
                val encryption = Encryption.getDefault(apiKey, apiSecret + Constants.salt, iv)
                val passphrase = encryption.decryptOrNull(passphraseEncrypted)
                if ((apiKey != null) && (apiSecret != null) && (passphrase != null)) {
                    val isApiKeyValid = prefs.isApiKeyValid(apiKey)
                    CBProApi.credentials = CBProApi.ApiCredentials(apiKey, apiSecret, passphrase, isApiKeyValid)
                }
            }
            if (prefs.isAnyXProActive) {
                if (BinanceApi.credentials == null || BinanceApi.credentials?.apiKey?.isEmpty() == true) {
                    val apiKey = prefs.binanceApiKey
                    val apiSecret = prefs.binanceApiSecret

                    if (apiKey != null && apiSecret != null) {
                        BinanceApi.credentials = BinanceApi.ApiCredentials(apiKey, apiSecret)
                    }
                }
            }
        }
    }

    fun reloadAllProducts(context: Context?, onFailure: (Result.Failure<String, FuelError>) -> Unit, onSuccess: () -> Unit) {
        getAllProducts(onFailure) {
            getAllAccounts(onFailure, {
                onSuccess()
                context?.let {
                    Prefs(it).stashProducts()
                }
            })
        }
    }

    fun getAndStashOrderList(exchange: Exchange, currency: Currency?, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (List<Order>) -> Unit) {
        if (exchange.isLoggedIn()) {
            when (exchange) {
                Exchange.CBPro -> {
                    CBProApi.listOrders(apiInitData, currency).getAndStash(onFailure, onSuccess)
                }
                Exchange.Binance -> {
                    BinanceApi.listOrders(apiInitData, currency).getAndStash(onFailure, onSuccess)
                }
            }
        }
    }

    fun getAndStashFillList(exchange: Exchange, tradingPair: TradingPair?, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (List<Fill>) -> Unit) {
        if (exchange.isLoggedIn()) {
            when (exchange) {
                Exchange.CBPro -> {
                    CBProApi.fills(apiInitData, tradingPair).getAndStash(onFailure, onSuccess)
                }
                Exchange.Binance -> {
                    if (tradingPair != null) {
                        BinanceApi.fills(apiInitData, tradingPair, null, null, null).getAndStash(onFailure, onSuccess)
                    } else {
                        onFailure(Result.Failure(FuelError(Exception())))
                    }
                }
            }
        }
    }

    fun cancelOrder(order: Order, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (result: Result.Success<String, FuelError>) -> Unit) {
        when (order.exchange) {
            Exchange.CBPro -> {
                CBProApi.cancelOrder(apiInitData, order.id).executeRequest(onFailure, onSuccess)
            }
            Exchange.Binance -> {
                BinanceApi.cancelOrder(apiInitData, order.tradingPair.idForExchange(Exchange.Binance), order.id.toLongOrNull()
                        ?: 0).executeRequest(onFailure, onSuccess)
            }
        }
    }

    fun ticker(tradingPair: TradingPair, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (Double?) -> Unit) {
        when (tradingPair.exchange) {
            Exchange.CBPro -> {
                CBProApi.ticker(apiInitData, tradingPair).get(onFailure) {
                    onSuccess(it.price?.toDoubleOrNull())
                }
            }
            Exchange.Binance -> {
                BinanceApi.ticker(apiInitData, tradingPair).get(onFailure) {
                    onSuccess(it?.price)
                }
            }
        }
    }
    fun updateAllTickers(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: () -> Unit) {
        var hasCbProCompleted = false
        var hasBinanceCompleted = false

        //CBPro:
        val cbProProducts = Product.map.values.filter { product -> product.tradingPairs.any { it.exchange == Exchange.CBPro } }
        var completedProducts = 0
        for (product in cbProProducts) {
            product.defaultTradingPair?.let { tradingPair ->
                CBProApi.ticker(apiInitData, tradingPair).get(onFailure) {
                    completedProducts++
                    if (completedProducts >= cbProProducts.size) {
                        hasCbProCompleted = true
                        if (hasBinanceCompleted || !isBinanceActive) {
                            onSuccess()
                        }
                    }
                }
            }
        }

        if (isBinanceActive) {
            //Binance:
            BinanceApi.ticker(apiInitData, null).get(onFailure) {
                hasBinanceCompleted = true
                if (hasCbProCompleted) {
                    onSuccess()
                }
            }
        }
    }

    fun updateAccount(account: Account, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (Account) -> Unit) {
        when (account.exchange) {
            Exchange.CBPro -> {
                CBProApi.account(apiInitData, account.id).get(onFailure) {cbProAccount ->
                    if (cbProAccount != null) {
                        account.updateWithApiAccount(cbProAccount)
                    }
                    onSuccess(account)
                }
            }
            Exchange.Binance -> {
                BinanceApi.accounts(apiInitData).getAndLink(onFailure) {
                    onSuccess(account)
                }
            }
        }
    }

    fun getCandles(exchange: Exchange, tradingPair: TradingPair, timespan: Long, timeOffset: Long, granularity: Long, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (List<Candle>) -> Unit) {
        when (exchange) {
            Exchange.CBPro -> {
                CBProApi.candles(apiInitData, tradingPair, timespan, granularity, 0).getCandles(onFailure, onSuccess)
            }
            Exchange.Binance -> {
                //TODO: fix interval here, or extrapolate for timespan for cbpro also
                val interval = when (granularity) {
                    TimeInSeconds.oneMinute -> BinanceApi.Interval.OneMinute
                    TimeInSeconds.fiveMinutes -> BinanceApi.Interval.FiveMinutes
                    TimeInSeconds.oneHour -> BinanceApi.Interval.OneHour
                    TimeInSeconds.sixHours -> BinanceApi.Interval.SixHours
                    TimeInSeconds.oneDay -> BinanceApi.Interval.OneDay
                    else -> BinanceApi.Interval.OneHour
                }
                val now = Date().time
                val startTime = now - ((timespan * 1000) + timeOffset)
                val endTime = now - timeOffset
                BinanceApi.candles(apiInitData, tradingPair, interval, startTime, endTime).getCandles(onFailure, onSuccess)
            }
        }
    }

    fun orderLimit(exchange: Exchange, tradeSide: TradeSide, tradingPair: TradingPair, limitPrice: Double, amount: Double, timeInForce: TimeInForce?,
                   cancelAfter: TimeInForce.CancelAfter?, icebergQty: Double?, onFailure: (result: Result.Failure<ByteArray, FuelError>) -> Unit, onSuccess: (Result<ByteArray, FuelError>) -> Unit) {
        when (exchange) {
            Exchange.CBPro -> {
                CBProApi.orderLimit(apiInitData, tradeSide, tradingPair.idForExchange(Exchange.CBPro), limitPrice, amount, timeInForce = timeInForce, cancelAfter = cancelAfter.toString()).executePost({ onFailure(it) }, { onSuccess(it) })
            }
            Exchange.Binance -> {
                val binanceTimeInForce = BinanceApi.TimeInForce.forString(timeInForce.toString())
                BinanceApi.orderLimit(apiInitData, tradingPair.idForExchange(Exchange.Binance), tradeSide, binanceTimeInForce, amount, limitPrice, icebergQty).executePost({ onFailure(it) }, { onSuccess(it) })

            }
        }
    }
    fun orderMarket(exchange: Exchange, tradeSide: TradeSide, tradingPair: TradingPair, amount: Double?, funds: Double?,
                    onFailure: (result: Result.Failure<ByteArray, FuelError>) -> Unit, onSuccess: (Result<ByteArray, FuelError>) -> Unit) {
        when (exchange) {
            Exchange.CBPro -> {
                CBProApi.orderMarket(apiInitData, tradeSide, tradingPair.idForExchange(Exchange.CBPro), size = amount, funds = funds).executePost({ onFailure(it) }, { onSuccess(it) })
            }
            Exchange.Binance -> {
                BinanceApi.orderMarket(apiInitData, tradingPair.idForExchange(Exchange.Binance), tradeSide, amount!!, amount).executePost({ onFailure(it) }, { onSuccess(it) })
            }
        }
    }
    fun orderStop(exchange: Exchange, tradeSide: TradeSide, tradingPair: TradingPair, stopPrice: Double, amount: Double, timeInForce: String?,
                  onFailure: (result: Result.Failure<ByteArray, FuelError>) -> Unit, onSuccess: (Result<ByteArray, FuelError>) -> Unit) {

        when (exchange) {
            Exchange.CBPro -> {
                when (tradeSide) {
                    TradeSide.BUY ->  CBProApi.orderStop(apiInitData, tradeSide, tradingPair.idForExchange(Exchange.CBPro), stopPrice, size = null, funds = amount).executePost({ onFailure(it) }, { onSuccess(it) })
                    TradeSide.SELL -> CBProApi.orderStop(apiInitData, tradeSide, tradingPair.idForExchange(Exchange.CBPro), stopPrice, size = amount, funds = null).executePost({ onFailure(it) }, { onSuccess(it) })
                }
            }
            Exchange.Binance -> {
                //TODO: fix timeInForce
                BinanceApi.orderStop(apiInitData, tradingPair.idForExchange(Exchange.Binance), tradeSide, null, amount, stopPrice).executePost({ onFailure(it) }, { onSuccess(it) })
            }
        }
    }

    fun getAllProducts(onFailure: (Result.Failure<String, FuelError>) -> Unit, onSuccess: () -> Unit) {
        //Do ALL exchanges
        var binanceProducts = listOf<BinanceSymbol>()
        var cbProProducts = listOf<CBProProduct>()

        CBProApi.products(apiInitData).get(onFailure) {
            if (binanceProducts.isEmpty() && isBinanceActive) {
                cbProProducts = it
            } else {
                compileAllProducts(it, binanceProducts)
                onSuccess()
            }
        }
        if (isBinanceActive) {
            BinanceApi.exchangeInfo(apiInitData).getProducts(onFailure) {
                if (cbProProducts.isEmpty()) {
                    binanceProducts = it
                } else {
                    compileAllProducts(cbProProducts, it)
                    onSuccess()
                }
            }
        }
    }

    private fun compileAllProducts(cbProProducts: List<CBProProduct>, binanceProducts: List<BinanceSymbol>) {
        val backupProductMap = Product.map
        Product.map.clear()
        for (apiProduct in cbProProducts) {
            val tradingPair = TradingPair(apiProduct)
            Product.map[apiProduct.base_currency]?.let { product ->
                product.tradingPairs = product.tradingPairs.plus(tradingPair)
            } ?: run {
                val currency = Currency(apiProduct.base_currency)
                val newProduct = Product(currency, listOf(tradingPair))
                newProduct.addToHashMap()
            }
        }
        for (apiProduct in binanceProducts) {
            val tradingPair = TradingPair(apiProduct)
            Product.map[tradingPair.baseCurrency.id]?.let {  product ->
                product.tradingPairs = product.tradingPairs.plus(tradingPair)
            } ?: run {
                val newProduct = Product(tradingPair.baseCurrency, listOf(tradingPair))
                newProduct.addToHashMap()
            }
        }
        for (product in Product.map.values) {
            product.accounts = backupProductMap[product.currency.id]?.accounts ?: mapOf()
        }
    }


    fun getAllAccounts(onFailure: (Result.Failure<String, FuelError>) -> Unit, onSuccess: () -> Unit) {
        if (!Exchange.isAnyLoggedIn()) {
            onSuccess()
        } else {
            if (CBProApi.credentials != null) {
                CBProApi.accounts(apiInitData).getAllAccountInfo(onFailure, onSuccess)
            }

            if (isBinanceActive) {
                if (BinanceApi.credentials != null) {
                    BinanceApi.accounts(apiInitData).getAndLink(onFailure, onSuccess)
                }
            }
        }
    }

    fun sendCrypto(currency: Currency, amount: Double, sourceExchange: Exchange, destAddress: String, onFailure: (Result.Failure<ByteArray, FuelError>) -> Unit, onSuccess: () -> Unit) {
        when (sourceExchange) {
            Exchange.CBPro -> {
                CBProApi.sendCrypto(apiInitData, amount, currency, destAddress).executePost(onFailure) {
                    onSuccess()
                }
            }
            Exchange.Binance -> {
                BinanceApi.sendCrypto(apiInitData, currency, destAddress, null, amount, null).executePost(onFailure) {
                    onSuccess()
                }
            }
        }
    }

    fun getDepositAddress(exchange: Exchange, currency: Currency, coinbaseAccountId: String?, onFailure: (Result.Failure<Any, FuelError>) -> Unit, onSuccess: (DepositAddressInfo) -> Unit) {
        when (exchange) {
            Exchange.CBPro -> {
                if (coinbaseAccountId != null) {
                    CBProApi.depositAddress(apiInitData, coinbaseAccountId).get(onFailure) { result ->
                        onSuccess(DepositAddressInfo(result))
                    }
                } else {
                    onFailure(Result.Failure(FuelError(Exception())))
                }
            }
            Exchange.Binance -> {
                BinanceApi.depositAddress(apiInitData, currency).get(onFailure) { result ->
                    onSuccess(DepositAddressInfo(result))
                }
            }
        }
    }

    fun stablecoinDirectConversion(tradingPair: TradingPair, amount: Double, onFailure: (Result.Failure<Any, FuelError>) -> Unit, onSuccess: () -> Unit) {
        if (tradingPair.exchange == Exchange.CBPro) {
            CBProApi.stablecoinConversion(apiInitData, amount, tradingPair).executePost(onFailure) { onSuccess() }
        }
    }


    private val isAnyXProActive: Boolean
        get() {
            apiInitData?.context?.let {
                val prefs = Prefs(it)
                return prefs.isAnyXProActive
            } ?: run {
                return false
            }
        }


    private val enabledExchanges: List<Exchange>
        get() {
            //TODO: add setting to enable/disable exchanges
            return if (isAnyXProActive) {
                listOf(Exchange.CBPro, Exchange.Binance)
            } else {
                listOf(Exchange.CBPro)
            }
        }

//    private val loggedInExchanges: List<Exchange>
//        get() {
//            val exchangeList = mutableListOf<Exchange>()
//            for (exchange in enabledExchanges) {
//                when (exchange) {
//                    Exchange.CBPro -> if (CBProApi.credentials != null) {
//                        exchangeList.add(Exchange.CBPro)
//                    }
//                    Exchange.Binance -> if (BinanceApi.credentials != null) {
//                        exchangeList.add(Exchange.Binance)
//                    }
//                }
//            }
//            return exchangeList
//        }

    val isBinanceActive: Boolean
        get() { return enabledExchanges.contains(Exchange.Binance) }

    val isBinanceLoggedIn: Boolean
        get() { return isBinanceActive && BinanceApi.credentials != null }
}