package com.jmnbehar.gdax.Classes

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

/**
 * Created by jmnbehar on 12/20/2017.
 */


class Account(val product: Product, apiAccount: ApiAccount) {
    var balance: Double = apiAccount.balance.toDoubleOrZero()
        private set(value) {}

    var value: Double
    var id: String
    var currency = product.currency
        get() = product.currency

    init {
        value = product.price * balance
        id = apiAccount.id
    }

    fun updateAccount(balance: Double = this.balance, price: Double) {
        product.price = price
        this.balance = balance
        value = product.price * balance
        updateInList()
    }

    private fun updateInList() {
        list.remove(forCurrency(currency))
        if (value != null) {
            list.add(this)
        }
    }

    //TODO: update to onfailure and onsuccess
    fun updateCandles(time: Int, onComplete: (didUpdate: Boolean) -> Unit) {
        var twoMinutesAgo = Calendar.getInstance()
        twoMinutesAgo.add(Int.MIN_VALUE, -2)
        val lastCandleUpdateTime = product.lastCandleUpdateTime.timeInSeconds()
        val twoMinutesAgoInSeconds = twoMinutesAgo.timeInSeconds()
        if (lastCandleUpdateTime > twoMinutesAgoInSeconds) {
            Candle.getCandles(product.id, time, { candleList ->
                product.lastCandleUpdateTime = Calendar.getInstance()
                product.candles = candleList
                onComplete(true)
            })
        } else {
            onComplete(false)
        }
    }

//    fun updateInfo(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
//        val onFailure = { result: Result.Failure<String, FuelError> ->  println("Error!: ${result.error}") }
//        GdaxApi.account(id).executeRequest(onFailure) { result ->
//            val apiAccount: ApiAccount = Gson().fromJson(result.value, object : TypeToken<ApiAccount>() {}.type)
//            val apiAccountBalance =  apiAccount.balance.toDoubleOrNull()
//            if (apiAccountBalance != null) {
//                balance = apiAccount.balance.toDouble()
//            }
//            updateInList()
//            onComplete()
//        }
//    }

    companion object {

        var list = mutableListOf<Account>()
        var btcAccount: Account?
            get() = forCurrency(Currency.BTC)
            set(value) {
                list.remove(btcAccount)
                if (value != null) {
                    list.add(value)
                }
            }

        var ltcAccount: Account?
            get() = forCurrency(Currency.LTC)
            set(value) {
                list.remove(ltcAccount)
                if (value != null) {
                    list.add(value)
                }
            }

        var ethAccount: Account?
            get() = forCurrency(Currency.ETH)
            set(value) {
                list.remove(ethAccount)
                if (value != null) {
                    list.add(value)
                }
            }

        var bchAccount: Account?
            get() = forCurrency(Currency.BCH)
            set(value) {
                list.remove(bchAccount)
                if (value != null) {
                    list.add(value)
                }
            }

        var usdAccount: Account? = null

        var totalBalance: Double = 0.0
            get() = Account.list.map { a -> a.value }.sum() + (Account.usdAccount?.value ?: 0.0)

        fun forCurrency(currency: Currency): Account? {
            return list.find { a -> a.product.currency == currency }
        }

        fun updateAllAccounts(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
            GdaxApi.accounts().executeRequest(onFailure) { result ->
                val apiAccountList: List<ApiAccount> = Gson().fromJson(result.value, object : TypeToken<List<ApiAccount>>() {}.type)
                for (account in list) {
                    val apiAccount = apiAccountList.find { a -> a.currency == account.currency.toString() }
                    val apiAccountBalance =  apiAccount?.balance?.toDoubleOrNull()
                    if (apiAccountBalance != null) {
                        account.balance = apiAccount.balance.toDouble()
                    }
                }
                onComplete()
            }
        }

        fun getAccounts(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
            list.clear()
            GdaxApi.products().executeRequest(onFailure = onFailure) { result ->
                val gson = Gson()

                val unfilteredApiProductList: List<ApiProduct> = gson.fromJson(result.value, object : TypeToken<List<ApiProduct>>() {}.type)
                val apiProductList = unfilteredApiProductList.filter { s ->
                    s.quote_currency == "USD"
                }
                val time = TimeInSeconds.oneDay
                var productList: MutableList<Product> = mutableListOf()
                for (product in apiProductList) {
                    Candle.getCandles(product.id, time, { candleList ->
                        val newProduct = Product(product, candleList)
                        productList.add(newProduct)
                        if (productList.size == apiProductList.size) {
                            GdaxApi.accounts().executeRequest(onFailure) { result ->
                                val apiAccountList: List<ApiAccount> = Gson().fromJson(result.value, object : TypeToken<List<ApiAccount>>() {}.type)
                                for (apiAccount in apiAccountList) {
                                    val currency = Currency.fromString(apiAccount.currency)
                                    val relevantProduct = productList.find { p -> p.currency == currency }
                                    if (relevantProduct != null) {
                                        list.add(Account(relevantProduct, apiAccount))
                                    } else if (currency == Currency.USD) {
                                        usdAccount = Account(Product.fiatProduct(currency.toString()), apiAccount)
                                    }
                                }
                                onComplete()
                            }
                        }
                    })
                }
            }
        }
    }
}
