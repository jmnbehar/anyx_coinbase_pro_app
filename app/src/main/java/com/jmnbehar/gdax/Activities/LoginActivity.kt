package com.jmnbehar.gdax.Activities

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getAs
import com.jmnbehar.gdax.Classes.ApiCredentials
import com.jmnbehar.gdax.Classes.GdaxApi
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {


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

        var apiKey: String = txt_login_api_key.text.toString()
        var apiSecret: String = txt_login_secret.text.toString()
        var passphrase: String = txt_login_passphrase.text.toString()

        var apiCredentials = ApiCredentials(passphrase, apiKey, apiSecret)

        btn_login.setOnClickListener { view ->
            showMessage(view,"Button")
            btn_login.text = "newButtonText"

            signIn(view, apiCredentials)
        }

//        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestIdToken(getString(R.string.default_web_client_id))
//                .requestEmail()
//                .build()

    }

    private fun showMessage(view: View, string: String) {
        Snackbar.make(view, string, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
    }

    private fun signIn(view: View, apiCredentials: ApiCredentials) {
        // Usage
        var data: String? = null
        GdaxApi.credentials = apiCredentials
        GdaxApi.products().executeRequest { result ->
            val result = result as Result.Success
            data = result.getAs()
            println("Success!: ${data}")
            val intent = MainActivity.newIntent(this, result.value)

            startActivity(intent)
        }


//
//        Fuel.request(GdaxApi.products()).responseString { request, response, result ->
//            //do something with response
//
//            println("url: " + request.url)
//            when (result) {
//                is Result.Failure -> {
//                    //error
//                    println("Error!: ${result.error}")
//                }
//                is Result.Success -> {
//                    data = result.getAs()
//                    println("Success!: ${data}")
//
//
//                    val intent = MainActivity.newIntent(this, result.value)
//
//                    startActivity(intent)
//                }
//            }
//        }
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