package com.anyexchange.anyx.classes

/**
 * Created by anyexchange on 12/20/2017.
 */

class Alert(var price: Double, val currency: Currency, val triggerIfAbove: Boolean, var hasTriggered: Boolean = false) {

    override fun equals(other: Any?): Boolean {
        return if (other is Alert) {
            ((this.currency == other.currency) && (this.price == other.price) && (this.triggerIfAbove == other.triggerIfAbove))
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        val triggerCode = if (triggerIfAbove) {
            0
        } else {
            1
        }
        val currencyCode = when (currency) {
            Currency.USD,
            Currency.EUR,
            Currency.GBP -> 0
            Currency.LTC -> 1
            Currency.ETH -> 2
            Currency.BCH -> 3
            Currency.BTC -> 4
            //Currency.ETC -> 5
        }
        val priceCode = (price * 100).toInt()
        val hashCodeInt = (triggerCode * 10000000) + (currencyCode * 1000000) + priceCode
        return hashCodeInt
    }

    override fun toString(): String {
        var alertString = price.toString() + '\n'
        alertString += currency.toString() + '\n'
        alertString += triggerIfAbove.toString() + '\n'
        alertString += hasTriggered.toString() + '\n'
        return alertString
    }

    companion object {
        fun fromString(string: String): Alert {
            val splitString = string.split('\n')
            val price = splitString[0].toDoubleOrZero()
            val currency = Currency.forString(splitString[1]) ?: Currency.USD
            val triggerIfAbove = splitString[2].toBoolean()
            val hasTriggered = splitString[3].toBoolean()
            return Alert(price, currency, triggerIfAbove, hasTriggered)
        }
    }
}