package com.anyexchange.anyx.classes

import com.anyexchange.anyx.classes.api.BinanceSymbol
import com.anyexchange.anyx.classes.api.CBProProduct

class TradingPair(val exchange: Exchange, val baseCurrency: Currency, val quoteCurrency: Currency, val id: String?, val orderTypes: List<TradeType>) {
    constructor(product: CBProProduct) : this(Exchange.CBPro, Currency(product.base_currency), Currency(product.quote_currency), product.id, listOf(TradeType.LIMIT, TradeType.MARKET, TradeType.STOP))
    constructor(product: BinanceSymbol) : this(Exchange.Binance, Currency(product.baseAsset), Currency(product.quoteAsset), product.symbol, listOf(TradeType.LIMIT, TradeType.MARKET, TradeType.STOP))
    constructor(exchange: Exchange, id: String) : this(exchange, Currency(id.substring(0, 3)),
            when (exchange) {
                Exchange.Binance -> Currency(id.substring(3, 6))
                Exchange.CBPro -> Currency(id.substring(4, 7)) }, id, listOf(TradeType.LIMIT, TradeType.MARKET, TradeType.STOP))
    constructor(exchange: Exchange, baseCurrency: Currency, quoteCurrency: Currency) : this(exchange, baseCurrency, quoteCurrency, "$baseCurrency-$quoteCurrency", listOf(TradeType.LIMIT, TradeType.MARKET, TradeType.STOP))


    fun idForExchange(exchange: Exchange) : String {
        return when (exchange) {
            Exchange.CBPro -> baseCurrency.toString() + "-" + quoteCurrency.toString()
            Exchange.Binance -> baseCurrency.toString() + quoteCurrency.toString()
        }
    }

    override fun toString(): String {
        return idForExchange(Exchange.CBPro)
    }

    override fun equals(other: Any?): Boolean {
        return if (other is TradingPair) {
            (other.baseCurrency == this.baseCurrency && other.quoteCurrency == this.quoteCurrency)
        } else {
            super.equals(other)
        }
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }
}