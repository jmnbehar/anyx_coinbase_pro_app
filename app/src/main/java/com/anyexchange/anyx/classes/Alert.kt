package com.anyexchange.anyx.classes

import java.util.*

/**
 * Created by anyexchange on 12/20/2017.
 */


abstract class Alert {
    abstract val title: String
    abstract val text: String
    abstract val tag: String
}

class PriceAlert(var price: Double, val currency: Currency, val triggerIfAbove: Boolean, var hasTriggered: Boolean = false): Alert() {

    override fun equals(other: Any?): Boolean {
        return if (other is PriceAlert) {
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

    override val title = "${currency.fullName} price alert"

    override val text: String
        get() {
            val overUnder = when(triggerIfAbove) {
                true  -> "over"
                false -> "under"
            }
            return "$currency is $overUnder ${price.fiatFormat(Account.defaultFiatCurrency)}"
        }

    override val tag = "PriceAlert_${currency}_$price"

    override fun toString(): String {
        var alertString = price.toString() + '\n'
        alertString += currency.toString() + '\n'
        alertString += triggerIfAbove.toString() + '\n'
        alertString += hasTriggered.toString() + '\n'
        return alertString
    }

    companion object {
        fun forString(string: String): PriceAlert {
            val splitString = string.split('\n')
            val price = splitString[0].toDoubleOrZero()
            val currency = Currency.forString(splitString[1]) ?: Currency.USD
            val triggerIfAbove = splitString[2].toBoolean()
            val hasTriggered = splitString[3].toBoolean()
            return PriceAlert(price, currency, triggerIfAbove, hasTriggered)
        }
    }
}

class QuickChangeAlert(val currencies: MutableList<Currency>, var percentChange: Double, val isChangePositive: Boolean, val timespan: AlertTimespan): Alert() {
    enum class AlertTimespan {
        TEN_MINUTES,
        HALF_HOUR,
        HOUR;

        override fun toString(): String {
            return when (this) {
                TEN_MINUTES -> "ten minutes"
                HALF_HOUR -> "half hour"
                HOUR -> "hour"
            }
        }
    }

    private fun currencyListToString() : String {
            var currencyListString = ""
            for ((index, currency) in currencies.withIndex()) {
                if (index != 0) {
                    currencyListString += if (index == currencies.size - 1) {
                        if (currencies.size > 2) {
                            ", and "
                        } else {
                            " and "
                        }
                    } else {
                        ", "
                    }
                }
                currencyListString += currency.fullName
            }
            return currencyListString
        }

    override val title: String
        get() =  "${currencyListToString()} price change alert"

    override val text: String
        get() {
            val upDown = when (isChangePositive) {
                true -> "up"
                false -> "down"
            }
            val currenciesString = currencyListToString()
            return when {
                currencies.size == 1 -> "$currenciesString is $upDown ${percentChange.percentFormat()} in the past $timespan"
                currencies.size > 1 -> "$currenciesString are $upDown at least ${percentChange.percentFormat()} in the past $timespan"
                else -> ""
            }
        }

    override val tag: String
        get() = "QuickChangeAlert_${Date().time}"
}