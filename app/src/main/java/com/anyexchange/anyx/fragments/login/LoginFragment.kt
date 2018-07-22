package com.anyexchange.anyx.fragments.login

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.PopupMenu
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
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

    companion object {
        fun newInstance(): LoginFragment {
            return LoginFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_login, container, false)

        val prefs = Prefs(context!!)

        apiKeyEditText = rootView.etxt_login_api_key
        apiSecretEditText = rootView.etxt_login_secret
        passphraseEditText = rootView.etxt_login_passphrase
        val btnLogin = rootView.btn_login
        val btnNewAccount = rootView.btn_login_new_account
        val btnLoginHelp = rootView.btn_login_help
        val btnSkipLogin = rootView.btn_login_skip

        if(prefs.apiKey != null) {
            apiKey = prefs.apiKey
            apiKeyEditText.setText(apiKey)
        }
        if (prefs.apiSecret != null) {
            apiSecret = prefs.apiSecret
            apiSecretEditText.setText(apiSecret)
        }

        if (prefs.passphrase != null) {
            val iv = ByteArray(16)
            val encryption = Encryption.getDefault(apiKey, apiSecret + Constants.salt, iv)
            passphrase = encryption.decryptOrNull(prefs.passphrase)

            passphraseEditText.setText(passphrase)
        }

        saveApiInfoCheckBox = rootView.cb_save_api_key
        savePassphraseCheckBox = rootView.cb_save_passphrase

        if (prefs.shouldSaveApiInfo) {
            saveApiInfoCheckBox.isChecked = true
            if (prefs.shouldSavePassphrase) {
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

        btnLogin.setOnClickListener {
            signIn()
        }

        btnNewAccount.setOnClickListener { _ ->
            val newAccountUrl = "https://pro.coinbase.com"
            (activity as com.anyexchange.anyx.activities.LoginActivity).goToFragment(com.anyexchange.anyx.activities.LoginActivity.LoginFragmentType.WebView, newAccountUrl)
        }

        btnSkipLogin.setOnClickListener { _ ->
            (activity as com.anyexchange.anyx.activities.LoginActivity).loginWithCredentials(null)
        }

        btnLoginHelp.setOnClickListener {
            //Creating the instance of PopupMenu
            val popup = PopupMenu(activity, btnLoginHelp)
            //Inflating the Popup using xml file
            popup.menuInflater.inflate(R.menu.login_help_menu, popup.menu)
            //registering popup with OnMenuItemClickListener
            val intent = Intent(activity, com.anyexchange.anyx.activities.LoginHelpActivity::class.java)
            popup.setOnMenuItemClickListener { item: MenuItem? ->
                when (item?.itemId ?: R.id.login_help_mobile) {
                    R.id.login_help_mobile -> {
                        popup.dismiss()
                        intent.putExtra(Constants.isMobileLoginHelp, true)
                        startActivity(intent)
                    }
                    R.id.login_help_desktop -> {
                        popup.dismiss()
                        intent.putExtra(Constants.isMobileLoginHelp, false)
                        startActivity(intent)
                    }
                }
                true
            }
            popup.show()  //showing popup menu
        }

        return rootView
    }

    private fun signIn() {
        val prefs = Prefs(context!!)
        prefs.shouldSaveApiInfo = saveApiInfoCheckBox.isChecked
        prefs.shouldSavePassphrase = saveApiInfoCheckBox.isChecked

        //TODO: use string resource
        if (apiKeyEditText.text.toString() != "*****") {
            apiKey = apiKeyEditText.text.toString().trimEnd()
        }
        if (apiSecretEditText.text.toString() != "*****") {
            apiSecret = apiSecretEditText.text.toString().trimEnd()
        }
        if (passphraseEditText.text.toString() != "*****") {
            passphrase = passphraseEditText.text.toString()
        }
        
        if (prefs.shouldSaveApiInfo) {
            val iv = ByteArray(16)
            val encryption = Encryption.getDefault(apiKey, apiSecret + Constants.salt, iv)
            val passphraseEncrypted = encryption.encryptOrNull(passphrase)
            prefs.apiKey = apiKey
            prefs.apiSecret = apiSecret
            if (prefs.shouldSavePassphrase && (passphrase != null))  {
                prefs.passphrase = passphraseEncrypted
            }
        }

        val apiKeyVal = apiKey ?: ""
        val apiSecretVal = apiSecret ?: ""
        val passphraseVal = passphrase ?: ""

        if (apiKeyVal.isBlank()) {
            toast(R.string.login_error_missing_api_key)
        } else if (apiSecretVal.isBlank()) {
            toast(R.string.login_error_missing_api_secret)
        } else if (passphraseVal.isBlank()) {
            toast(R.string.login_error_missing_passphrase)
        } else {
            val isApiKeyValid = prefs.isApiKeyValid(apiKeyVal)
            (activity as com.anyexchange.anyx.activities.LoginActivity).loginWithCredentials(CBProApi.ApiCredentials(apiKeyVal, apiSecretVal, passphraseVal, isApiKeyValid))
        }
    }
}
