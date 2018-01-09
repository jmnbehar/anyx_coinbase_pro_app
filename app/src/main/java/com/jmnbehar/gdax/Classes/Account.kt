package com.jmnbehar.gdax.Classes

import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Created by jmnbehar on 12/20/2017.
 */


class Account(val product: Product, apiAccount: ApiAccount) {
    var balance: Double = apiAccount.balance.toDoubleOrZero()
    var value: Double
    var currency = product.currency
        get() = product.currency

    init {
        value = product.price * balance
    }

    companion object {

        var list = mutableListOf<Account>()
        var btcAccount: Account? = null
            get() = forCurrency(Currency.BTC)
        var ltcAccount: Account? = null
            get() = forCurrency(Currency.LTC)
        var ethAccount: Account? = null
            get() = forCurrency(Currency.ETH)
        var usdAccount: Account? = null
            get() = forCurrency(Currency.USD)
        var bchAccount: Account? = null
            get() = forCurrency(Currency.BCH)

        fun forCurrency(currency: Currency): Account? {
            return list.find { a -> a.product.currency == currency }
        }

        fun getAccountInfo(onComplete: () -> Unit) {
            list.clear()

            GdaxApi.accounts().executeRequest { result ->
                when (result) {
                    is Result.Failure -> {
                        //error
                        println("Error!: ${result.error}")
                    }
                    is Result.Success -> {
                        val gson = Gson()

                        val apiAccountList: List<ApiAccount> = gson.fromJson(result.value, object : TypeToken<List<ApiAccount>>() {}.type)
                        for (apiAccount in apiAccountList) {
                            val currency = Currency.fromString(apiAccount.currency)
                            val relevantProduct = Product.withCurrency( currency )
                            if (relevantProduct != null) {
                                list.add(Account(relevantProduct, apiAccount))
                            }
                        }
                        onComplete()
                    }
                }
            }
        }
    }
}
