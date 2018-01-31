package com.jmnbehar.gdax.Classes

import android.content.Context
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

    var value: Double = 0.0
        get() = balance * product.price

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
        list.add(this)
    }

    fun updateCandles(timespan: Int, onFailure: (Result.Failure<String, FuelError>) -> Unit, onComplete: (didUpdate: Boolean) -> Unit) {
        var now = Calendar.getInstance()
        var longAgo = Calendar.getInstance()
        longAgo.add(Calendar.YEAR, -2)
        val lastCandleTime = product.candles.lastOrNull()?.time?.toInt() ?: longAgo.timeInSeconds()
        val nextCandleTime = lastCandleTime + Candle.granularityForTimespan(timespan)
        val nowInSeconds = now.timeInSeconds()

        if (nextCandleTime < nowInSeconds) {
            Candle.getCandles(product.id, timespan, onFailure, { candleList ->
                val newLastCandleTime = candleList.lastOrNull()?.time?.toInt() ?: 0.0
                val didGetNewCandle = (lastCandleTime != newLastCandleTime)
                if (didGetNewCandle) {
                    product.candles = candleList
                    product.price = candleList.last().close
                }
                onComplete(didGetNewCandle)
            })
        } else {
            onComplete(false)
        }
    }

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

        fun getAccountsWithProductList(productList: List<Product>, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
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

        fun getAccounts(context: Context, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
            list.clear()
            var prefs = Prefs(context)
            val time = TimeInSeconds.oneDay
            var productList: MutableList<Product> = mutableListOf()
            val stashedProductList = prefs.stashedProducts
            if (stashedProductList.isNotEmpty()) {
                for (product in stashedProductList) {
                    Candle.getCandles(product.id, time, onFailure, { candleList ->
                        product.candles = candleList
                        product.price = candleList.lastOrNull()?.close  ?: 0.0
                        productList.add(product)
                        if (productList.size == stashedProductList.size) {
                            getAccountsWithProductList(productList, onFailure, onComplete)
                        }
                    })
                }
            } else {
                GdaxApi.products().executeRequest(onFailure = onFailure) { result ->
                    val gson = Gson()
                    val unfilteredApiProductList: List<ApiProduct> = gson.fromJson(result.value, object : TypeToken<List<ApiProduct>>() {}.type)
                    val apiProductList = unfilteredApiProductList.filter { s ->
                        s.quote_currency == "USD"
                    }
                    for (product in apiProductList) {
                        Candle.getCandles(product.id, time, onFailure, { candleList ->
                            val newProduct = Product(product, candleList)
                            productList.add(newProduct)
                            if (productList.size == apiProductList.size) {
                                prefs.stashedProducts = productList
                                getAccountsWithProductList(productList, onFailure, onComplete)
                            }
                        })
                    }
                }
            }
        }
    }
}
