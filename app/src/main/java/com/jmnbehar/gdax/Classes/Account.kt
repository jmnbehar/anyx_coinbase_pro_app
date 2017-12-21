package com.jmnbehar.gdax.Classes

/**
 * Created by jmnbehar on 12/20/2017.
 */


class Account(product: Product, val apiAccount: ApiAccount) {
    var currency: String
    var balance: Double
    var value: Double
    var price: Double
    init {
        currency = product.currency
        balance = apiAccount.balance.toDouble()
        price = product.price
        value = price * balance
    }

}
