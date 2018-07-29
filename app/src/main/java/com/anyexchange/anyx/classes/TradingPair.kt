package com.anyexchange.anyx.classes

class TradingPair(val baseCurrency: Currency, val quoteCurrency: Currency) {
    constructor(id: String) : this(Currency.forString(id.substring(0, 3)) ?: Currency.BTC, Currency.forString(id.substring(4, 7))  ?: Currency.USD)

    val id = baseCurrency.toString() + "-" + quoteCurrency.toString()

    override fun toString(): String {
        return id
    }
}