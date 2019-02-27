package com.anyexchange.anyx.api

//Data classes for Binance  Exchange Calls

data class BinanceExchangeInfo(
        val timezone: String,
        val serverTime: String,
        val rateLimits: List<BinanceRateLimit>,
        val exchangeFilters: List<Any>, //TODO: figure out what goes here
        val symbols: List<BinanceSymbol>)

data class BinanceRateLimit(
        val rateLimitType: String,
        val interval: String,
        val limit: Long)

data class BinanceSymbol(
        val symbol: String,
        val status: String,
        val baseAsset: String,
        val baseAssetPrecision: Int,
        val quoteAsset: String,
        val quotePrecision: Int,
        val orderTypes: List<String>,
        val icebergAllowed: Boolean,
        val filters: List<BinanceFilter>)

data class BinanceFilter(
        val filterType: String,

        val minPrice: Double?,
        val maxPrice: Double?,
        val tickSize: Double?,

        val minQty: Double?,
        val maxQty: Double?,
        val stepSize: Double?,

        val minNotional: Double?
)

data class BinanceDepth(
        val lastUpdateId: String,
        val bids: List<List<Any>>,
        val asks: List<List<Any>>
)

data class BinanceGeneralFill(
        val id: String,
        val price: Double,
        val qty: Double,
        val time: Long,
        val isBuyerMaker: Boolean,
        val isBestMatch: Boolean)

data class BinanceAccountFill(
        val symbol: String,
        val id: String,
        val orderId: Long,
        val price: Double,
        val qty: Double,
        val commission: Double,
        val commissionAsset: String,
        val time: Long,
        val isBuyer: Boolean,
        val isMaker: Boolean,
        val isBestMatch: Boolean)

data class BinanceAggregateFill(
        val a: String,  // Aggregate tradeId

        val p: Double,  // Price
        val q: Double,  // Quantity
        val f: Long,    // First tradeId
        val l: Long,    // Last tradeId
        val T: Long,    // Timestamp
        val m: Boolean // Was the buyer the maker?
        //val M: Boolean  // Was the trade the best price match?
)

data class BinanceDayChange(
        val symbol: String,
        val priceChange: Double,
        val priceChangePercent: Double,
        val weightedAvgPrice: Double,
        val prevClosePrice: Double,
        val lastPrice: Double,
        val lastQty: Double,
        val bidPrice: Double,
        val askPrice: Double,
        val openPrice: Double,
        val highPrice: Double,
        val lowPrice: Double,
        val volume: Double,
        val quoteVolume: Double,
        val openTime: Long,
        val closeTime: Long,
        val firstId: Long,
        val lastId: Long,
        val count: Long)


data class BinanceTicker(
        val symbol: String,
        val price: Double)

data class BinanceBookTicker(
        val symbol: String,
        val bidPrice: Double,
        val bidQty: Double,
        val askPrice: Double,
        val askQty: Double)

data class BinanceTradeResponseAck(
        val symbol: String,
        val orderId: Long,
        val clientOrderId: String,
        val transactTime: Long)

data class BinanceTradeResponseResult(
        val symbol: String,
        val orderId: Long,
        val clientOrderId: String,
        val transactTime: Long,
        val price: Double,
        val origQty: Double,
        val executedQty: Double,
        val cummulativeQuoteQty: Double,
        val status: String,
        val timeInForce: String,
        val type: String,
        val side: String)

data class BinanceTradeResponseFull(
        //Ack, result, or full:
        val symbol: String,
        val orderId: Long,
        val clientOrderId: String,
        val transactTime: Long,
        //Here on is Result or Full:
        val price: Double?,
        val origQty: Double?,
        val executedQty: Double?,
        val cummulativeQuoteQty: Double?,
        val status: String?,
        val timeInForce: String?,
        val type: String?,
        val side: String?,
        //Full only:
        val fills: List<BinanceTradeRespFill>?)

data class BinanceTradeRespFill(
        val price: Double,
        val qty: Double,
        val commission: Double,
        val commissionAsset: String)

data class BinanceOrder(
        val symbol: String,
        val orderId: Int,
        val clientOrderId: String?,
        val price: Double,
        val origQty: Double,
        val executedQty: Double,
        val cummulativeQuoteQty: Double,    //cumulative?
        val status: String,
        val timeInForce: String,
        val type: String,
        val side: String,
        val stopPrice: Double?,
        val icebergQty: Double?,
        val time: Long,
        val updateTime: Long?,
        val isWorking: Boolean)

data class BinanceAccount(
        val makerCommission: Int,
        val takerCommission: Int,
        val buyerCommission: Int,
        val sellerCommission: Int,
        val canTrade: Boolean,
        val canWithdraw: Boolean,
        val canDeposit: Boolean,
        val updateTime: Long,
        val balances: List<BinanceBalance>)

data class BinanceBalance(
        val asset: String,
        val free: Double,
        val locked: Double)

data class BinanceWithdrawResponse(
        val msg: String,
        val success: Boolean,
        val id: String)


data class BinanceDepositHistory(
        val depositList: List<BinanceDeposit>,
        val success: Boolean)

data class BinanceDeposit(
        val insertTime: Long,
        val amount: Double,
        val asset: String,
        val address: String,
        val addressTag: String?,
        val txId: String,
        val status: Int)

data class BinanceWithdrawHistory(
        val withdrawList: List<BinanceWithdrawal>,
        val success: Boolean)

data class BinanceWithdrawal(
        val id: String,
        val amount: Double,
        val address: String,
        val addressTag: String?,
        val asset: String,
        val txId: String,
        val applyTime: Long,
        val status: Int)

data class BinanceDepositAddress(
        val address: String,
        val success: Boolean,
        val addressTag: String?,
        val asset: String)

data class BinanceStatus(
        val status: Int?, //for system status only, 0: normal，1：system maintenance
        val msg: String,

        //for account status only:
        val success: Boolean?,
        val objs: List<String>?
)