package com.jmnbehar.gdax.Classes

import android.content.Context
import android.content.SharedPreferences
//import se.simbio.encryption.Encryption

/**
 * Created by josephbehar on 12/28/17.
 */

class Prefs (context: Context) {
    private val FILE_NAME = "com.jmnbehar.gdax.prefs"
    private val PASSPHRASE = "passphrase"
    private val API_KEY = "api_key"
    private val API_SECRET = "api_secret"
    private val SAVE_PASSWORDS = "save_passwords"
    private val ALERTS = "alerts"
    private val prefs: SharedPreferences = context.getSharedPreferences(FILE_NAME, 0)

    var passphrase: String
        get() = prefs.getString(PASSPHRASE, "")
        set(value) = prefs.edit().putString(PASSPHRASE, value).apply()

    var apiKey: String
        get() = prefs.getString(API_KEY, "")
        set(value) = prefs.edit().putString(API_KEY, value).apply()

    var apiSecret: String
        get() = prefs.getString(API_SECRET, "")
        set(value) = prefs.edit().putString(API_SECRET, value).apply()

    var shouldSavePasswords: Boolean
        get() = prefs.getBoolean(SAVE_PASSWORDS, false)
        set(value) = prefs.edit().putBoolean(SAVE_PASSWORDS, value).apply()

    var alerts: Set<Alert>
        get() = prefs.getStringSet(ALERTS, setOf<String>()).map { s -> Alert.fromString(s) }.toSet()
        set(value) = prefs.edit().putStringSet(ALERTS, value.map { a -> a.toString() }.toSet()).apply()

    fun addAlert(alert: Alert) {
        val tempAlerts = alerts.toMutableSet()
        tempAlerts.add(alert)
        alerts = tempAlerts.toSet()
    }

    fun removeAlert(alert: Alert) {
        val tempAlerts = alerts.toMutableSet()
        tempAlerts.remove(alert)
        alerts = tempAlerts.toSet()
    }
}

