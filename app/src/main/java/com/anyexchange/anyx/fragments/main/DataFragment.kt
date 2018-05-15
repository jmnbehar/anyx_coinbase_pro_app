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

    private var backupCredentials: GdaxApi.ApiCredentials? = null
    private var backupAccountList: MutableList<Account> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // retain this fragment
        retainInstance = true
    }

    fun backupData() {
        if (GdaxApi.credentials != null) {
            backupCredentials = GdaxApi.credentials
        }   //TODO: wipe this backup on log out

        backupAccountList = Account.list
        activity?.let { activity ->
            val prefs = Prefs(activity)
            prefs.stashedAccountList = Account.list
        }
    }

    fun restoreData(context: Context) {
        if ( GdaxApi.credentials == null || GdaxApi.credentials?.apiKey?.isEmpty() == true) {
            if (backupCredentials != null) {
                GdaxApi.credentials = backupCredentials
            } else {
                val prefs = Prefs(context)

                val apiKey = prefs.apiKey
                val apiSecret = prefs.apiSecret
                val passphraseEncrypted = prefs.passphrase
                val iv = ByteArray(16)
                val encryption = Encryption.getDefault(apiKey, apiSecret + Constants.salt, iv)
                val passphrase = encryption.decryptOrNull(passphraseEncrypted)
                if ((apiKey != null) && (apiSecret != null) && (passphrase != null)) {
                    val isApiKeyValid = prefs.isApiKeyValid(apiKey)
                    backupCredentials = GdaxApi.ApiCredentials(apiKey, apiSecret, passphrase, isApiKeyValid)
                    GdaxApi.credentials = backupCredentials
                }
            }
        }
        if (backupAccountList.isNotEmpty()) {
            Account.list = backupAccountList
        } else if (Account.list.isEmpty()){
            val prefs = Prefs(context)
            Account.list = prefs.stashedAccountList
        }
    }

    fun destroyData() {
        backupCredentials = null
        backupAccountList = mutableListOf()
    }
}
