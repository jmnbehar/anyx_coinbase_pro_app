package com.jmnbehar.gdax.Classes

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jmnbehar.gdax.Fragments.AccountsFragment

/**
 * Created by jmnbehar on 12/21/2017.
 */

object AccountList {
    var list = mutableListOf<Account>()
    var btcAccount: Account? = list.filter { a -> a.currency == "BTC" }.firstOrNull()
    var ltcAccount: Account? = list.filter { a -> a.currency == "LTC" }.firstOrNull()
    var ethAccount: Account? = list.filter { a -> a.currency == "ETH" }.firstOrNull()
    var usdAccount: Account? = list.filter { a -> a.currency == "USD" }.firstOrNull()
    var bchAccount: Account? = list.filter { a -> a.currency == "BCH" }.firstOrNull()

    fun getAccountInfo(onComplete: () -> Unit) {
        Fuel.request(GdaxApi.accounts()).responseString { request, _, result ->
            //do something with response
            println("url: " + request.url)
            when (result) {
                is Result.Failure -> {
                    //error
                    println("Error!: ${result.error}")
                }
                is Result.Success -> {
                    val gson = Gson()

                    val apiAccountList: List<ApiAccount> = gson.fromJson(result.value, object : TypeToken<List<ApiAccount>>() {}.type)
                    for (apiAccount in apiAccountList) {
                        val relevantProduct = AccountsFragment.products.filter { p -> p.currency == apiAccount.currency }.firstOrNull()
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