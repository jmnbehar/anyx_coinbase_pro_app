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
    var btcAccount: Account? = null
        get() = list.filter { a -> a.currency == "BTC" }.firstOrNull()
    var ltcAccount: Account? = null
        get() = list.filter { a -> a.currency == "LTC" }.firstOrNull()
    var ethAccount: Account? = null
        get() = list.filter { a -> a.currency == "ETH" }.firstOrNull()
    var usdAccount: Account? = null
        get() = list.filter { a -> a.currency == "USD" }.firstOrNull()
    var bchAccount: Account? = null
        get() = list.filter { a -> a.currency == "BCH" }.firstOrNull()

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