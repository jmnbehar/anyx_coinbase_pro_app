package com.jmnbehar.gdax.Classes

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

    init {
        currency = Currency.fromString(apiProduct.base_currency)
        price = candles.first().close
        id = apiProduct.id
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
    }

}
