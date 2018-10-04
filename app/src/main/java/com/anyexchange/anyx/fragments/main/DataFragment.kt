package com.anyexchange.anyx.fragments.main

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.classes.api.CBProApi
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
    private var backupFiatAccountList = listOf<Account>()
    private var backupPaymentMethodList = listOf<Account.PaymentMethod>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // retain this fragment
        retainInstance = true
    }

    fun backupData() {
        if (CBProApi.credentials != null) {
            backupCredentials = CBProApi.credentials
        }

        backupFiatAccountList = Account.fiatAccounts

        context?.let {
            val prefs = Prefs(it)
            prefs.stashedProducts = Product.map.values.toList()
            prefs.stashedFiatAccountList = Account.fiatAccounts
            prefs.stashedPaymentMethodList = Account.paymentMethods
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

        Product.map = prefs.stashedProducts.associateBy { it.currency.id }.toMutableMap()

        if (backupFiatAccountList.isNotEmpty()) {
            Account.fiatAccounts = backupFiatAccountList
        } else if (Account.fiatAccounts.isEmpty()){
            Account.fiatAccounts = prefs.stashedFiatAccountList
        }

        if (backupPaymentMethodList.isNotEmpty()) {
            Account.paymentMethods = backupPaymentMethodList
        } else if (Account.paymentMethods.isEmpty()){
            Account.paymentMethods = prefs.stashedPaymentMethodList
        }
    }

    fun destroyData(context: Context) {
        backupCredentials = null
        backupCryptoAccountList = mutableListOf()
        backupFiatAccountList = mutableListOf()

        val prefs = Prefs(context)

        prefs.stashedProducts = mutableListOf()
        prefs.stashedPaymentMethodList = mutableListOf()
        prefs.stashedFiatAccountList = mutableListOf()

    }
}
