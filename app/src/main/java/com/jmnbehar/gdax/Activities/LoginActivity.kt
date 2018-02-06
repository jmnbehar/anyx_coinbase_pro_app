package com.jmnbehar.gdax.Activities

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.Fragments.*
import com.jmnbehar.gdax.R
import org.jetbrains.anko.toast
import se.simbio.encryption.Encryption


class LoginActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var prefs: Prefs

    private var shouldSaveApiInfo = false
    private var shouldSavePassphrase = false
    private var skipLogin = false
    private var currentFragment: Fragment? = null

    private fun checkUser() : Boolean {
        if(!skipLogin && shouldSaveApiInfo && shouldSavePassphrase) {
            val apiKey = prefs.apiKey
            val apiSecret = prefs.apiSecret
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
        setContentView(R.layout.activity_login
        )
      //  nav_view.setNavigationItemSelectedListener(this)

        prefs = Prefs(this)

        if (intent.getBooleanExtra(Constants.exit, false)) {
            finish()
            return
        } else if (prefs.isFirstTime) {
            onboardNewUser()
            skipLogin = true
        } else if (intent.getBooleanExtra(Constants.logout, false)) {
            skipLogin = true
        }

        shouldSaveApiInfo = prefs.shouldSaveApiInfo
        shouldSavePassphrase = prefs.shouldSavePassphrase
        if (savedInstanceState == null) {
            if (!checkUser()) {
                goToFragment(false)
            }
        }
    }

    private fun onboardNewUser() {
        val intent = Intent(this, OnboardActivity::class.java)
        startActivity(intent)
    }

    fun goToFragment(isWebViewFragment: Boolean, url: String? = null) {
        val tag: String
        val fragment = if (isWebViewFragment) {
            tag = "Webview"
            WebviewFragment.newInstance(url)
        } else {
            tag = "Login"
            LoginFragment.newInstance()
        }
        currentFragment = fragment
        if (supportFragmentManager.backStackEntryCount == 0) {
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container, fragment, tag)
                    .addToBackStack(tag)
                    .commit()
        } else {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment, tag)
                    .addToBackStack(tag)
                    .commit()
        }
    }

    private fun loginWithCredentials(credentials: ApiCredentials) {
        GdaxApi.credentials = credentials
        MainActivity.progressDialog?.show()
        Account.getAccounts(this, { result -> toast("Error!: ${result.error}") }, {
            MainActivity.progressDialog?.dismiss()
            toast("Success! logging in")
            var prefs = Prefs(this)
            prefs.shouldAutologin = true
            val intent = MainActivity.newIntent(this)
            startActivity(intent)
        })
    }


    override fun onBackPressed() {
        val currentFragment = currentFragment
        if (currentFragment is WebviewFragment) {
            if (currentFragment.webView?.onBackPressed() ?: true) {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
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