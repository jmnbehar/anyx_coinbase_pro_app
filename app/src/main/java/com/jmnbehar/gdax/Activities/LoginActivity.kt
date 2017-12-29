package com.jmnbehar.gdax.Activities

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getAs
import com.jmnbehar.gdax.Classes.ApiCredentials
import com.jmnbehar.gdax.Classes.GdaxApi
import com.jmnbehar.gdax.Classes.Prefs
import com.jmnbehar.gdax.Classes.toast
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.activity_login.*
import se.simbio.encryption.Encryption


class LoginActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var prefs: Prefs

    var apiKey: String? = ""
    var apiSecret: String? = ""

    fun checkUser() {
        // Check if user is signed in (non-null) and update UI accordingly.

//        if (currentUser == null) {
//            //stay here
//        } else {
//            val intent = MainActivity.newIntent(this, currentUser)
//            startActivity(intent)
//        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            checkUser()
        }

        setContentView(R.layout.activity_login)

        prefs = Prefs(this)

        if(prefs.apiKey != "") {
            apiKey = prefs.apiKey
            txt_login_api_key.setText("*****")
        }
        if (prefs.apiSecret != "") {
            apiSecret = prefs.apiSecret
            txt_login_secret.setText("*****")
        }

        btn_login.setOnClickListener { view ->
            showMessage(view,"Button")
            btn_login.text = "newButtonText"

            signIn()
        }

    }

    private fun showMessage(view: View, string: String) {
        Snackbar.make(view, string, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
    }

    private fun signIn() {
        // Usage
        var data: String?

        var passphrase = txt_login_passphrase.text.toString()
        val salt = "GdaxApp"
        val iv = ByteArray(16)
        val encryption = Encryption.getDefault(passphrase, salt, iv)

        val shouldSavePassword = cb_save_passwords.isChecked

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
        
        if (shouldSavePassword) {
            val apiKeyEncrypted = encryption.encrypt(apiKey)
            val apiSecretEncrypted = encryption.encrypt(apiSecret)
            prefs.apiKey = apiKeyEncrypted
            prefs.apiSecret = apiSecretEncrypted
        }
        val apiKeyVal = apiKey
        val apiSecretVal = apiSecret
        if((apiKeyVal != null) && (apiSecretVal != null)) {
            var apiCredentials = ApiCredentials(passphrase, apiKeyVal, apiSecretVal)

            GdaxApi.credentials = apiCredentials
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
        } else {
            toast("Wrong Passphrase")
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