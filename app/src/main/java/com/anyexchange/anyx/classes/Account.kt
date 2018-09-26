package com.anyexchange.anyx.classes


import com.anyexchange.anyx.classes.APIs.*
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result

/**
 * Created by anyexchange on 12/20/2017.
 */
class Account(var exchange: Exchange, override val currency: Currency, override var id: String, override var balance: Double, var holds: Double): BaseAccount() {
    constructor(apiAccount: CBProAccount) : this(Exchange.CBPro, Currency(apiAccount.currency), apiAccount.id, apiAccount.balance.toDoubleOrZero(), apiAccount.holds.toDoubleOrZero())
    constructor(apiAccount: BinanceBalance) : this(Exchange.Binance, Currency(apiAccount.asset), apiAccount.asset, apiAccount.free + apiAccount.locked, apiAccount.locked)

    fun updateWithApiAccount(apiAccount: CBProAccount) {
        assert(exchange == Exchange.CBPro)
        id = apiAccount.id
        balance = apiAccount.balance.toDoubleOrZero()
        holds = apiAccount.holds.toDoubleOrZero()
    }
    fun updateWithApiAccount(binanceBalance: BinanceBalance) {
        assert(exchange == Exchange.Binance)
        id = binanceBalance.asset
        balance = binanceBalance.free + binanceBalance.locked
        holds = binanceBalance.locked
    }
    val availableBalance: Double
        get() {
            return balance - holds
        }

    fun valueForQuoteCurrency(quoteCurrency: Currency) : Double {
        return balance * product.priceForQuoteCurrency(quoteCurrency)
    }
    val defaultValue: Double
        get() = balance * product.defaultPrice


    val product: Product
        get() {
            //TODO: if product is null, get product
            return Product.hashMap[currency]!!
        }

    var coinbaseAccount: CoinbaseAccount? = null

    var depositInfo: CBProDepositAddress? = null

    fun update(apiInitData: ApiInitData?, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: () -> Unit) {
        AnyApi.updateAccount(apiInitData, this, onFailure)  {
            onSuccess()
        }
    }

    override fun toString(): String {
        val cbproAccountBalanceString =  if (currency.isFiat) {
            balance.fiatFormat(Account.defaultFiatCurrency)
        } else {
            "${balance.btcFormatShortened()} $currency"
        }
        return "Coinbase Pro $currency Balance: $cbproAccountBalanceString"
    }

    companion object {
        var cryptoAccounts = listOf<Account>()
        var fiatAccounts = listOf<Account>()

        val areAccountsOutOfDate: Boolean
            get() {
                val areAccountsMissing = Account.cryptoAccounts.size < Currency.cryptoList.size || Account.fiatAccounts.isEmpty()
                val areAccountsUnidentified = Account.cryptoAccounts.find { it.exchange == Exchange.CBPro && ( it.currency.knownCurrency == KnownCurrency.USD || it.currency.knownCurrency == KnownCurrency.OTHER ) } != null
                val tradingPairs = Account.cryptoAccounts.firstOrNull()?.product?.tradingPairs
                val areTradingPairsDuplicates = (tradingPairs?.distinct()?.size ?: 0) < (tradingPairs?.size ?: 1)
                return areAccountsMissing || areAccountsUnidentified || areTradingPairsDuplicates
            }

        //TODO: stash this
        var paymentMethods: List<Account.PaymentMethod> = listOf()

        //TODO: make this changeable
        val defaultFiatAccount: Account?
            get() = fiatAccounts.sortedBy { it.balance }.lastOrNull()

        val defaultFiatCurrency: Currency
            get() = defaultFiatAccount?.currency ?: Currency.USD

        val dummyAccount = Account(Exchange.CBPro, Currency.USD, Currency.USD.toString(), 0.0, 0.0)

        var totalValue: Double = 0.0
            get() = Account.cryptoAccounts.map { a -> a.defaultValue }.sum() + Account.fiatAccounts.map { a -> a.defaultValue }.sum()

        fun forCurrency(currency: Currency): Account? {
            return if (currency.isFiat) {
                fiatAccounts.find { a -> a.product.currency == currency }
            } else {
                cryptoAccounts.find { a -> a.product.currency == currency }
            }
        }

        fun updateAllAccountsCandles(apiInitData: ApiInitData?, onFailure: (Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
            var candlesUpdated = 0
            for (account in cryptoAccounts) {
                val tradingPair = account.product.defaultTradingPair
                //TODO: do we really need to call updateAllProducts here?
                account.product.updateCandles(Timespan.DAY, tradingPair, apiInitData, onFailure) { didUpdate ->
                    candlesUpdated++
                    if (candlesUpdated == cryptoAccounts.size) {
                        if (didUpdate && apiInitData?.context != null) {
                            Prefs(apiInitData.context).stashedCBProCryptoAccountList = Account.cryptoAccounts
                        }
                        onComplete()
                    }
                }
            }
            if (cryptoAccounts.isEmpty()) {
                onComplete()
            }
        }
    }

    class CoinbaseAccount(apiCoinbaseAccount: ApiCoinbaseAccount) : BaseAccount() {
        override val id: String = apiCoinbaseAccount.id
        override val balance: Double = apiCoinbaseAccount.balance.toDoubleOrZero()
        override val currency = Currency(apiCoinbaseAccount.currency)

        override fun toString(): String {
            //TODO: use string resources
            return if (currency.isFiat) {
                "Coinbase $currency Balance: ${balance.fiatFormat(defaultFiatCurrency)}"
            } else {
                "Coinbase $currency Balance: ${balance.btcFormatShortened()} $currency"
            }
        }
    }

    class PaymentMethod(val apiPaymentMethod: CBProPaymentMethod) : BaseAccount() {
        override val id: String = apiPaymentMethod.id
        override val balance = apiPaymentMethod.balance?.toDoubleOrNull()
        override val currency = Currency(apiPaymentMethod.currency)

        override fun toString(): String {
            return apiPaymentMethod.name
        }
    }

//    class ExternalAccount(currency: Currency) : BaseAccount() {
//        override val id: String = "External $currency Account"
//        override val balance = 0.0
//        override val currency = currency
//
//        override fun toString(): String {
//            return id
//        }
//    }
}
