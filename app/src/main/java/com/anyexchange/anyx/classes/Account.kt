package com.anyexchange.anyx.classes


import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import kotlinx.android.parcel.IgnoredOnParcel

/**
 * Created by anyexchange on 12/20/2017.
 */
class Account(var product: Product, var apiAccount: ApiAccount) {
    val balance: Double
        get() = apiAccount.balance.toDoubleOrZero()

    val availableBalance: Double
        get() {
//        val holds = apiAccount.holds.toDoubleOrZero()
//        return balance - holds
            return apiAccount.available.toDoubleOrZero()
        }

    fun valueForTradingPair(tradingPair: TradingPair?): Double {
        return balance * product.priceForTradingPair(tradingPair)
    }
    val defaultValue: Double
        get() = balance * product.defaultPrice

    val fiatValue: Double
        get() = balance * (product.defaultDayCandles.lastOrNull()?.close ?: product.defaultPrice)

    val id: String
        get() = apiAccount.id

    val currency: Currency
        get() = product.currency

    @IgnoredOnParcel
    var coinbaseAccount: CoinbaseAccount? = null

    fun update(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        CBProApi.account(id).get(onFailure) { apiAccount ->
            if (apiAccount != null) {
                this.apiAccount = apiAccount
            }
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

        var fiatAccount: Account? = null
        val fiatCurrency = fiatAccount?.currency ?: Currency.USD

        var totalValue: Double = 0.0
            get() = Account.list.map { a -> a.defaultValue }.sum() + (Account.fiatAccount?.defaultValue ?: 0.0)

        fun forCurrency(currency: Currency): Account? {
            return if (currency.isFiat) {
                fiatAccount
            } else {
                list.find { a -> a.product.currency == currency }
            }
        }

        fun updateAllAccountsCandles(onFailure: (Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
            var candlesUpdated = 0
            for (account in list) {
                val tradingPair = TradingPair(account.product.id)
                Product.updateAllProducts(onFailure) {
                    account.product.updateCandles(Timespan.DAY, tradingPair, onFailure) {
                        candlesUpdated++
                        if (candlesUpdated == list.size) {
                            onComplete()
                        }
                    }
                }
            }
        }
    }

    abstract class RelatedAccount {
        abstract val id: String
        abstract val balance: Double?
        abstract val currency: Currency
    }

    class CoinbaseAccount(apiCoinbaseAccount: ApiCoinbaseAccount) : RelatedAccount() {
        override val id: String = apiCoinbaseAccount.id
        override val balance: Double = apiCoinbaseAccount.balance.toDoubleOrZero()
        override val currency = Currency.forString(apiCoinbaseAccount.currency) ?: Currency.USD

        override fun toString(): String {
            //TODO: use string resources
            return if (currency.isFiat) {
                "Coinbase $currency Balance: ${balance.fiatFormat(fiatCurrency)}"
            } else {
                "Coinbase $currency Balance: ${balance.btcFormatShortened()} $currency"
            }
        }
    }

    class PaymentMethod(val apiPaymentMethod: ApiPaymentMethod) : RelatedAccount() {
        override val id: String = apiPaymentMethod.id
        override val balance = apiPaymentMethod.balance?.toDoubleOrNull()
        override val currency = Currency.forString(apiPaymentMethod.currency) ?: Currency.USD

        override fun toString(): String {
            return apiPaymentMethod.name
        }
    }
}
