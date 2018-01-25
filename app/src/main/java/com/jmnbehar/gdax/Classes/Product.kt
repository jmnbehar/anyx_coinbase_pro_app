package com.jmnbehar.gdax.Classes

import java.util.*

/**
 * Created by jmnbehar on 12/20/2017.
 */


class Product(apiProduct: ApiProduct, var candles: List<Candle>) {
    var currency: Currency
    var price: Double
    var id: String
    var lastCandleUpdateTime: Calendar


    init {
        currency = Currency.fromString(apiProduct.base_currency)
        price = candles.lastOrNull()?.close ?: 0.0
        id = apiProduct.id
        lastCandleUpdateTime = Calendar.getInstance()
    }

    companion object {

        fun fiatProduct(currency: String) = Product(ApiProduct(currency, currency, "0", "0", "0", "0", "0", "0", false, "0"), listOf(Candle(0.0, 1.0, 1.0, 1.0, 1.0, 0.0)))
    }

}
