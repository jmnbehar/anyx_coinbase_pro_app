package com.jmnbehar.gdax.Fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ListView
import com.jmnbehar.gdax.Activities.LoginActivity
import com.jmnbehar.gdax.Activities.MainActivity
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_login.view.*
import org.jetbrains.anko.support.v4.toast
import se.simbio.encryption.Encryption

/**
 * Created by josephbehar on 2/4/18.
 */

class LoginFragment : Fragment()  {
    lateinit var inflater: LayoutInflater

    private lateinit var apiKeyEditText: EditText
    private lateinit var apiSecretEditText: EditText
    private lateinit var passphraseEditText: EditText
    private lateinit var saveApiInfoCheckBox: CheckBox
    private lateinit var savePassphraseCheckBox: CheckBox

    private var apiKey: String? = ""
    private var apiSecret: String? = ""
    private var passphrase: String? = ""

    private var shouldSaveApiInfo = false
    private var shouldSavePassphrase = false

    companion object {
        fun newInstance(): LoginFragment {
            return LoginFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_login, container, false)

        val prefs = Prefs(context)

        apiKeyEditText = rootView.etxt_login_api_key
        apiSecretEditText = rootView.etxt_login_secret
        passphraseEditText = rootView.etxt_login_passphrase
        val btnLogin = rootView.btn_login
        val btnNewApiKey = rootView.btn_login_new_apikey
        val btnNewAccount = rootView.btn_login_new_acccount
        val btnSkipLogin = rootView.btn_login_skip

        if(prefs.apiKey != null) {
            apiKey = prefs.apiKey
            apiKeyEditText.setText("*****")
        }
        if (prefs.apiSecret != null) {
            apiSecret = prefs.apiSecret
            apiSecretEditText.setText("*****")
        }

        if (prefs.passphrase != null) {
            passphrase = prefs.passphrase
            passphraseEditText.setText("*****")
        }

        saveApiInfoCheckBox = rootView.cb_save_api_key
        savePassphraseCheckBox = rootView.cb_save_passphrase

        if(shouldSaveApiInfo) {
            saveApiInfoCheckBox.isChecked = true
            if(shouldSavePassphrase) {
                savePassphraseCheckBox.isChecked = true
            }
        } else {
            savePassphraseCheckBox.isChecked = false
            savePassphraseCheckBox.isEnabled = false
        }

        saveApiInfoCheckBox.setOnCheckedChangeListener { _, isChecked ->
            prefs.shouldSaveApiInfo = isChecked
            if (!isChecked) {
                savePassphraseCheckBox.isChecked = false
                savePassphraseCheckBox.isEnabled = false
            } else {
                savePassphraseCheckBox.isEnabled = true
            }
        }
        savePassphraseCheckBox.setOnCheckedChangeListener { _, isChecked ->
            prefs.shouldSavePassphrase = isChecked
        }

        btnLogin.setOnClickListener { view ->
            btn_login.text = "Logging In"

            signIn()
        }

        btnNewApiKey.setOnClickListener { _ ->
            val newApiKeyUrl = "https://www.gdax.com/settings/api"
            (activity as LoginActivity).goToFragment(true, newApiKeyUrl)
        }

        btnNewAccount.setOnClickListener { _ ->
            val newAccountUrl = "https://www.coinbase.com/users/oauth_signup?client_id=2e7433cc0730d8cb8c77dd30e04b5658aacbf7612b2bad8aa7bb97b87fc0f876&meta%5Baccount%5D=all&redirect_uri=https%3A%2F%2Fwww.gdax.com%2Foauth_redirect&response_type=code"
            (activity as LoginActivity).goToFragment(true, newAccountUrl)
        }

        btnSkipLogin.setOnClickListener { _ ->
            (activity as LoginActivity).loginWithCredentials(null)
        }

        return rootView
    }

    private fun signIn() {
        val prefs = Prefs(context)

        val iv = ByteArray(16)
        val encryption = Encryption.getDefault(passphrase, Constants.salt, iv)

        shouldSaveApiInfo = saveApiInfoCheckBox.isChecked
        shouldSavePassphrase = saveApiInfoCheckBox.isChecked

        if (apiKeyEditText.text.toString() != "*****") {
            apiKey = apiKeyEditText.text.toString()
        }
        if (apiSecretEditText.text.toString() != "*****") {
            apiSecret = apiSecretEditText.text.toString()
        }
        if (passphraseEditText.text.toString() != "*****") {
            passphrase = passphraseEditText.text.toString()
        }

        if (apiKey == prefs.apiKey) {
            apiKey =  encryption.decryptOrNull(apiKey)
        }
        if (apiSecret == prefs.apiSecret) {
            apiSecret = encryption.decryptOrNull(apiSecret)
        }

        val apiKeyVal = apiKey
        val apiSecretVal = apiSecret
        val passphraseVal = passphrase
        if (shouldSaveApiInfo) {
            val apiKeyEncrypted = encryption.encryptOrNull(apiKeyVal)
            val apiSecretEncrypted = encryption.encryptOrNull(apiSecretVal)
            prefs.apiKey = apiKeyEncrypted
            prefs.apiSecret = apiSecretEncrypted
            if (shouldSavePassphrase && (passphraseVal != null))  {
                prefs.passphrase = passphraseVal
            }
        }
        if((apiKeyVal != null) && (apiSecretVal != null) && (passphraseVal != null)
                && (apiKeyVal != "") && (apiSecretVal != "") && (passphraseVal != "")) {
            (activity as LoginActivity).loginWithCredentials(ApiCredentials(passphraseVal, apiKeyVal, apiSecretVal))
        } else {
            toast("Wrong Passphrase")
        }
    }
}
