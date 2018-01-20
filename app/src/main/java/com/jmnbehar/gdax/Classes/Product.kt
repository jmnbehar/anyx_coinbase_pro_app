package com.jmnbehar.gdax.Classes

import java.time.LocalDateTime

import com.jmnbehar.gdax.R

/**
 * Created by jmnbehar on 12/20/2017.
 */


class Product(apiProduct: ApiProduct, var candles: List<Candle>) {
    var currency: Currency
    var price: Double
    var id: String
    var lastCandleUpdateTime: LocalDateTime


    init {
        currency = Currency.fromString(apiProduct.base_currency)
        price = candles.lastOrNull()?.close ?: 0.0
        id = apiProduct.id
        lastCandleUpdateTime = LocalDateTime.now()
    }

    companion object {
        private var list = mutableListOf<Product>()

        fun addToList(product: Product) {
            list.add(product)
        }
        var listSize: Int = 0
            get() = list.size

        fun withCurrency(currency: Currency): Product? {
            return list.find { p -> p.currency == currency }
        }

        fun fiatProduct(currency: String) = Product(ApiProduct(currency, currency, "0", "0", "0", "0", "0", "0", false, "0"), listOf(Candle(0.0, 1.0, 1.0, 1.0, 1.0, 0.0)))
    }

}
