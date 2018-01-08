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
        var list = mutableListOf<Product>()
        var btcProduct: Product? = null
            get() = list.filter { a -> a.currency == Currency.BTC }.firstOrNull()
        var ltcProduct: Product? = null
            get() = list.filter { a -> a.currency == Currency.LTC }.firstOrNull()
        var ethProduct: Product? = null
            get() = list.filter { a -> a.currency == Currency.ETH }.firstOrNull()
        var usdProduct: Product? = null
            get() = list.filter { a -> a.currency == Currency.USD }.firstOrNull()
        var bchProduct: Product? = null
            get() = list.filter { a -> a.currency == Currency.BCH }.firstOrNull()
    }

}
