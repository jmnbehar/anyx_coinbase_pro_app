package com.jmnbehar.anyx.Classes

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
    var availableBalance: Double = balance

    var value: Double = 0.0
        get() = balance * product.price

    var id: String
    var currency = product.currency
        get() = product.currency

    var coinbaseAccount: CoinbaseAccount? = null

    init {
        value = product.price * balance
        id = apiAccount.id
        availableBalance = apiAccount.available.toDoubleOrZero()
    }

    fun update(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        GdaxApi.account(id).executeRequest(onFailure) { result ->
            val apiAccount: ApiAccount = Gson().fromJson(result.value, object : TypeToken<ApiAccount>() {}.type)
            balance = apiAccount.balance.toDoubleOrZero()
            availableBalance = apiAccount.available.toDoubleOrZero()
            onComplete()
        }
    }


    companion object {
        var list = mutableListOf<Account>()
        val btcAccount: Account?
            get() = forCurrency(Currency.BTC)

        val ltcAccount: Account?
            get() = forCurrency(Currency.LTC)

        val ethAccount: Account?
            get() = forCurrency(Currency.ETH)

        val bchAccount: Account?
            get() = forCurrency(Currency.BCH)

        var usdAccount: Account? = null

        //TODO: use this
        var totalValue: Double = 0.0
            get() = Account.list.map { a -> a.value }.sum() + (Account.usdAccount?.value ?: 0.0)

        fun forCurrency(currency: Currency): Account? {
            return if (currency == Currency.USD) {
                usdAccount
            } else {
                list.find { a -> a.product.currency == currency }
            }
        }
    }

    class CoinbaseAccount(apiCoinbaseAccount: ApiCoinbaseAccount) {
        val id: String = apiCoinbaseAccount.id
        val balance: Double = apiCoinbaseAccount.balance.toDoubleOrZero()
        val currency = Currency.forString(apiCoinbaseAccount.currency) ?: Currency.USD

        override fun toString(): String {
            return if (currency.isFiat) {
                "Coinbase $currency Wallet: ${balance.fiatFormat()}"
            } else {
                "Coinbase $currency Wallet: ${balance.btcFormatShortened()} $currency"
            }
        }
    }
}
