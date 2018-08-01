package com.anyexchange.anyx.fragments.main

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import com.anyexchange.anyx.classes.*
import se.simbio.encryption.Encryption

/**
 * Created by josephbehar on 5/11/18.
 */
class DataFragment : Fragment() {
    companion object {
        fun newInstance(): DataFragment
        {
            return DataFragment()
        }
    }

    private var backupCredentials: CBProApi.ApiCredentials? = null
    private var backupCryptoAccountList = listOf<Account>()
    private var backupFiatAccountList =listOf<Account>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // retain this fragment
        retainInstance = true
    }

    fun backupData() {
        if (CBProApi.credentials != null) {
            backupCredentials = CBProApi.credentials
        }   //TODO: wipe this backup on log out

        backupCryptoAccountList = Account.cryptoAccounts
        activity?.let { activity ->
            val prefs = Prefs(activity)
            prefs.stashedCryptoAccountList = Account.cryptoAccounts
            prefs.stashedFiatAccountList = Account.fiatAccounts
        }
    }

    fun restoreData(context: Context) {
        val prefs = Prefs(context)
        if ( CBProApi.credentials == null || CBProApi.credentials?.apiKey?.isEmpty() == true) {
            if (backupCredentials != null) {
                CBProApi.credentials = backupCredentials
            } else {
                val apiKey = prefs.apiKey
                val apiSecret = prefs.apiSecret
                val passphraseEncrypted = prefs.passphrase
                val iv = ByteArray(16)
                val encryption = Encryption.getDefault(apiKey, apiSecret + Constants.salt, iv)
                val passphrase = encryption.decryptOrNull(passphraseEncrypted)
                if ((apiKey != null) && (apiSecret != null) && (passphrase != null)) {
                    val isApiKeyValid = prefs.isApiKeyValid(apiKey)
                    backupCredentials = CBProApi.ApiCredentials(apiKey, apiSecret, passphrase, isApiKeyValid)
                    CBProApi.credentials = backupCredentials
                }
            }
        }
        if (backupCryptoAccountList.isNotEmpty()) {
            Account.cryptoAccounts = backupCryptoAccountList
        } else if (Account.cryptoAccounts.isEmpty()){
            Account.cryptoAccounts = prefs.stashedCryptoAccountList
        }
        if (backupFiatAccountList.isNotEmpty()) {
            Account.fiatAccounts = backupFiatAccountList
        } else if (Account.fiatAccounts.isEmpty()){
            Account.fiatAccounts = prefs.stashedFiatAccountList
        }
    }

    fun destroyData(context: Context) {
        backupCredentials = null
        backupCryptoAccountList = mutableListOf()
        backupFiatAccountList = mutableListOf()

        val prefs = Prefs(context)
        prefs.apiKey = null
        prefs.apiSecret = null
        prefs.passphrase = null
        prefs.stashedCryptoAccountList = mutableListOf()
        prefs.stashedFiatAccountList = mutableListOf()

    }
}
