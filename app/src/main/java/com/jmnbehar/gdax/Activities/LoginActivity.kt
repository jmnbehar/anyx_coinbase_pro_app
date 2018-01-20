package com.jmnbehar.gdax.Activities

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getAs
import com.jmnbehar.gdax.Classes.ApiCredentials
import com.jmnbehar.gdax.Classes.GdaxApi
import com.jmnbehar.gdax.Classes.Prefs
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.toast
import se.simbio.encryption.Encryption


class LoginActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var prefs: Prefs

    private lateinit var saveApiInfoCheckBox: CheckBox
    private lateinit var savePassphraseCheckBox: CheckBox

    private var apiKey: String? = ""
    private var apiSecret: String? = ""

    private var shouldSaveApiInfo = false
    private var shouldSavePassphrase = false

    private fun checkUser() : Boolean {
        if(shouldSaveApiInfo && shouldSavePassphrase) {
            apiKey = prefs.apiKey
            apiSecret = prefs.apiSecret
            var passphrase = prefs.passphrase

            val salt = "GdaxApp"
            val iv = ByteArray(16)
            val encryption = Encryption.getDefault(passphrase, salt, iv)

            val apiKeyVal = encryption.decryptOrNull(apiKey)
            val apiSecretVal = encryption.decryptOrNull(apiSecret)
            if((apiKeyVal != null) && (apiSecretVal != null)) {
                var apiCredentials = ApiCredentials(passphrase, apiKeyVal, apiSecretVal)
                loginWithCredentials(apiCredentials)
                return true
            }
        }
        return false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("EXIT", false)) {
            finish()
            return
        }
        prefs = Prefs(this)
        shouldSaveApiInfo = prefs.shouldSaveApiInfo
        shouldSavePassphrase = prefs.shouldSavePassphrase
        if (savedInstanceState == null) {
            if (!checkUser()) {
                setContentView(R.layout.activity_login)


                if(prefs.apiKey != "") {
                    apiKey = prefs.apiKey
                    txt_login_api_key.setText("*****")
                }
                if (prefs.apiSecret != "") {
                    apiSecret = prefs.apiSecret
                    txt_login_secret.setText("*****")
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

                saveApiInfoCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
                    prefs.shouldSaveApiInfo = isChecked
                    if (!isChecked) {
                        savePassphraseCheckBox.isChecked = false
                        savePassphraseCheckBox.isEnabled = false
                    } else {
                        savePassphraseCheckBox.isEnabled = true
                    }
                }
                savePassphraseCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
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

        var passphrase = txt_login_passphrase.text.toString()

        val salt = "GdaxApp"
        val iv = ByteArray(16)
        val encryption = Encryption.getDefault(passphrase, salt, iv)

        shouldSaveApiInfo = saveApiInfoCheckBox.isChecked
        shouldSavePassphrase = saveApiInfoCheckBox.isChecked

        apiKey = if (apiKey == prefs.apiKey) {
            encryption.decryptOrNull(apiKey)
        } else {
            txt_login_api_key.text.toString()
        }

        apiSecret = if (apiSecret == prefs.apiSecret) {
            encryption.decryptOrNull(apiSecret)
        } else {
            txt_login_secret.text.toString()
        }

        if (shouldSaveApiInfo) {
            val apiKeyEncrypted = encryption.encrypt(apiKey)
            val apiSecretEncrypted = encryption.encrypt(apiSecret)
            prefs.apiKey = apiKeyEncrypted
            prefs.apiSecret = apiSecretEncrypted
            if (shouldSavePassphrase)  {
                prefs.passphrase = passphrase
            }

            var apiKeyDecrypted = encryption.decryptOrNull(apiKeyEncrypted)
            var apiSecretDecrypted = encryption.decryptOrNull(apiSecretEncrypted)

            println(apiKeyDecrypted)
            println(apiSecretDecrypted)
            toast(apiKeyDecrypted)
        }
        val apiKeyVal = apiKey
        val apiSecretVal = apiSecret
        if((apiKeyVal != null) && (apiSecretVal != null)) {
            var apiCredentials = ApiCredentials(passphrase, apiKeyVal, apiSecretVal)

            loginWithCredentials(apiCredentials)
        } else {
            toast("Wrong Passphrase")
        }
    }

    fun loginWithCredentials(credentials: ApiCredentials) {
        var data: String?
        GdaxApi.credentials = credentials
        //TODO: move this call into mainactivity
        //TODO: make mainactivity default 1st activity, bounce back to login if not available
        GdaxApi.products().executeRequest { result ->
            when (result) {
                is Result.Failure -> {
                    //error
                    println("Error!: ${result.error}")
                }
                is Result.Success -> {
                    data = result.getAs()
                    println("Success!: ${data}")

                    val intent = MainActivity.newIntent(this, result.value)

                    startActivity(intent)
                }
            }
        }
    }

//    override fun onBackPressed() {
//        super.onBackPressed()
//    }

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