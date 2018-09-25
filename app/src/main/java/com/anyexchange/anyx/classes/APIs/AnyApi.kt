package com.anyexchange.anyx.classes.APIs

import com.anyexchange.anyx.R
import com.anyexchange.anyx.adapters.HistoryPagerAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.fragments.main.ChartFragment
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import java.util.*

class AnyApi {
    companion object {
        private val defaultFailure: Result.Failure<String, FuelError> = Result.Failure(FuelError(Exception()))

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

        fun ticker(apiInitData: ApiInitData?, exchange: Exchange, tradingPair: TradingPair, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (Double?) -> Unit) {
            when (exchange) {
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
                            account.apiAccount = cbProAccount
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
        fun orderMarket(apiInitData: ApiInitData?, exchange: Exchange, tradeSide: TradeSide, tradingPair: TradingPair, amount: Double? = null, funds: Double? = null,
                        onFailure: (result: Result.Failure<ByteArray, FuelError>) -> Unit, onSuccess: (Result<ByteArray, FuelError>) -> Unit) {
            when (exchange) {
                Exchange.CBPro -> {
                    when (tradeSide) {
                        TradeSide.BUY ->  CBProApi.orderMarket(apiInitData, tradeSide, tradingPair.idForExchange(Exchange.CBPro), size = null, funds = amount).executePost({ onFailure(it) }, { onSuccess(it) })
                        TradeSide.SELL -> CBProApi.orderMarket(apiInitData, tradeSide, tradingPair.idForExchange(Exchange.CBPro), size = amount, funds = null).executePost({ onFailure(it) }, { onSuccess(it) })
                    }
                }
                Exchange.Binance -> {
                    BinanceApi.orderMarket(apiInitData, tradingPair.idForExchange(Exchange.Binance), tradeSide, amount!!, funds!!).executePost({ onFailure(it) }, { onSuccess(it) })
                }
            }
        }
        fun orderStop(apiInitData: ApiInitData?, exchange: Exchange, tradeSide: TradeSide, productId: String, price: Double, size: Double? = null, funds: Double? = null,
                      onFailure: (Result.Failure<String, FuelError>) -> Unit, onSuccess: () -> Unit) {

        }
    }
}