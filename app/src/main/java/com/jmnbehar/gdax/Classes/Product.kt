package com.jmnbehar.gdax.Classes

/**
 * Created by jmnbehar on 12/20/2017.
 */


class Product(val apiProduct: ApiProduct, val candles: List<List<Long>>) {
    var currency: String
    lateinit var price: String

    init {
        currency = apiProduct.base_currency
    }

}
