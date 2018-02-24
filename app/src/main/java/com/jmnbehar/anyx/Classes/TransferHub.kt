package com.jmnbehar.anyx.Classes

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
    fun getFromPayment(amount: Double, currency: Currency) {
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
                GdaxApi.getFromPayment(amount, currency, relevantPaymentMethod.id).executePost( { result ->
                    println("failure")
                }, { result ->
                    println("success")
                } )
            }
        })
    }
    fun sendToPayment(amount: Double, currency: Currency) {
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
                GdaxApi.getFromPayment(amount, currency, relevantPaymentMethod.id).executePost( { result ->
                    println("failure")
                }, { result ->
                    println("success")
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
