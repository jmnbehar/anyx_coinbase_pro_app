package com.anyexchange.anyx.classes

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Created by josephbehar on 12/28/17.
 */

//Never change these strings:
private const val FILE_NAME = "com.anyexchange.gdax.prefs"  //do not rename
private const val PASSPHRASE = "passphrase"
private const val API_KEY = "api_key"
private const val API_SECRET = "api_secret"
private const val SHOULD_SAVE_API_INFO = "save_api_info"
private const val SHOULD_SAVE_PASSPHRASE = "save_passphrase"
private const val ALERTS = "alerts"
private const val SHOULD_SHOW_TRADE_CONFIRM = "show_trade_confirm"
private const val SHOULD_SHOW_SEND_CONFIRM = "show_send_confirm"
private const val ARE_ALERT_FILLS_ON = "ARE_ALERT_FILLS_ON"
private const val STASHED_PRODUCTS = "stashed_products"
private const val STASHED_ORDERS = "stashed_orders"
private const val STASHED_FILLS = "stashed_fills"
private const val DARK_MODE = "dark_mode"
private const val IS_FIRST_TIME = "is_first_time"
private const val IS_LOGGED_IN = "is_logged_in"
private const val UNPAID_FEES = "unpaid_fees_"
private const val APPROVED_API_KEYS = "approved_api_keys"
private const val REJECTED_API_KEYS = "rejected_api_keys"
private const val RAPID_PRICE_MOVES = "rapid_price_movement"
private const val PREFERRED_FIAT = "preferred_fiat"
private const val MOVEMENT_ALERT_TIMESTAMP = "MOVEMENT_ALERT_TIMESTAMP"

private const val PRODUCT = "account_product_"
private const val ACCOUNT = "account_raw_"

@Suppress("LiftReturnOrAssignment")
class Prefs (var context: Context) {


    private val prefs: SharedPreferences = context.getSharedPreferences(FILE_NAME, 0)

    var isFirstTime: Boolean
        get() = prefs.getBoolean(IS_FIRST_TIME, true)
        set(value) = prefs.edit().putBoolean(IS_FIRST_TIME, value).apply()

    var passphrase: String?
        get() = prefs.getString(PASSPHRASE, null)
        set(value) = prefs.edit().putString(PASSPHRASE, value).apply()

    var apiKey: String?
        get() = prefs.getString(API_KEY, null)
        set(value) = prefs.edit().putString(API_KEY, value).apply()

    var apiSecret: String?
        get() = prefs.getString(API_SECRET, null)
        set(value) = prefs.edit().putString(API_SECRET, value).apply()

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

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(IS_LOGGED_IN, value).apply()

    var shouldSavePassphrase: Boolean
        get() = prefs.getBoolean(SHOULD_SAVE_PASSPHRASE, true)
        set(value) = prefs.edit().putBoolean(SHOULD_SAVE_PASSPHRASE, value).apply()

    var alerts: Set<Alert>
        get() = prefs.getStringSet(ALERTS, setOf<String>())?.map { s -> Alert.forString(s) }?.toSet() ?: setOf()
        set(value) = prefs.edit().putStringSet(ALERTS, value.map { a -> a.toString() }.toSet()).apply()

    var stashedProducts: List<Product>
        get() = prefs.getStringSet(STASHED_PRODUCTS, setOf<String>())?.map { s -> Product.forString(s) } ?: listOf()
        set(value) = prefs.edit().putStringSet(STASHED_PRODUCTS, value.map { a -> a.toString() }.toSet()).apply()

    fun setRapidMovementAlerts(currency: Currency, isActive: Boolean) {
        val tempRapidMovementAlerts = rapidMovementAlertCurrencies.toMutableSet()
        if (!isActive && rapidMovementAlertCurrencies.contains(currency)) {
            tempRapidMovementAlerts.remove(currency)
        } else if (isActive && !rapidMovementAlertCurrencies.contains(currency)) {
            tempRapidMovementAlerts.add(currency)
        }
        rapidMovementAlertCurrencies = tempRapidMovementAlerts
    }

    var stashedFiatAccountList: List<Account>
        get() {
            val gson = Gson()
            val newAccountList = mutableListOf<Account>()
            for (currency in Currency.fiatList) {
                val accountString = prefs.getString(ACCOUNT + currency.toString(), "")
                val productString = prefs.getString(PRODUCT + currency.toString(), "")
                if (accountString?.isNotBlank() == true && productString?.isNotBlank() == true) {
                    try {
                        val apiAccount = gson.fromJson(accountString, ApiAccount::class.java)
                        val product = gson.fromJson(productString, Product::class.java)
                        val newAccount = Account(product, apiAccount)
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
                    prefs.edit().putString(ACCOUNT + currency.toString(), null)
                                .putString(PRODUCT + currency.toString(), null).apply()
                }
            } else {
                val gson = Gson()
                for (account in value) {
                    val accountJson = gson.toJson(account.apiAccount) ?: ""
                    val productJson = gson.toJson(account.product) ?: ""
                    prefs.edit().putString(ACCOUNT + account.currency.toString(), accountJson)
                            .putString(PRODUCT + account.currency.toString(), productJson).apply()
                }
            }
        }

    var stashedCryptoAccountList: List<Account>
        get() {
            val gson = Gson()
            val newAccountList = mutableListOf<Account>()
            for (currency in Currency.cryptoList) {
                val accountString = prefs.getString(ACCOUNT + currency.toString(), "")
                val productString = prefs.getString(PRODUCT + currency.toString(), "")
                if (accountString?.isNotBlank() == true && productString?.isNotBlank() == true) {
                    try {
                        val apiAccount = gson.fromJson(accountString, ApiAccount::class.java)
                        val product = gson.fromJson(productString, Product::class.java)
                        val dayCandleOutliers = product.defaultDayCandles.filter { it.tradingPair.id != product.id }
                        if (dayCandleOutliers.isEmpty()) {
                            val newAccount = Account(product, apiAccount)
                            newAccountList.add(newAccount)
                        } else {
                            return mutableListOf()
                        }
                    } catch (e: Exception) {
                        return mutableListOf()
                    }
                }
            }
            return newAccountList
        }
        set(value) {
            if (value.isEmpty()) {
                for (currency in Currency.cryptoList) {
                    prefs.edit().putString(ACCOUNT + currency.toString(), null)
                            .putString(PRODUCT + currency.toString(), null).apply()
                }
            } else {
                val gson = Gson()
                for (account in value) {
                    val accountJson = gson.toJson(account.apiAccount) ?: ""
                    val productJson = gson.toJson(account.product) ?: ""
                    prefs.edit().putString(ACCOUNT + account.currency.toString(), accountJson)
                            .putString(PRODUCT + account.currency.toString(), productJson).apply()
                }
            }
        }

    var lastMovementAlertTimestamp: Long
        get() = prefs.getLong(MOVEMENT_ALERT_TIMESTAMP, 0)
        set(value) = prefs.edit().putLong(MOVEMENT_ALERT_TIMESTAMP, value).apply()

    var rapidMovementAlertCurrencies: Set<Currency>
        get() = prefs.getStringSet(RAPID_PRICE_MOVES, setOf<String>())?.mapNotNull { string -> Currency.forString(string) }?.toSet() ?: setOf()
        set(value) = prefs.edit().putStringSet(RAPID_PRICE_MOVES, value.map { currency -> currency.toString() }.toSet()).apply()

    fun setMovementAlert(currency: Currency, isActive: Boolean) {
        val currentActiveAlerts = rapidMovementAlertCurrencies.toMutableSet()
        if (isActive && !rapidMovementAlertCurrencies.contains(currency)) {
            currentActiveAlerts.add(currency)
        } else if (!isActive && rapidMovementAlertCurrencies.contains(currency)) {
            currentActiveAlerts.remove(currency)
        }
        rapidMovementAlertCurrencies = currentActiveAlerts.toSet()
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

    fun stashOrders(orderListString: String?) {
        prefs.edit().putString(STASHED_ORDERS, orderListString).apply()
    }
    fun getStashedOrders(productId: String) : List<ApiOrder> {
        val apiOrdersJson = prefs.getString(STASHED_ORDERS, null)
        return try {
            val apiOrderList: List<ApiOrder> = Gson().fromJson(apiOrdersJson, object : TypeToken<List<ApiOrder>>() {}.type)
            apiOrderList.filter { it.product_id == productId }
        } catch (e: Exception) {
            listOf()
        }
    }

    fun stashFills(fillListJson: String?, productId: String) {
        //TODO: remove this line in the next version, its just there to delete old stuff
        prefs.edit().remove(STASHED_FILLS).apply()
        prefs.edit().putString(STASHED_FILLS + productId, fillListJson).apply()
    }
    fun nukeStashedFills() {
        for (product in Account.cryptoAccounts.map { it.product }) {
            for (tradingPair in product.tradingPairs) {
                prefs.edit().remove(STASHED_FILLS + tradingPair.id).apply()
            }
        }
    }
    fun getStashedFills(productId: String) : List<ApiFill> {
        val fillListJson = prefs.getString(STASHED_FILLS + productId, null)
        return try {
            val apiFillList: List<ApiFill> = Gson().fromJson(fillListJson, object : TypeToken<List<ApiFill>>() {}.type)
            apiFillList.filter { it.product_id == productId }
        } catch (e: Exception) {
            listOf()
        }
    }

    var areAlertFillsActive: Boolean
        get() = prefs.getBoolean(ARE_ALERT_FILLS_ON, true)
        set(value) = prefs.edit().putBoolean(ARE_ALERT_FILLS_ON, value).apply()

    fun isApiKeyValid(apiKey: String) : Boolean? {
        val approvedApiKeys = prefs.getStringSet(APPROVED_API_KEYS, setOf<String>())?.toMutableSet() ?: mutableSetOf()
        val rejectedApiKeys = prefs.getStringSet(REJECTED_API_KEYS, setOf<String>())?.toMutableSet() ?: mutableSetOf()
        if (approvedApiKeys.contains(apiKey)) {
            return true
        } else if (rejectedApiKeys.contains(apiKey)) {
            return false
        } else {
            return null //testResult ?: false
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

    fun addAlert(alert: Alert) {
        val tempAlerts = alerts.toMutableSet()
        tempAlerts.add(alert)
        alerts = tempAlerts.toSet()
    }

    fun removeAlert(alert: Alert) {
        val tempAlerts = alerts.toMutableSet()
        tempAlerts.removeAlert(alert)
        alerts = tempAlerts.toSet()
    }
}

