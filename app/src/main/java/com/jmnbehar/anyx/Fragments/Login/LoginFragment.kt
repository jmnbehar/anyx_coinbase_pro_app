package com.jmnbehar.anyx.Fragments.Login

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
import com.jmnbehar.anyx.Activities.LoginActivity
import com.jmnbehar.anyx.Activities.LoginHelpActivity
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.R
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
        val btnNewApiKey = rootView.btn_login_new_apikey
        val btnNewAccount = rootView.btn_login_new_acccount
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

        btnLogin.setOnClickListener { view ->
            btn_login.text = "Logging In"

            signIn()
        }

        btnNewApiKey.setOnClickListener { _ ->
            val newApiKeyUrl = "https://www.gdax.com/settings/api"
            (activity as LoginActivity).goToFragment(LoginActivity.LoginFragmentType.WebView, newApiKeyUrl)
        }

        btnNewAccount.setOnClickListener { _ ->
            //            val newAccountUrl = "https://www.coinbase.com/users/oauth_signup?client_id=2e7433cc0730d8cb8c77dd30e04b5658aacbf7612b2bad8aa7bb97b87fc0f876&meta%5Baccount%5D=all&redirect_uri=https%3A%2F%2Fwww.gdax.com%2Foauth_redirect&response_type=code"
//            val newAccountUrl = "https://www.coinbase.com/oauth/authorize?response_type=code&client_id=YOUR_CLIENT_ID&redirect_uri=YOUR_REDIRECT_URL&state=SECURE_RANDOM&scope=wallet:accounts:read"
            val newAccountUrl = "https://www.gdax.com/trade/BTC-USD"
            (activity as LoginActivity).goToFragment(LoginActivity.LoginFragmentType.WebView, newAccountUrl)
        }

        btnSkipLogin.setOnClickListener { _ ->
            (activity as LoginActivity).loginWithCredentials(null)
        }

        btnLoginHelp.setOnClickListener {
            //Creating the instance of PopupMenu
            val popup = PopupMenu(activity, btnLoginHelp);
            //Inflating the Popup using xml file
            popup.menuInflater.inflate(R.menu.login_help_menu, popup.menu);

            //registering popup with OnMenuItemClickListener
            val intent = Intent(activity, LoginHelpActivity::class.java)
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

        if (apiKeyEditText.text.toString() != "*****") {
            apiKey = apiKeyEditText.text.toString()
        }
        if (apiSecretEditText.text.toString() != "*****") {
            apiSecret = apiSecretEditText.text.toString()
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

        if((apiKeyVal != "") && (apiSecretVal != "") && (passphraseVal != "")) {
            val isApiKeyValid = prefs.isApiKeyValid(apiKeyVal)
            (activity as LoginActivity).loginWithCredentials(GdaxApi.ApiCredentials(apiKeyVal, apiSecretVal, passphraseVal, isApiKeyValid))
        } else {
            toast("Invalid Api Key, Secret, or Passphrase")
        }
    }
}
