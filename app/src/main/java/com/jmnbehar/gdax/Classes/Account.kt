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
            get() = list.filter { a -> a.product.currency == "BTC" }.firstOrNull()
        var ltcAccount: Account? = null
            get() = list.filter { a -> a.product.currency == "LTC" }.firstOrNull()
        var ethAccount: Account? = null
            get() = list.filter { a -> a.product.currency == "ETH" }.firstOrNull()
        var usdAccount: Account? = null
            get() = list.filter { a -> a.product.currency == "USD" }.firstOrNull()
        var bchAccount: Account? = null
            get() = list.filter { a -> a.product.currency == "BCH" }.firstOrNull()

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
                            //do not reference AccountsFragmet, thats not aprop
                            val relevantProduct = Product.list.filter { p -> p.currency == apiAccount.currency }.firstOrNull()
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
