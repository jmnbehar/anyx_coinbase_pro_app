package com.jmnbehar.anyx.Classes

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jmnbehar.anyx.Activities.MainActivity


/**
 * Created by jmnbehar on 12/20/2017.
 */

//TODO: delete this object and portion everything out to gdax api or classes

object TransferHub {
    fun linkCoinbaseAccounts(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        GdaxApi.coinbaseAccounts().get(onFailure) { coinbaseAccounts ->
            for (cbAccount in coinbaseAccounts) {
                val currency = Currency.forString(cbAccount.currency)
                if (currency != null && cbAccount.active) {
                    val account = Account.forCurrency(currency)
                    account?.coinbaseAccount = Account.CoinbaseAccount(cbAccount)
                }
            }
            onComplete()
        }
    }

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
    fun sweepIntoCoinbaseAccount(currency: Currency) {
        GdaxApi.coinbaseAccounts().executeRequest({ println("failure") } , { result->
            println("result")
            val gson = Gson()
            val apiCBAccountString = result.value
            val apiCBAccountList: List<ApiCoinbaseAccount> = gson.fromJson(apiCBAccountString, object : TypeToken<List<ApiCoinbaseAccount>>() {}.type)
            val relevantAccount = apiCBAccountList.find { cbAccount -> cbAccount.currency == currency.toString() }
            if (relevantAccount != null) {
                val balance = relevantAccount.balance.toDoubleOrZero()
                GdaxApi.sendToCoinbase(balance, currency, relevantAccount.id).executePost( { result ->
                    println("failure")
                }, { result ->
                    println("success")
                } )
            }
        })
    }
    fun sendToCoinbase(amount: Double, currency: Currency, onFailure: (String?) -> Unit, onComplete: () -> Unit) {
        val relevantAccount = Account.forCurrency(currency)
        val cbAccount = relevantAccount?.coinbaseAccount
        if (cbAccount != null) {
            GdaxApi.sendToCoinbase(amount, currency, cbAccount.id).executePost( { result ->
                onFailure(result.error.message)
            }, {
                onComplete()
            } )
        } else {
            onFailure("Coinbase accounts could not be accessed")
        }
    }

    fun getFromCoinbase(amount: Double, currency: Currency, onFailure: (String?) -> Unit, onComplete: () -> Unit) {
        val relevantAccount = Account.forCurrency(currency)
        val cbAccount = relevantAccount?.coinbaseAccount
        if (cbAccount != null) {
            GdaxApi.sendToCoinbase(amount, currency, cbAccount.id).executePost( { result ->
                onFailure(result.error.message)
            }, {
                onComplete()
            } )
        } else {
            onFailure("Coinbase account could not be accessed")
        }
    }

    fun getFromPayment(amount: Double, currency: Currency,  onFailure: (result: Result.Failure<ByteArray, FuelError>) -> Unit, onComplete: () -> Unit) {
        GdaxApi.paymentMethods().executeRequest({ println("failure") } , { result->
            println("result")
            val gson = Gson()
            val apiPaymentMethodsString = result.value
            val apiPaymentMethodList: List<ApiPaymentMethod> = gson.fromJson(apiPaymentMethodsString, object : TypeToken<List<ApiPaymentMethod>>() {}.type)
            val relevantPaymentMethod = apiPaymentMethodList.find { paymentMethod ->
                        paymentMethod.currency == currency.toString() && paymentMethod.allow_withdraw && paymentMethod.primary_buy
            } ?: apiPaymentMethodList.find { paymentMethod ->
                paymentMethod.currency == currency.toString() && paymentMethod.allow_withdraw
            }
            if (relevantPaymentMethod != null) {
                GdaxApi.getFromPayment(amount, currency, relevantPaymentMethod.id).executePost( onFailure, {
                    onComplete()
                } )
            }
        })
    }
    fun sendToPayment(amount: Double, currency: Currency, onFailure: (result: Result.Failure<ByteArray, FuelError>) -> Unit, onComplete: () -> Unit) {
        GdaxApi.coinbaseAccounts().executeRequest({ println("failure") } , { result->
            println("result")
            val gson = Gson()
            val apiCBAccountString = result.value
            val apiPaymentMethodList: List<ApiPaymentMethod> = gson.fromJson(apiCBAccountString, object : TypeToken<List<ApiPaymentMethod>>() {}.type)
            val relevantPaymentMethod = apiPaymentMethodList.find { paymentMethod ->
                paymentMethod.currency == currency.toString() && paymentMethod.allow_deposit && paymentMethod.primary_sell
            } ?: apiPaymentMethodList.find { paymentMethod ->
                paymentMethod.currency == currency.toString() && paymentMethod.allow_deposit
            }
            if (relevantPaymentMethod != null) {
                GdaxApi.getFromPayment(amount, currency, relevantPaymentMethod.id).executePost( onFailure, {
                    onComplete()
                } )
            }
        })
    }


//    fun sendCrypto(amount: Double, currency: Currency) {
//        GdaxApi.sendCrypto(amount, currency, )
//        GdaxApi.coinbaseAccounts().executeRequest({ println("failure") } , { result->
//            println("result")
//            val gson = Gson()
//            val apiCBAccountString = result.value
//            val apiPaymentMethodList: List<ApiPaymentMethod> = gson.fromJson(apiCBAccountString, object : TypeToken<List<ApiPaymentMethod>>() {}.type)
//            val relevantPaymentMethod = apiPaymentMethodList.find { paymentMethod ->
//                paymentMethod.currency == currency.toString() && paymentMethod.allow_deposit && paymentMethod.primary_sell
//            } ?: apiPaymentMethodList.find { paymentMethod ->
//                paymentMethod.currency == currency.toString() && paymentMethod.allow_deposit
//            }
//            if (relevantPaymentMethod != null) {
//                GdaxApi.getFromPayment(amount, currency, relevantPaymentMethod.id).executePost( { result ->
//                    println("failure")
//                }, { result ->
//                    println("success")
//                } )
//            }
//        })
//    }
}
