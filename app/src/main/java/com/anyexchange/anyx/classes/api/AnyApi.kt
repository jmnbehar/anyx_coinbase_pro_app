package com.anyexchange.anyx.classes.api

import com.anyexchange.anyx.classes.*
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import java.util.*
import com.anyexchange.anyx.classes.Currency

class AnyApi(val apiInitData: ApiInitData?) {
    companion object {
        val defaultFailure: Result.Failure<String, FuelError> = Result.Failure(FuelError(Exception()))
    }

    fun getAndStashOrderList(exchange: Exchange, currency: Currency?, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (List<Order>) -> Unit) {
        when (exchange) {
            Exchange.CBPro -> {
                CBProApi.listOrders(apiInitData, currency).getAndStash(onFailure, onSuccess)
            }
            Exchange.Binance -> {
                BinanceApi.listOrders(apiInitData, currency).getAndStash(onFailure, onSuccess)
            }
        }
    }

    fun getAndStashFillList(exchange: Exchange, tradingPair: TradingPair?, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (List<Fill>) -> Unit) {
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
                    onSuccess(it.price.toDoubleOrNull())
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
                        if (hasBinanceCompleted || !isAnyXProActive) {
                            onSuccess()
                        }
                    }
                }
            }
        }

        if (isAnyXProActive) {
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
                //TODO: figure this out
                onFailure(defaultFailure)
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
                val interval = BinanceApi.Interval.FiveMinutes
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
            if (binanceProducts.isEmpty()) {
                cbProProducts = it
            } else {
                compileAllProducts(it, binanceProducts)
                onSuccess()
            }
        }
        if (isAnyXProActive) {
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

    fun getAllAccounts(onFailure: (Result.Failure<String, FuelError>) -> Unit, onSuccess: () -> Unit) {
        if (CBProApi.credentials == null && CBProApi.credentials == null) {
            onSuccess()
        } else {
            if (CBProApi.credentials != null) {
                CBProApi.accounts(apiInitData).getAllAccountInfo(onFailure, onSuccess)
            }

            if (isAnyXProActive) {
                if (BinanceApi.credentials != null) {
                    BinanceApi.accounts(apiInitData).getAndLink(onFailure, onSuccess)
                }
            }
        }
    }


    private fun compileAllProducts(cbProProducts: List<CBProProduct>, binanceProducts: List<BinanceSymbol>) {
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
}