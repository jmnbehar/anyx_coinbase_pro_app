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

}
