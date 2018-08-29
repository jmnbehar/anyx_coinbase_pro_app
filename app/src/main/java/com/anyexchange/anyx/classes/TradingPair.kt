package com.anyexchange.anyx.classes

class TradingPair(val baseCurrency: Currency, val quoteCurrency: Currency) {
    constructor(id: String) : this(
            if (id.length >= 3) { Currency.forString(id.substring(0, 3)) ?: Currency.BTC } else { Currency.BTC },
            if (id.length >= 7) { Currency.forString(id.substring(4, 7)) ?: Currency.USD } else { Currency.USD })

    val id = baseCurrency.toString() + "-" + quoteCurrency.toString()

    override fun toString(): String {
        return id
    }

    override fun equals(other: Any?): Boolean {
        return if (other is TradingPair) {
            (other.baseCurrency == this.baseCurrency && other.quoteCurrency == this.quoteCurrency)
        } else {
            super.equals(other)
        }
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}