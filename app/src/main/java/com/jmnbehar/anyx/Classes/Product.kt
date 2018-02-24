package com.jmnbehar.anyx.Classes

/**
 * Created by jmnbehar on 12/20/2017.
 */


class Product(var currency: Currency, var id: String, var candles: List<Candle>) {
    constructor(apiProduct: ApiProduct, candles: List<Candle>)
            : this(Currency.fromString(apiProduct.base_currency), apiProduct.id, candles)

    var price = candles.lastOrNull()?.close ?: 0.0
    var dayCandles = candles
    var candlesTimespan = TimeInSeconds.oneDay

    override fun toString(): String {
        var alertString = currency.toString() + '\n'
        alertString += id + '\n'
//        alertString += price.toString() + '\n'
        return alertString
    }

    companion object {
        fun fromString(string: String): Product {
            val splitString = string.split('\n')
            val currency = Currency.fromString(splitString[0])
            val id = splitString[1]
//            val price = splitString[1].toDoubleOrZero()
            return Product(currency, id, listOf())
        }

        fun fiatProduct(currency: String) = Product(Currency.fromString(currency), currency,
                listOf(Candle(0.0, 1.0, 1.0, 1.0, 1.0, 0.0)))

    }

}
