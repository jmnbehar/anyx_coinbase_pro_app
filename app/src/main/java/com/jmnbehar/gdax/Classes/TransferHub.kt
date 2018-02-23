package com.jmnbehar.gdax.Classes

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


/**
 * Created by jmnbehar on 12/20/2017.
 */


object TransferHub {
    fun sweepCoinbaseAccount(currency: Currency) {
        GdaxApi.coinbaseAccounts().executeRequest({ println("failure") } , { result->
            println("result")
            val gson = Gson()
            val apiCBAccountString = result.value
            val apiCBAccountList: List<ApiCoinbaseAccount> = gson.fromJson(apiCBAccountString, object : TypeToken<List<ApiCoinbaseAccount>>() {}.type)
            val relevantAccount = apiCBAccountList.find { cbAccount -> cbAccount.currency == currency.toString() }
            if (relevantAccount != null) {
                val balance = relevantAccount.balance.toDoubleOrZero()
                GdaxApi.getFromCoinbase(balance, currency, relevantAccount.id).executePost( { result ->
                    println("failure")
                }, { result ->
                    println("success")
                } )
            }
        })
    }
}
