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
        val currencyCode = currency.orderValue
        val priceCode = (price * 100).toInt()
        return (triggerCode * 10000000) + (currencyCode * 1000000) + priceCode
    }

    override fun toString(): String {
        var alertString = price.toString() + '\n'
        alertString += currency.toString() + '\n'
        alertString += triggerIfAbove.toString() + '\n'
        alertString += hasTriggered.toString() + '\n'
        return alertString
    }

    companion object {
        fun forString(string: String): Alert {
            val splitString = string.split('\n')
            val price = splitString[0].toDoubleOrZero()
            val currency = Currency.forString(splitString[1]) ?: Currency.USD
            val triggerIfAbove = splitString[2].toBoolean()
            val hasTriggered = splitString[3].toBoolean()
            return Alert(price, currency, triggerIfAbove, hasTriggered)
        }
    }
}