package com.jmnbehar.gdax.Classes

import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.list_row_account.view.*

/**
 * Created by jmnbehar on 12/20/2017.
 */


class Account(val product: Product, apiAccount: ApiAccount) {
    var balance: Double = apiAccount.balance.toDoubleOrZero()
    var value: Double
    var id: String
    var currency = product.currency
        get() = product.currency

    init {
        value = product.price * balance
        id = apiAccount.id
    }

    fun updateAccount(balance: Double, price: Double) {
        product.price = price
        this.balance = balance
        value = product.price * balance
        updateInList()
    }

    fun updateInList() {
        list.remove(forCurrency(currency))
        if (value != null) {
            list.add(this)
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
                            } else if (currency == Currency.USD) {
                                usdAccount = Account(Product.fiatProduct(currency.toString()), apiAccount)
                            }
                        }
                        onComplete()
                    }
                }
            }
        }
    }
}
