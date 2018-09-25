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
                    CBProApi.ticker(apiInitData, tradingPair.idForExchange(Exchange.CBPro)).get(onFailure) {
                        onSuccess(it.price.toDoubleOrNull())
                    }
                }
                Exchange.Binance -> {
                    BinanceApi.ticker(apiInitData, tradingPair.idForExchange(Exchange.Binance)).get(onFailure) {
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
                    onFailure(Result.Failure(FuelError(Exception())))
                }
            }
        }

        fun getCandles(apiInitData: ApiInitData?, exchange: Exchange, tradingPair: TradingPair, timespan: Long, timeOffset: Long, granularity: Long, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (List<Candle>) -> Unit) {
            when (exchange) {
                Exchange.CBPro -> {
                    CBProApi.candles(apiInitData, tradingPair.idForExchange(Exchange.CBPro), timespan, granularity, 0).getCandles(onFailure, onSuccess)
                }
                Exchange.Binance -> {
                    val interval = granularity.toString()
                    val now = Date().time
                    val startTime = now - (timespan + timeOffset)
                    val endTime = now - timeOffset
                    BinanceApi.candles(apiInitData, tradingPair.idForExchange(Exchange.Binance), interval, startTime, endTime).getCandles(onFailure, onSuccess)
                }
                //    class candles(initData: ApiInitData?, val productId: String, val interval: String, var startTime: Long? = null, var endTime: Long? = null, var limit: Int? = null) : BinanceApi(initData) {

            }
        }
    }
}