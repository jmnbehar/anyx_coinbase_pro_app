package com.jmnbehar.gdax.Activities

import android.os.Bundle
import android.support.compat.R.id.async
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.activity_login.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.Buffer
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.Base64.getDecoder
import java.util.Base64.getEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


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

        val apiKey: String = txt_login_api_key.text.toString()
        val apiSecret: String = txt_login_secret.text.toString()
        val passphrase: String = txt_login_passphrase.text.toString()


        btn_login.setOnClickListener { view ->
            showMessage(view,"Button")
            btn_login.text = "newButtonText"

            signIn(view, passphrase, apiKey, apiSecret)
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

    private fun signIn(view: View, passPhrase: String, apiKey: String, secret: String) {
        var timestamp = Date()
        var requestPath = "/accounts"

//        var body = JSON.stringify({
//            price: '1.0',
//            size: '1.0',
//            side: 'buy',
//            product_id: 'BTC-USD'
//    });
        val body = ""
        var method = "GET"

        var message = timestamp.toInstant().epochSecond.toString() + method + requestPath + body
        println("timestamp:")
        println(timestamp)
        println(timestamp.toString())
        println(timestamp.toInstant().epochSecond)
        println(timestamp.toInstant())

        val secretDecoded = Base64.getDecoder().decode(secret)

        val sha256_HMAC = Mac.getInstance("HmacSHA256")
        val secret_key = SecretKeySpec(secretDecoded, "HmacSHA256")
        sha256_HMAC.init(secret_key)


        val hash = Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(message.toByteArray()))
        println("hash:")
        println(hash)

//        async {
//            val result = URL("<api call>").readText()
//            uiThread {
//                Log.d("Request", result)

//                longToast("Request performed")
//            }
//        }
        val connection = URL("https://api.gdax.com/accounts").openConnection() as HttpURLConnection
        connection.headerFields

        val result = URL("https://api.gdax.com/accounts").readText()
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