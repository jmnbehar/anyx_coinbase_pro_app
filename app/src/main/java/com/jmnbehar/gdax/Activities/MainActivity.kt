package com.jmnbehar.gdax.Activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.NotificationCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.Fragments.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var notificationManager: NotificationManager? = null

    object Constants {
        val alertChannelId = "com.jmnbehar.gdax.alerts"
    }
    companion object {
        var currentFragment: Fragment? = null
        lateinit var apiProductList: List<ApiProduct>
        lateinit var fragmentManager: FragmentManager
        fun newIntent(context: Context, result: String): Intent {
            val intent = Intent(context, MainActivity::class.java)

            val gson = Gson()

            val unfilteredApiProductList: List<ApiProduct> = gson.fromJson(result, object : TypeToken<List<ApiProduct>>() {}.type)

            apiProductList = unfilteredApiProductList.filter {
                s -> s.quote_currency == "USD" && s.base_currency != "BCH"
            }

            return intent
        }

        fun setSupportFragmentManager(fragmentManager: FragmentManager) {
            this.fragmentManager = fragmentManager
        }


        fun goToFragment(fragment: Fragment, tag: String) {
            currentFragment = fragment
            if (fragmentManager.fragments.isEmpty()) {
                fragmentManager
                        .beginTransaction()
                        .add(R.id.fragment_container, fragment, tag)
                        .addToBackStack(tag)
                        .commit()
            } else {
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment, tag)
                        .addToBackStack(tag)
                        .commit()
            }
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

        createNotificationChannel(Constants.alertChannelId, "Alerts", "Alerts go here")

        if (savedInstanceState == null) {
            getCandles()
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
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

    fun getCandles(time: Int = TimeInSeconds.oneDay) {
        for (product in apiProductList) {
            Candle.getCandles(product.id, time, { candleList ->

                val newProduct = Product(product, candleList)
                Product.addToList(newProduct)
               // println("pl size: ${Product.list.size},    apl size: ${apiProductList.size}")
                if (Product.listSize == apiProductList.size) {
                    loopThroughAlerts()
                    runAlarms()
                    Account.getAccountInfo { goToFragment(PricesFragment.newInstance(), "chart") }
                }
            })
        }
    }

    fun updatePrices() {
        for (account in Account.list) {
            GdaxApi.ticker(account.product.id).executeRequest { result ->
                when (result) {
                    is Result.Failure -> {
                        println("Error!: ${result.error}")
                    }
                    is Result.Success -> {
                        val gson = Gson()
                        val ticker: ApiTicker = gson.fromJson(result.value, object : TypeToken<ApiTicker>() {}.type)
                        val price = ticker.price.toDoubleOrNull()
                        if (price != null) {
                            account.product.price = price
                        }
                    }
                }
            }
        }
    }

    private fun runAlarms() {
        val handler = Handler()

        val runnable = Runnable {
            updatePrices()

            loopThroughAlerts()

            runAlarms()
        }

        //TODO: add variable time checking, and run on launch
        //TODO: (ideally run on system launch)
        handler.postDelayed(runnable, (TimeInSeconds.fifteenMinutes * 1000).toLong())
    }

    fun loopThroughAlerts() {
        val prefs = Prefs(this)
        val alerts = prefs.alerts
        for (alert in alerts) {
            if (!alert.hasTriggered) {
                var currentPrice = Account.forCurrency(alert.currency)?.product?.price
                if (alert.triggerIfAbove && (currentPrice != null) && (currentPrice >= alert.price)) {
                    triggerAlert(alert)
                } else if ((currentPrice != null) && (currentPrice <= alert.price)) {
                    triggerAlert(alert)
                }
            }
        }
    }

    fun triggerAlert(alert: Alert) {
        val prefs = Prefs(this)
        prefs.removeAlert(alert)

        val overUnder = when(alert.triggerIfAbove) {
            true  -> "over"
            false -> "under"
        }
        val notificationString = "${alert.currency.toString()} is $overUnder ${alert.price}"
        val intent = Intent(this, this.javaClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, Constants.alertChannelId)
                .setContentText(notificationString)
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)


        notificationManager?.notify(0, notificationBuilder.build())
    }

    private fun createNotificationChannel(id: String, name: String,
                                          description: String) {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(id, name, importance)

        channel.description = description
        channel.enableLights(true)
        channel.lightColor = Color.RED
        channel.enableVibration(true)
        channel.vibrationPattern =
                longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        notificationManager?.createNotificationChannel(channel)
    }





    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_btc -> {
                val btcAccount = Account.btcAccount
                if (btcAccount != null) {
                    goToFragment(ChartFragment.newInstance(btcAccount), "BTC Chart")
                } else {
                    Account.getAccountInfo { goToFragment(ChartFragment.newInstance(Account.btcAccount!!), "BTC Chart") }
                }
            }
            R.id.nav_eth -> {
                val ethAccount = Account.ethAccount
                if (ethAccount != null) {
                    goToFragment(ChartFragment.newInstance(ethAccount), "ETH Chart")
                } else {
                    Account.getAccountInfo { goToFragment(ChartFragment.newInstance(Account.ethAccount!!), "ETH Chart") }
                }
            }
            R.id.nav_ltc -> {
                val ltcAccount = Account.ltcAccount
                if (ltcAccount != null) {
                    goToFragment(ChartFragment.newInstance(ltcAccount), "LTC Chart")
                } else {
                    Account.getAccountInfo { goToFragment(ChartFragment.newInstance(Account.ltcAccount!!), "LTC Chart") }
                }
            }
            R.id.nav_accounts -> {
                goToFragment(AccountsFragment.newInstance(), "AccountList")
            }
            R.id.nav_send -> {
                goToFragment(SendFragment.newInstance(), "Send")
            }
            R.id.nav_alerts -> {
                goToFragment(AlertsFragment.newInstance(this), "Alerts")
            }
            R.id.nav_settings -> {
                goToFragment(RedFragment.newInstance(), "red page = killer feature")
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
