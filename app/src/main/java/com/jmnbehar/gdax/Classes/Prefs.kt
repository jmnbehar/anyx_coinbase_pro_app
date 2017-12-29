package com.jmnbehar.gdax.Classes

import android.content.Context
import android.content.SharedPreferences
//import se.simbio.encryption.Encryption

/**
 * Created by josephbehar on 12/28/17.
 */

class Prefs (context: Context) {
    val FILE_NAME = "com.jmnbehar.gdax.prefs"
    val PASSPHRASE = "passphrase"
    val API_KEY = "api_key"
    val API_SECRET = "api_secret"
    val SAVE_PASSWORDS = "save_passwords"
    val prefs: SharedPreferences = context.getSharedPreferences(FILE_NAME, 0)

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
}

