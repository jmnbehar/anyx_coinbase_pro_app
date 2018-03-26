package com.jmnbehar.anyx.Activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.Fragments.Login.LoginFragment
import com.jmnbehar.anyx.Fragments.Login.WebviewFragment
import com.jmnbehar.anyx.R
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.toast
import se.simbio.encryption.Encryption


class LoginActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var prefs: Prefs

    private var shouldSaveApiInfo = false
    private var shouldSavePassphrase = false
    private var skipLogin = false
    private var currentFragment: Fragment? = null

    //TODO: nuke this like i nuked it in mainactivity
    var progressDialog: ProgressDialog? = null


    private fun checkUser() : Boolean {
        if(!skipLogin && shouldSaveApiInfo && shouldSavePassphrase) {
            val apiKey = prefs.apiKey
            val apiSecret = prefs.apiSecret
            var passphrase = prefs.passphrase

            val salt = "GdaxApp"
            val iv = ByteArray(16)
            val encryption = Encryption.getDefault(passphrase, salt, iv)

            passphrase = encryption.decryptOrNull(passphrase)
            if((apiKey != null) && (apiSecret != null) && (passphrase != null)) {
                val isApiKeyValid = prefs.isApiKeyValid(apiKey)
                var apiCredentials = GdaxApi.ApiCredentials(apiKey, apiSecret, passphrase, isApiKeyValid)
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

        val nullMessage: CharSequence? = null
        progressDialog = indeterminateProgressDialog(nullMessage)
        progressDialog?.dismiss()

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
                goToFragment(LoginFragmentType.Login)
            }
        }
    }

    private fun onboardNewUser() {
        val intent = Intent(this, OnboardActivity::class.java)
        startActivity(intent)
    }

    enum class LoginFragmentType {
        Login,
        WebView;
    }

    fun goToFragment(loginFragmentType: LoginFragmentType, url: String? = null) {
        val tag: String

        val fragment = when(loginFragmentType) {
            LoginFragmentType.Login -> {
                tag = "Login"
                LoginFragment.newInstance()
            }
            LoginFragmentType.WebView -> {
                tag = "Webview"
                WebviewFragment.newInstance(url)
            }
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

    fun loginWithCredentials(credentials: GdaxApi.ApiCredentials?) {
        GdaxApi.credentials = credentials
        progressDialog?.show()
        GdaxApi.accounts().getAllAccountInfo(this, { result ->
            val errorMessage = GdaxApi.ErrorMessage.forString(result.errorMessage)
            when (errorMessage) {
                GdaxApi.ErrorMessage.Forbidden -> {
                    //TODO: show popup instead of toast
                    toast("Error: Forbidden" +
                            "\nMake sure your API Key has all required permissions")
                }
                GdaxApi.ErrorMessage.InvalidApiKey-> {
                    toast("Error: ${result.errorMessage}")
                }
                GdaxApi.ErrorMessage.InvalidApiSignature -> {
                    toast("Error: Invalid API Secret")
                }
                GdaxApi.ErrorMessage.InvalidPassphrase -> {
                    toast("Error: ${result.errorMessage}")
                }
                else -> toast("Error: ${result.errorMessage}")
            }
            progressDialog?.dismiss()

        }, {
            progressDialog?.dismiss()
            //toast("Success! logging in")
            prefs.shouldAutologin = GdaxApi.isLoggedIn
            val intent = MainActivity.newIntent(this)
            startActivity(intent)
            finish()
        })
    }


    override fun onBackPressed() {
        when (currentFragment) {
            is LoginFragment -> {
                super.onBackPressed()
                finish()
            }
            else -> super.onBackPressed()
        }
        if (supportFragmentManager.backStackEntryCount > 0) {
            val prevFragmentTag = supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 1).name
            currentFragment = supportFragmentManager.findFragmentByTag(prevFragmentTag)
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