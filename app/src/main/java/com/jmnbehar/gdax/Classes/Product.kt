package com.jmnbehar.gdax.Classes

/**
 * Created by jmnbehar on 12/20/2017.
 */


class Product(apiProduct: ApiProduct, var candles: List<Candle>) {
    var currency: String
    var price: Double

    init {
        currency = apiProduct.base_currency
        price = candles.first().close
    }

    companion object {
        var list = mutableListOf<Product>()
        var btcProduct: Product? = null
            get() = list.filter { a -> a.currency == "BTC" }.firstOrNull()
        var ltcProduct: Product? = null
            get() = list.filter { a -> a.currency == "LTC" }.firstOrNull()
        var ethProduct: Product? = null
            get() = list.filter { a -> a.currency == "ETH" }.firstOrNull()
        var usdProduct: Product? = null
            get() = list.filter { a -> a.currency == "USD" }.firstOrNull()
        var bchProduct: Product? = null
            get() = list.filter { a -> a.currency == "BCH" }.firstOrNull()
    }

}
