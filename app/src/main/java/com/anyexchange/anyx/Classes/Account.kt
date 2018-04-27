package com.anyexchange.anyx.Classes


import android.annotation.SuppressLint
import android.os.Parcelable
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

/**
 * Created by anyexchange on 12/20/2017.
 */


@SuppressLint("ParcelCreator")
@Parcelize
class Account(val product: Product, var apiAccount: ApiAccount) : Parcelable {
    val balance: Double
        get() = apiAccount.balance.toDoubleOrZero()

    val availableBalance: Double
        get() {
//        val holds = apiAccount.holds.toDoubleOrZero()
//        return balance - holds
            return apiAccount.available.toDoubleOrZero()
        }

    val value: Double
        get() = balance * product.price

    val id: String
        get() = apiAccount.id

    val currency: Currency
        get() = product.currency


    @IgnoredOnParcel
    var coinbaseAccount: CoinbaseAccount? = null

    fun update(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        GdaxApi.account(id).executeRequest(onFailure) { result ->
            val apiAccount: ApiAccount = Gson().fromJson(result.value, object : TypeToken<ApiAccount>() {}.type)
            this.apiAccount = apiAccount
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
            return if (currency.isFiat) {
                "Coinbase $currency Balance: ${balance.fiatFormat()}"
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
            return "Payment Method: ${apiPaymentMethod.name}"
        }
    }
}
