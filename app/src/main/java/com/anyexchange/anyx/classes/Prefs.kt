package com.anyexchange.anyx.classes

import android.content.Context
import android.content.SharedPreferences
import com.anyexchange.anyx.api.CBProApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import se.simbio.encryption.Encryption
import java.util.*

/**
 * Created by josephbehar on 12/28/17.
 */

//Never change these strings:
private const val FILE_NAME = "com.anyexchange.gdax.prefs"  //do not rename

private const val PASSPHRASE = "cbProPassphrase"
private const val API_KEY = "api_key"
private const val API_SECRET = "api_secret"

private const val CBPRO_API_KEY = "cb_pro_api_key"
private const val CBPRO_API_SECRET = "cb_pro_api_secret"
private const val CBPRO_PASSPHRASE = "cbProPassphrase"

private const val BINANCE_API_KEY = "binance_api_key"
private const val BINANCE_API_SECRET = "binance_api_secret"

private const val SHOULD_SAVE_API_INFO = "save_api_info"
private const val SHOULD_SAVE_PASSPHRASE = "save_passphrase"
private const val ALERTS = "alerts"
private const val SHOULD_SHOW_TRADE_CONFIRM = "show_trade_confirm"
private const val SHOULD_SHOW_SEND_CONFIRM = "show_send_confirm"
private const val ARE_ALERT_FILLS_ON = "ARE_ALERT_FILLS_ON"
private const val STASHED_PRODUCTS = "stashed_products"
private const val STASHED_ORDERS = "stashed_orders"
private const val STASHED_FILLS = "stashed_fills"
private const val STASHED_FILLS_DATE = "stashed_fills_date"
private const val STASHED_ORDERS_DATE = "stashed_orders_date"
private const val DARK_MODE = "dark_mode"
private const val IS_FIRST_TIME = "is_first_time"
private const val IS_LOGGED_IN = "is_logged_in"
private const val UNPAID_FEES = "unpaid_fees_"
private const val APPROVED_API_KEYS = "approved_api_keys"
private const val REJECTED_API_KEYS = "rejected_api_keys"
private const val QUICK_CHANGE_ALERTS_ACTIVE = "rapid_price_movement"
private const val DEFAULT_QUOTE = "DEFAULT_QUOTE"
private const val QUICK_CHANGE_ALERT_TIME = "QUICK_CHANGE_ALERT_TIME"
private const val SORT_FAVORITES_ALPHABETICAL = "SORT_FAVORITES_ALPHABETICAL"
private const val LAST_VERSION_CODE = "LAST_VERSION_CODE"

private const val QUICK_CHANGE_THRESHOLD = "QUICK_CHANGE_THRESHOLD"

private const val PAYMENT_METHODS = "PAYMENT_METHODS"
private const val PRODUCT = "account_product_"
private const val ACCOUNT = "account_raw_"

@Suppress("LiftReturnOrAssignment")
class Prefs (var context: Context) {


    private val prefs: SharedPreferences = context.getSharedPreferences(FILE_NAME, 0)

    var isFirstTime: Boolean
        get() = prefs.getBoolean(IS_FIRST_TIME, true)
        set(value) = prefs.edit().putBoolean(IS_FIRST_TIME, value).apply()

    var cbProApiKey: String?
        get() = prefs.getString(API_KEY, null)
        set(value) {
            prefs.edit().putString(API_KEY, value).apply()
            prefs.edit().putString(CBPRO_API_KEY, value).apply()
        }

    var cbProApiSecret: String?
        get() = prefs.getString(API_SECRET, null)
        set(value) {
            prefs.edit().putString(API_SECRET, value).apply()
            prefs.edit().putString(CBPRO_API_SECRET, value).apply()
        }

    var cbProPassphrase: String?
        get() = prefs.getString(PASSPHRASE, null)
        set(value) {
            prefs.edit().putString(PASSPHRASE, value).apply()
            prefs.edit().putString(CBPRO_PASSPHRASE, value).apply()
        }

    fun stashCBProCreds(apiKey: String, apiSecret: String, apiPassphrase: String) {
        if (shouldSaveApiInfo) {
            val iv = ByteArray(16)
            val encryption = Encryption.getDefault(apiKey, apiSecret + Constants.salt, iv)
            val passphraseEncrypted = encryption.encryptOrNull(apiPassphrase)
            cbProApiKey = apiKey
            cbProApiSecret = apiSecret
            if (shouldSavePassphrase) {
                cbProPassphrase = passphraseEncrypted
            }
        }
    }


    var binanceApiKey: String?
        get() = prefs.getString(BINANCE_API_KEY, null)
        set(value) = prefs.edit().putString(BINANCE_API_KEY, value).apply()

    var binanceApiSecret: String?
        get() = prefs.getString(BINANCE_API_SECRET, null)
        set(value) = prefs.edit().putString(BINANCE_API_SECRET, value).apply()

    fun stashBinanceCreds(apiKey: String, apiSecret: String) {
        if (shouldSaveApiInfo) {
            binanceApiKey = apiKey
            binanceApiSecret = apiSecret
        }
    }

    var shouldShowTradeConfirmModal: Boolean
        get() = prefs.getBoolean(SHOULD_SHOW_TRADE_CONFIRM, true)
        set(value) = prefs.edit().putBoolean(SHOULD_SHOW_TRADE_CONFIRM, value).apply()

    var shouldShowSendConfirmModal: Boolean
        get() = prefs.getBoolean(SHOULD_SHOW_SEND_CONFIRM, true)
        set(value) = prefs.edit().putBoolean(SHOULD_SHOW_SEND_CONFIRM, value).apply()

    var isDarkModeOn: Boolean
        get() = prefs.getBoolean(DARK_MODE, true)
        set(value) = prefs.edit().putBoolean(DARK_MODE, value).apply()

    var shouldSaveApiInfo: Boolean
        get() = prefs.getBoolean(SHOULD_SAVE_API_INFO, true)
        set(value) {
            prefs.edit().putBoolean(SHOULD_SAVE_API_INFO, value).apply()
            if (!value) {
                shouldSavePassphrase = false
            }
        }

    var shouldSavePassphrase: Boolean
        get() = prefs.getBoolean(SHOULD_SAVE_PASSPHRASE, true)
        set(value) = prefs.edit().putBoolean(SHOULD_SAVE_PASSPHRASE, value).apply()

    var alerts: Set<PriceAlert>
        get() = prefs.getStringSet(ALERTS, setOf<String>())?.asSequence()?.map { s -> PriceAlert.forString(s) }?.toSet() ?: setOf()
        set(value) = prefs.edit().putStringSet(ALERTS, value.asSequence().map { a -> a.toString() }.toSet()).apply()

    //TODO: change this to a hash map:
    var stashedProducts: List<Product>
        get() = prefs.getStringSet(STASHED_PRODUCTS, setOf<String>())?.mapNotNull {
            try {
                Gson().fromJson(it, Product::class.java)
            } catch (e: Exception) {
                null
            }
        } ?: listOf()
        set(value) {
            val jsonValues = value.asSequence().mapNotNull { Gson().toJson(it) }.toSet()
            prefs.edit().putStringSet(STASHED_PRODUCTS, jsonValues).apply()
        }

    fun stashProducts() {
        stashedProducts = Product.map.values.toList()
        stashedFiatAccountList = Account.fiatAccounts
        stashedPaymentMethodList = Account.paymentMethods

    }


    //For now assume all fiat accounts are CBPro
    var stashedFiatAccountList: List<Account>
        get() {
            val gson = Gson()
            val newAccountList = mutableListOf<Account>()
            val fiatAndStableCoins = Currency.fiatList.asSequence().plus(Currency.stableCoinList).toList()
            for (currency in fiatAndStableCoins) {
                val accountString = prefs.getString(ACCOUNT + currency.toString(), "")
                if (accountString?.isNotBlank() == true) {
                    try {
                        val newAccount = gson.fromJson(accountString, Account::class.java)
                        newAccountList.add(newAccount)
                    } catch (e: Exception) {
                        return newAccountList
                    }
                }
            }
            return newAccountList
        }
        set(value) {
            if (value.isEmpty()) {
                for (currency in Currency.fiatList) {
                    prefs.edit().putString(ACCOUNT + currency.toString(), null).apply()
                }
            } else {
                val gson = Gson()
                for (account in value) {
                    val accountJson = gson.toJson(account) ?: ""
                    prefs.edit().putString(ACCOUNT + account.currency.toString(), accountJson).apply()
                }
            }
        }

    var stashedPaymentMethodList: List<Account.PaymentMethod>
        get() {
            val gson = Gson()
            val paymentMethodList = mutableListOf<Account.PaymentMethod>()

            val paymentMethodJsons: Set<String> = prefs.getStringSet(PAYMENT_METHODS, setOf()) ?: setOf()
            for (paymentMethodJson in paymentMethodJsons) {
                if (paymentMethodJson.isNotBlank()) {
                    try {
                        val paymentMethod = gson.fromJson(paymentMethodJson, Account.PaymentMethod::class.java)
                        paymentMethodList.add(paymentMethod)
                    } catch (e: Exception) {  }
                }
            }
            return paymentMethodList
        }
        set(value) {
            val gson = Gson()
            val paymentMethodJsons = value.map { gson.toJson(it) }.toSet()
            prefs.edit().putStringSet(PAYMENT_METHODS, paymentMethodJsons).apply()
        }

    var lastQuickChangeAlertTimestamp: Long
        get() = prefs.getLong(QUICK_CHANGE_ALERT_TIME, 0)
        set(value) = prefs.edit().putLong(QUICK_CHANGE_ALERT_TIME, value).apply()

    var defaultQuoteCurrency: Currency
        get() {
            val currencyStr = prefs.getString(DEFAULT_QUOTE, Currency.USD.id) ?: Currency.USD.id
            return Currency(currencyStr)
        }
        set(value) = prefs.edit().putString(DEFAULT_QUOTE, value.id).apply()

    var quickChangeAlertCurrencies: Set<String>
        get() = prefs.getStringSet(QUICK_CHANGE_ALERTS_ACTIVE, setOf<String>()) ?: setOf()
        set(value) = prefs.edit().putStringSet(QUICK_CHANGE_ALERTS_ACTIVE, value).apply()

    var quickChangeThreshold: Float
        get() = prefs.getFloat(QUICK_CHANGE_THRESHOLD, 2.0f)
        set(value) = prefs.edit().putFloat(QUICK_CHANGE_THRESHOLD, value).apply()

    // someday this will actually work
    val isAnyXProActive: Boolean
        get() = false
//        get() = prefs.getFloat(QUICK_CHANGE_THRESHOLD, false)
//        set(value) = prefs.edit().putFloat(QUICK_CHANGE_THRESHOLD, false).apply()

    val isVerified: Boolean?
        get() {
            return if (isAnyXProActive) {
                true
            } else {
                CBProApi.credentials?.isVerified
            }
        }

    fun setQuickChangeAlertActive(currency: Currency, isActive: Boolean) {
        val currentActiveAlerts = quickChangeAlertCurrencies.toMutableSet()
        if (isActive && !quickChangeAlertCurrencies.contains(currency.id)) {
            currentActiveAlerts.add(currency.id)
        } else if (!isActive && quickChangeAlertCurrencies.contains(currency.id)) {
            currentActiveAlerts.remove(currency.id)
        }
        quickChangeAlertCurrencies = currentActiveAlerts
    }
    fun isQuickChangeAlertActive(currency: Currency): Boolean {
        return quickChangeAlertCurrencies.contains(currency.id)
    }

    fun addUnpaidFee(unpaidFee: Double, currency: Currency): Double {
        /* Keeps track of unpaid fees to be paid once over the min send amount */
        var totalUnpaidFees = prefs.getFloat(UNPAID_FEES + currency.toString(), 0.0f)
        totalUnpaidFees += unpaidFee.toFloat()
        prefs.edit().putFloat(UNPAID_FEES + currency.toString(), totalUnpaidFees).apply()
        return totalUnpaidFees.toDouble()
    }

    fun wipeUnpaidFees(currency: Currency) {
        prefs.edit().putFloat(UNPAID_FEES + currency.toString(), 0.0f).apply()
    }

    fun stashOrders(orderList: List<Order>?, exchange: Exchange) {
        val orderListString = Gson().toJson(orderList)
        val stashDate = if (orderList == null) { 0 } else { Date().time }
        prefs.edit().putString(STASHED_ORDERS + exchange.name, orderListString)
                .putLong(STASHED_ORDERS_DATE + exchange.name, stashDate).apply()
    }
    fun nukeStashedOrders() {
        for (exchange in Exchange.values()) {
            prefs.edit().remove(STASHED_FILLS + exchange.name).apply()
        }
    }

    fun getStashedOrders(baseCurrency: Currency, exchange: Exchange) : List<Order> {
        val apiOrdersJson = prefs.getString(STASHED_ORDERS + exchange.name, null)
        return try {
            val apiOrderList: List<Order> = Gson().fromJson(apiOrdersJson, object : TypeToken<List<Order>>() {}.type)
            val filteredOrders = apiOrderList.filter { it.tradingPair.baseCurrency == baseCurrency && it.exchange == exchange }
            filteredOrders
        } catch (e: Exception) {
            listOf()
        }
    }
    fun getStashedOrders(baseCurrency: Currency) : List<Order> {
        val orderList = mutableListOf<Order>()
        for (exchange in Exchange.values()) {
            val apiOrdersJson = prefs.getString(STASHED_ORDERS + exchange.name, null)
            try {
                val apiOrderList: List<Order> = Gson().fromJson(apiOrdersJson, object : TypeToken<List<Order>>() {}.type)
                val filteredOrders = apiOrderList.filter { it.tradingPair.baseCurrency == baseCurrency}
                orderList.addAll(filteredOrders)
            } catch (e: Exception) { }
        }
        return orderList
    }

    fun getDateOrdersLastStashed(exchange: Exchange): Long {
        return prefs.getLong(STASHED_FILLS_DATE + exchange.name, 0)
    }

    fun stashFills(fillList: List<Fill>, tradingPair: TradingPair, exchange: Exchange) {
        val fillListJson = Gson().toJson(fillList)
        prefs.edit().putString(STASHED_FILLS + exchange.toString() + tradingPair.idForExchange(exchange), fillListJson)
                    .putLong(STASHED_FILLS_DATE + exchange.toString() + tradingPair.idForExchange(exchange), Date().time).apply()
    }

    fun nukeStashedFills() {
        for (product in Product.map.values) {
            for (exchange in Exchange.values()) {
                for (tradingPair in product.tradingPairs) {
                    prefs.edit().remove(STASHED_FILLS + exchange.toString() + tradingPair.idForExchange(exchange)).apply()
                }
            }
        }
    }
    fun getStashedFills(tradingPair: TradingPair, exchange: Exchange) : List<Fill> {
        val fillListJson = prefs.getString(STASHED_FILLS + exchange + tradingPair.idForExchange(exchange), null)
        return try {
            val apiFillList: List<Fill> = Gson().fromJson(fillListJson, object : TypeToken<List<Fill>>() {}.type)
            apiFillList.filter { it.tradingPair == tradingPair }
        } catch (e: Exception) {
            listOf()
        }
    }
    fun getStashedFills(baseCurrency: Currency) : List<Fill> {
        val tradingPairs = Product.map[baseCurrency.id]?.tradingPairs ?: listOf()
        val fillList = mutableListOf<Fill>()
        for (tradingPair in tradingPairs) {
            val fillListJson = prefs.getString(STASHED_FILLS + tradingPair.exchange + tradingPair.idForExchange(tradingPair.exchange), null)
            try {
                val partialFillList: List<Fill> = Gson().fromJson(fillListJson, object : TypeToken<List<Fill>>() {}.type)
                fillList.addAll(partialFillList)
            } catch (e: Exception) {
                //do nothing
            }
        }
        return fillList
    }
    fun getDateFillsLastStashed(tradingPair: TradingPair, exchange: Exchange): Long {
        return prefs.getLong(STASHED_FILLS_DATE + exchange.toString() + tradingPair.idForExchange(exchange), 0)
    }

    var areFillAlertsActive: Boolean
        get() = prefs.getBoolean(ARE_ALERT_FILLS_ON, true)
        set(value) = prefs.edit().putBoolean(ARE_ALERT_FILLS_ON, value).apply()

    var sortFavoritesAlphabetical: Boolean
        get() = prefs.getBoolean(SORT_FAVORITES_ALPHABETICAL, false)
        set(value) = prefs.edit().putBoolean(SORT_FAVORITES_ALPHABETICAL, value).apply()

    var lastVersionCode: Int
        get() = prefs.getInt(LAST_VERSION_CODE, 0)
        set(value) = prefs.edit().putInt(LAST_VERSION_CODE, value).apply()

    fun isApiKeyValid(apiKey: String) : Boolean? {
        val approvedApiKeys = prefs.getStringSet(APPROVED_API_KEYS, setOf<String>())?.toMutableSet() ?: mutableSetOf()
        val rejectedApiKeys = prefs.getStringSet(REJECTED_API_KEYS, setOf<String>())?.toMutableSet() ?: mutableSetOf()
        when {
            approvedApiKeys.contains(apiKey) -> return true
            rejectedApiKeys.contains(apiKey) -> return false
            else -> return null
        }
    }
    fun approveApiKey(apiKey: String) {
        val apiKeys = prefs.getStringSet(APPROVED_API_KEYS, setOf<String>())?.toMutableSet() ?: mutableSetOf()
        apiKeys.add(apiKey)
        prefs.edit().putStringSet(APPROVED_API_KEYS, apiKeys).apply()
        if (CBProApi.credentials?.apiKey == apiKey) {
            CBProApi.credentials?.isVerified = true
        }
    }
    fun rejectApiKey(apiKey: String) {
        val apiKeys = prefs.getStringSet(REJECTED_API_KEYS, setOf<String>())?.toMutableSet() ?: mutableSetOf()
        apiKeys.add(apiKey)
        prefs.edit().putStringSet(REJECTED_API_KEYS, apiKeys).apply()
        if (CBProApi.credentials?.apiKey == apiKey) {
            CBProApi.credentials?.isVerified = false
        }
    }

    fun addAlert(alert: PriceAlert) {
        val tempAlerts = alerts.toMutableSet()
        tempAlerts.add(alert)
        alerts = tempAlerts.toSet()
    }

    fun removeAlert(alert: PriceAlert) {
        val tempAlerts = alerts.toMutableSet()
        tempAlerts.removeAlert(alert)
        alerts = tempAlerts.toSet()
    }
}

