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
    private var backupAccountList: MutableList<Account> = mutableListOf()
    private var backupFiatAccount: Account? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // retain this fragment
        retainInstance = true
    }

    fun backupData() {
        if (CBProApi.credentials != null) {
            backupCredentials = CBProApi.credentials
        }   //TODO: wipe this backup on log out

        backupAccountList = Account.list
        activity?.let { activity ->
            val prefs = Prefs(activity)
            prefs.stashedAccountList = Account.list
            prefs.stashedFiatAccount = Account.usdAccount
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
        if (backupAccountList.isNotEmpty()) {
            Account.list = backupAccountList
        } else if (Account.list.isEmpty()){
            Account.list = prefs.stashedAccountList
        }
        if (backupFiatAccount != null) {
            Account.usdAccount = backupFiatAccount
        } else if (Account.usdAccount == null){
            Account.usdAccount = prefs.stashedFiatAccount
        }
    }

    fun destroyData(context: Context) {
        backupCredentials = null
        backupAccountList = mutableListOf()
        backupFiatAccount = null

        val prefs = Prefs(context)
        prefs.apiKey = null
        prefs.apiSecret = null
        prefs.passphrase = null
        prefs.stashedAccountList = mutableListOf()
        prefs.stashedFiatAccount = null

    }
}
