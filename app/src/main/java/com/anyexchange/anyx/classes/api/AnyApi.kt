package com.anyexchange.anyx.classes.api

import com.anyexchange.anyx.classes.*
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import java.util.*
import com.anyexchange.anyx.classes.Currency
import com.anyexchange.anyx.fragments.main.ChartFragment

class AnyApi {
    companion object {
        val defaultFailure: Result.Failure<String, FuelError> = Result.Failure(FuelError(Exception()))

        fun getAndStashOrderList(apiInitData: ApiInitData?, exchange: Exchange, tradingPair: TradingPair?, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (List<Order>) -> Unit) {
            when (exchange) {
                Exchange.CBPro -> {
                    CBProApi.listOrders(apiInitData, tradingPair).getAndStash(onFailure, onSuccess)
                }
                Exchange.Binance -> {
                    BinanceApi.listOrders(apiInitData, tradingPair).getAndStash(onFailure, onSuccess)
                }
            }
        }

        fun getAndStashFillList(apiInitData: ApiInitData?, exchange: Exchange, tradingPair: TradingPair?, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (List<Fill>) -> Unit) {
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

        fun cancelOrder(apiInitData: ApiInitData?, order: Order, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (result: Result.Success<String, FuelError>) -> Unit) {
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

        fun ticker(apiInitData: ApiInitData?, tradingPair: TradingPair, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (Double?) -> Unit) {
            when (tradingPair.exchange) {
                Exchange.CBPro -> {
                    CBProApi.ticker(apiInitData, tradingPair).get(onFailure) {
                        onSuccess(it.price.toDoubleOrNull())
                    }
                }
                Exchange.Binance -> {
                    BinanceApi.ticker(apiInitData, tradingPair).get(onFailure) {
                        onSuccess(it.price)
                    }
                }
            }
        }

        fun updateAccount(apiInitData: ApiInitData?, account: Account, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (Account) -> Unit) {
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

        fun getCandles(apiInitData: ApiInitData?, exchange: Exchange, tradingPair: TradingPair, timespan: Long, timeOffset: Long, granularity: Long, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (List<Candle>) -> Unit) {
            when (exchange) {
                Exchange.CBPro -> {
                    CBProApi.candles(apiInitData, tradingPair, timespan, granularity, 0).getCandles(onFailure, onSuccess)
                }
                Exchange.Binance -> {
                    val interval = granularity.toString()
                    val now = Date().time
                    val startTime = now - (timespan + timeOffset)
                    val endTime = now - timeOffset
                    BinanceApi.candles(apiInitData, tradingPair, interval, startTime, endTime).getCandles(onFailure, onSuccess)
                }
            }
        }

        fun orderLimit(apiInitData: ApiInitData?, exchange: Exchange, tradeSide: TradeSide, tradingPair: TradingPair, limitPrice: Double, amount: Double, timeInForceStr: String?,
                       cancelAfter: String?, icebergQty: Double?, onFailure: (result: Result.Failure<ByteArray, FuelError>) -> Unit, onSuccess: (Result<ByteArray, FuelError>) -> Unit) {
            when (exchange) {
                Exchange.CBPro -> {
                    val timeInForce = CBProApi.TimeInForce.forString(timeInForceStr)
                    CBProApi.orderLimit(apiInitData, tradeSide, tradingPair.idForExchange(Exchange.CBPro), limitPrice, amount, timeInForce = timeInForce, cancelAfter = cancelAfter).executePost({ onFailure(it) }, { onSuccess(it) })
                }
                Exchange.Binance -> {
                    val timeInForce = BinanceApi.TimeInForce.forString(timeInForceStr)
                    BinanceApi.orderLimit(apiInitData, tradingPair.idForExchange(Exchange.Binance), tradeSide, timeInForce, amount, limitPrice, icebergQty).executePost({ onFailure(it) }, { onSuccess(it) })

                }
            }
        }
        fun orderMarket(apiInitData: ApiInitData?, exchange: Exchange, tradeSide: TradeSide, tradingPair: TradingPair, amount: Double?,
                        onFailure: (result: Result.Failure<ByteArray, FuelError>) -> Unit, onSuccess: (Result<ByteArray, FuelError>) -> Unit) {
            when (exchange) {
                Exchange.CBPro -> {
                    when (tradeSide) {
                        TradeSide.BUY ->  CBProApi.orderMarket(apiInitData, tradeSide, tradingPair.idForExchange(Exchange.CBPro), size = null, funds = amount).executePost({ onFailure(it) }, { onSuccess(it) })
                        TradeSide.SELL -> CBProApi.orderMarket(apiInitData, tradeSide, tradingPair.idForExchange(Exchange.CBPro), size = amount, funds = null).executePost({ onFailure(it) }, { onSuccess(it) })
                    }
                }
                Exchange.Binance -> {
                    BinanceApi.orderMarket(apiInitData, tradingPair.idForExchange(Exchange.Binance), tradeSide, amount!!, amount).executePost({ onFailure(it) }, { onSuccess(it) })
                }
            }
        }
        fun orderStop(apiInitData: ApiInitData?, exchange: Exchange, tradeSide: TradeSide, tradingPair: TradingPair, stopPrice: Double, amount: Double, timeInForce: String?,
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

        fun getAllProducts(apiInitData: ApiInitData?, onFailure: (Result.Failure<String, FuelError>) -> Unit, onSuccess: () -> Unit) {
            //Do ALL exchanges
            var binanceProducts = listOf<BinanceSymbol>()
            var cbProProducts = listOf<CBProProduct>()
            CBProApi.products(apiInitData).get(onFailure) {
//                if (binanceProducts.isEmpty()) {
//                    cbProProducts = it
//                } else {
//                    compileAllProducts(it, binanceProducts)
//                    onSuccess()
//                }

                compileAllProducts(it, binanceProducts)
                onSuccess()
            }
//            BinanceApi.products(apiInitData).get(onFailure) {
//                if (cbProProducts.isEmpty()) {
//                    binanceProducts = it
//                } else {
//                    compileAllProducts(cbProProducts, it)
//                    onSuccess()
//                }
//            }
        }
        private fun compileAllProducts(cbProProducts: List<CBProProduct>, binanceProducts: List<BinanceSymbol>) {
            for (apiProduct in cbProProducts) {
                val tradingPair = TradingPair(apiProduct)
                Product.map[apiProduct.base_currency]?.let {  product ->
                    product.tradingPairs = product.tradingPairs.plus(tradingPair)
                } ?: run {
                    val currency = Currency(apiProduct.base_currency)
                    val newProduct = Product(currency, listOf(tradingPair))
                    newProduct.addToHashMap()
                }
            }
            for (apiProduct in binanceProducts) {
                val tradingPair = TradingPair(apiProduct)
                Product.map[apiProduct.symbol]?.let {  product ->
                    product.tradingPairs = product.tradingPairs.plus(tradingPair)
                } ?: run {
                    val currency = Currency(apiProduct.symbol)
                    val newProduct = Product(currency, listOf(tradingPair))
                    newProduct.addToHashMap()
                }
            }
            for (product in Product.map.values) {
                product.tradingPairs = product.tradingPairs.sortedWith(compareBy({ it.quoteCurrency == Account.defaultFiatCurrency }, { it.quoteCurrency.orderValue })).reversed()
            }
        }
    }
}