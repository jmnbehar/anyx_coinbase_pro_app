package com.jmnbehar.gdax.Classes

/**
 * Created by jmnbehar on 12/20/2017.
 */

class Alert(var price: Double, val currency: Currency, val triggerIfAbove: Boolean, var hasTriggered: Boolean = false) {

    override fun equals(other: Any?): Boolean {
        return if (other is Alert) {
            ((this.currency == other.currency) && (this.price == other.price) && (this.triggerIfAbove == other.triggerIfAbove))
        } else {
            false
        }
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
            val currency = Currency.fromString(splitString[1])
            val triggerIfAbove = splitString[2].toBoolean()
            val hasTriggered = splitString[3].toBoolean()
            return Alert(price, currency, triggerIfAbove, hasTriggered)
        }
    }
}