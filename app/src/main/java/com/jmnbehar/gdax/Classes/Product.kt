package com.jmnbehar.gdax.Classes

import java.time.LocalDateTime

/**
 * Created by jmnbehar on 12/20/2017.
 */

enum class Currency {
    BTC,
    BCH,
    ETH,
    LTC,
    USD;

    override fun toString() : String {
        return when (this) {
            BTC -> "BTC"
            BCH -> "BCH"
            ETH -> "ETH"
            LTC -> "LTC"
            USD -> "USD"
        }
    }

    fun productId() : String {
        return when (this) {
            BTC -> "BTC-USD"
            BCH -> "BCH-USD"
            ETH -> "ETH-USD"
            LTC -> "LTC-USD"
            USD -> "USD"
        }
    }

    var fullName : String = ""
        get() = when (this) {
            BTC -> "Bitcoin"
            BCH -> "Bitcoin Cash"
            ETH -> "Ethereum"
            LTC -> "Litecoin"
            USD -> "USD"
        }

    companion object {
        fun fromString(string: String) : Currency {
            return when (string) {
                "BTC-USD" -> BTC
                "BTC" -> BTC
                "BCH-USD" -> BCH
                "BCH" -> BCH
                "ETH-USD" -> ETH
                "ETH" -> ETH
                "LTC-USD" -> LTC
                "LTC" -> LTC
                "USD" -> USD
                else -> USD
            }
        }
    }
}

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
