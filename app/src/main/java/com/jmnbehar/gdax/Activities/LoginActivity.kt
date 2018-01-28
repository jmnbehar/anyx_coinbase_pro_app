package com.jmnbehar.gdax.Activities

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.toast
import se.simbio.encryption.Encryption


class LoginActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var prefs: Prefs

    private lateinit var saveApiInfoCheckBox: CheckBox
    private lateinit var savePassphraseCheckBox: CheckBox

    private lateinit var apiKeyEditText: EditText
    private lateinit var apiSecretEditText: EditText
    private lateinit var passphraseEditText: EditText

    private var apiKey: String? = ""
    private var apiSecret: String? = ""
    private var passphrase: String? = ""

    private var shouldSaveApiInfo = false
    private var shouldSavePassphrase = false
    private var skipLogin = false

    private fun checkUser() : Boolean {
        if(!skipLogin && shouldSaveApiInfo && shouldSavePassphrase) {
            apiKey = prefs.apiKey
            apiSecret = prefs.apiSecret
            var passphrase = prefs.passphrase

            val salt = "GdaxApp"
            val iv = ByteArray(16)
            val encryption = Encryption.getDefault(passphrase, salt, iv)

            val apiKeyVal = encryption.decryptOrNull(apiKey)
            val apiSecretVal = encryption.decryptOrNull(apiSecret)
            if((apiKeyVal != null) && (apiSecretVal != null) && (passphrase != null)) {
                var apiCredentials = ApiCredentials(passphrase, apiKeyVal, apiSecretVal)
                loginWithCredentials(apiCredentials)
                return true
            }
        }
        return false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(Constants.exit, false)) {
            finish()
            return
        } else if (intent.getBooleanExtra(Constants.logout, false)) {
            skipLogin = true
        }

        prefs = Prefs(this)
        shouldSaveApiInfo = prefs.shouldSaveApiInfo
        shouldSavePassphrase = prefs.shouldSavePassphrase
        if (savedInstanceState == null) {
            if (!checkUser()) {
                setContentView(R.layout.activity_login)
                apiKeyEditText = etxt_login_api_key
                apiSecretEditText = etxt_login_secret
                passphraseEditText = etxt_login_passphrase
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

                saveApiInfoCheckBox = cb_save_api_key
                savePassphraseCheckBox = cb_save_passphrase

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

                btn_login.setOnClickListener { view ->
                    showMessage(view,"Button")
                    btn_login.text = "newButtonText"

                    signIn()
                }
            }
        }
    }

    private fun showMessage(view: View, string: String) {
        Snackbar.make(view, string, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
    }

    private fun signIn() {
        // Usage

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
            loginWithCredentials(ApiCredentials(passphraseVal, apiKeyVal, apiSecretVal))
        } else {
            toast("Wrong Passphrase")
        }
    }

    private fun loginWithCredentials(credentials: ApiCredentials) {
        GdaxApi.credentials = credentials
        Account.getAccounts(this, { result -> toast("Error!: ${result.error}") }, {
            toast("Success! logging in")
            var prefs = Prefs(this)
            prefs.shouldAutologin = true
            val intent = MainActivity.newIntent(this)
            startActivity(intent)
        })
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return true
    }
}