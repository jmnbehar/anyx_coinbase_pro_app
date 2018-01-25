package com.jmnbehar.gdax.Activities

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.NavigationView
import android.support.v4.app.FragmentManager
import android.support.v4.app.NotificationCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.Fragments.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.support.v4.onRefresh
import org.jetbrains.anko.toast
import se.simbio.encryption.Encryption


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var notificationManager: NotificationManager? = null

    enum class FragmentType {
        BTC_CHART,
        BCH_CHART,
        ETH_CHART,
        LTC_CHART,
        ACCOUNT,
        SEND,
        ALERTS,
        SETTINGS,
        TRADE,
        PRICES;


        override fun toString() : String {
            return when (this) {
                BTC_CHART -> "BTC"
                BCH_CHART -> "BCH"
                ETH_CHART -> "ETH"
                LTC_CHART -> "LTC"
                ACCOUNT -> "ACCOUNT"
                SEND -> "SEND"
                ALERTS -> "ALERTS"
                SETTINGS -> "SETTINGS"
                TRADE -> "TRADE"
                PRICES -> "PRICES"
            }
        }

    }

    companion object {
        var currentFragment: RefreshFragment? = null
        lateinit private var fragmentManager: FragmentManager
        lateinit var swipeRefreshLayout: SwipeRefreshLayout

        var btcChartFragment: ChartFragment? = null
        var ethChartFragment: ChartFragment? = null
        var ltcChartFragment: ChartFragment? = null
        var bchChartFragment: ChartFragment? = null

        var accountsFragment: AccountsFragment? = null

        var sendFragment: SendFragment? = null

        var alertsFragment: AlertsFragment? = null

        var settingsFragment: SettingsFragment? = null

        var pricesFragment: PricesFragment? = null

        var progressDialog: ProgressDialog? = null

        fun newIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }

        fun setSupportFragmentManager(fragmentManager: FragmentManager) {
            this.fragmentManager = fragmentManager
        }


        fun goToNavigationId(navigationId: Int, context: Context) {
            when (navigationId) {
                R.id.nav_btc -> {
                    goToFragment(FragmentType.BTC_CHART)
                }
                R.id.nav_eth -> {
                    goToFragment(FragmentType.ETH_CHART)
                }
                R.id.nav_ltc -> {
                    goToFragment(FragmentType.LTC_CHART)
                }
                R.id.nav_bch -> {
                    goToFragment(FragmentType.BCH_CHART)
                }
                R.id.nav_accounts -> {
                    goToFragment(FragmentType.ACCOUNT)
                }
                R.id.nav_send -> {
                    goToFragment(FragmentType.SEND)
                }
                R.id.nav_alerts -> {
                    goToFragment(FragmentType.ALERTS, context = context)
                }
                R.id.nav_settings -> {
                    goToFragment(FragmentType.SETTINGS)
                }
                R.id.nav_home -> {
                    goToFragment(FragmentType.PRICES)
                }
            }
        }

        fun goToFragment(fragmentType: FragmentType, context: Context? = null) {
            val fragment = when (fragmentType) {

                FragmentType.BTC_CHART -> if (btcChartFragment != null ) { btcChartFragment } else {
                    //TODO: confirm account is not null
                    val account = Account.btcAccount!!
                    ChartFragment.newInstance(account)
                }
                FragmentType.BCH_CHART -> if (bchChartFragment != null ) { bchChartFragment } else {
                    val account = Account.bchAccount!!
                    ChartFragment.newInstance(account)
                }
                FragmentType.ETH_CHART -> if (ethChartFragment != null ) { ethChartFragment } else {
                    val account = Account.ethAccount!!
                    ChartFragment.newInstance(account)
                }
                FragmentType.LTC_CHART -> if (ltcChartFragment != null ) { ltcChartFragment } else {
                    val account = Account.ltcAccount!!
                    ChartFragment.newInstance(account)
                }
                FragmentType.ACCOUNT -> if (accountsFragment != null ) { accountsFragment } else {
                    AccountsFragment.newInstance()
                }
                FragmentType.SEND -> if (sendFragment != null ) { sendFragment } else {
                    SendFragment.newInstance()
                }
                FragmentType.ALERTS -> if (alertsFragment != null ) { alertsFragment } else {
                    AlertsFragment.newInstance(context!!)
                }
                FragmentType.SETTINGS -> if (settingsFragment != null ) { settingsFragment } else {
                    SettingsFragment.newInstance()
                }
                FragmentType.PRICES -> if (pricesFragment != null ) { pricesFragment } else {
                    PricesFragment.newInstance()
                }
                FragmentType.TRADE -> {
                    println("Do not use this function for tradeFragments")
                    null
                }
            }
            if (fragment != null) {
                val tag = fragmentType.toString()
                goToFragment(fragment, tag)
            } else {
                println("Error switching fragments")
            }
        }

        fun goToFragment(fragment: RefreshFragment, tag: String) {
            currentFragment = fragment
//            if (Companion.fragmentManager.fragments.isEmpty()) {
                Companion.fragmentManager
                        .beginTransaction()
                        .add(R.id.fragment_container, fragment, tag)
                        .addToBackStack(tag)
                        .commit()
//            } else {
//                Companion.fragmentManager
//                        .beginTransaction()
//                        .replace(R.id.fragment_container, fragment, tag)
//                        .addToBackStack(tag)
//                        .commit()
//            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        setSupportFragmentManager(supportFragmentManager)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        progressDialog = indeterminateProgressDialog("")
        progressDialog?.dismiss()

        swipeRefreshLayout = swipe_refresh_layout
        swipeRefreshLayout.onRefresh {
            if (currentFragment != null) {
                currentFragment?.refresh { endRefresh() }
            } else {
                swipeRefreshLayout.isRefreshing = false
            }
        }
        val prefs = Prefs(this)
        if (!prefs.shouldAutologin) {
            returnToLogin()
        } else if (Account.list.size > 0) {
            goHome()
        } else {
            signIn()
        }
    }

   // private fun goHome(onFailure: (result: Result.Failure<String, FuelError>) -> Unit) {
   private fun goHome() {
        loopThroughAlerts()
        runAlarms()
        goToFragment(FragmentType.PRICES)
    }


    private fun signIn() {
        val prefs = Prefs(this)

        val passphrase = prefs.passphrase
        val apiKeyEncrypted = prefs.apiKey
        val apiSecretEncrypted = prefs.apiSecret

        if ((apiKeyEncrypted != null) && (apiSecretEncrypted != null)) {
            val iv = ByteArray(16)
            val encryption = Encryption.getDefault(passphrase, Constants.salt, iv)

            val apiKey = encryption.decryptOrNull(apiKeyEncrypted)
            val apiSecret = encryption.decryptOrNull(apiSecretEncrypted)

            if ((apiKey != null) && (apiSecret != null)) {
                GdaxApi.credentials = ApiCredentials(passphrase, apiKey, apiSecret)
                Account.getAccounts({ toast("Error!")}) {
                    goHome()
                }
            } else {
                returnToLogin()
            }
        } else {
            returnToLogin()
        }
    }


    private fun returnToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra(Constants.logout, true)
        startActivity(intent)
    }

    private fun endRefresh() {
        swipeRefreshLayout.isRefreshing = false
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()

            val fragmentManager = supportFragmentManager
            if (fragmentManager.backStackEntryCount > 0) {
                val fragmentTag = fragmentManager.getBackStackEntryAt(fragmentManager.backStackEntryCount - 1).name
                currentFragment = fragmentManager.findFragmentByTag(fragmentTag) as RefreshFragment
                currentFragment?.refresh { endRefresh() }
            } else {
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.putExtra(Constants.exit, true)
                startActivity(intent)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun updatePrices(onComplete: () -> Unit) {
        var tickersUpdated = 0
        val accountListSize = Account.list.size
        val onFailure = { result: Result.Failure<String, FuelError> ->  println("Error!: ${result.error}") }
        for (account in Account.list) {
            GdaxApi.ticker(account.product.id).executeRequest(onFailure) { result ->
                val ticker: ApiTicker = Gson().fromJson(result.value, object : TypeToken<ApiTicker>() {}.type)
                val price = ticker.price.toDoubleOrNull()
                if (price != null) {
                    account.product.price = price
                }
                tickersUpdated++
                if (tickersUpdated == accountListSize) {
                    onComplete()
                }
            }
        }
    }

    private fun runAlarms() {
        val handler = Handler()

        val runnable = Runnable {
            updatePrices {
                loopThroughAlerts()
                runAlarms()
            }
        }

        //TODO: add variable time checking, and run on launch
        //TODO: (ideally run on system launch)
        handler.postDelayed(runnable, (TimeInSeconds.oneMinute * 1000).toLong())
    }

    fun loopThroughAlerts() {
        val prefs = Prefs(this)
        val alerts = prefs.alerts
        for (alert in alerts) {
            if (!alert.hasTriggered) {
                var currentPrice = Account.forCurrency(alert.currency)?.product?.price
                if (alert.triggerIfAbove && (currentPrice != null) && (currentPrice >= alert.price)) {
                    triggerAlert(alert)
                } else if (!alert.triggerIfAbove && (currentPrice != null) && (currentPrice <= alert.price)) {
                    triggerAlert(alert)
                }
            }
        }
    }

    private fun triggerAlert(alert: Alert) {
        val prefs = Prefs(this)
        prefs.removeAlert(alert)

        val overUnder = when(alert.triggerIfAbove) {
            true  -> "over"
            false -> "under"
        }
        val notificationString = "${alert.currency} is $overUnder ${alert.price.fiatFormat()}"
        val intent = Intent(this, this.javaClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this)
                .setContentText(notificationString)
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)

        notificationManager?.notify(0, notificationBuilder.build())
    }



    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        goToNavigationId(item.itemId, this)

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

}
