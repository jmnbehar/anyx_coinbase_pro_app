package com.anyexchange.anyx.Activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.ColorFilter
import android.os.Build
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.NavigationView
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.Spinner
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.anyexchange.anyx.Adapters.NavigationSpinnerAdapter
import com.anyexchange.anyx.Classes.*
import com.anyexchange.anyx.Fragments.Main.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.contentView
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast
import se.simbio.encryption.Encryption


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var notificationManager: NotificationManager? = null
    lateinit var spinnerNav: Spinner
    var defaultSpinnerColorFilter: ColorFilter? = null

    enum class FragmentType {
        BTC_CHART,
        BCH_CHART,
        ETH_CHART,
        LTC_CHART,
        ACCOUNT,
        SEND,
        ALERTS,
        DEPOSIT,
        WITHDRAW,
        SETTINGS,
        TRADE,
        HOME,
        OTHER;


        override fun toString() : String {
            return when (this) {
                BTC_CHART -> "CHART"
                BCH_CHART -> "CHART"
                ETH_CHART -> "CHART"
                LTC_CHART -> "CHART"
                ACCOUNT -> "ACCOUNT"
                SEND -> "SEND"
                ALERTS -> "ALERTS"
                DEPOSIT -> "DEPOSIT"
                WITHDRAW -> "WITHDRAW"
                SETTINGS -> "SETTINGS"
                TRADE -> "TRADE"
                HOME -> "HOME"
                OTHER -> "OTHER"
            }
        }

    }

    companion object {

        fun newIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }


    }

    private var currentFragment: RefreshFragment? = null

    private lateinit var progressBar: ProgressBar
    private lateinit var progressBarLayout: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        onRestoreInstanceState(savedInstanceState)

        setSupportActionBar(toolbar)

        var toggle = object : ActionBarDrawerToggle(
                this, drawer_layout, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                hideSoftKeyboard()
            }
        }

        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        progressBarLayout = progress_bar_layout
        progressBar = progress_bar

        spinnerNav = toolbar_spinner
        defaultSpinnerColorFilter = spinnerNav.background.colorFilter

        //TODO: list currencies better
        val currencies = Currency.cryptoList
        val spinnerNavAdapter = NavigationSpinnerAdapter(this, R.layout.list_row_coinbase_account, currencies)
        spinnerNavAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNav.adapter = spinnerNavAdapter

        //TODO: show spinner later
        spinnerNav.visibility = View.GONE

        val prefs = Prefs(this)

        if (Account.list.size > 0) {
            goHome()
            setDrawerMenu()
        } else if (!prefs.shouldAutologin) {
            returnToLogin()
        } else {
            signIn()
        }
    }

    val GDAX_API_KEY = "GDAX_API_KEY"
    val GDAX_API_PASS = "GDAX_API_PASS"
    val GDAX_API_SECRET = "GDAX_API_SECRET"
    val ACCOUNT_LIST = "ACCOUNT_LIST"

    private fun setDrawerMenu() {
        nav_view.menu.clear()
        if (GdaxApi.isLoggedIn) {
            nav_view.inflateMenu(R.menu.activity_main_drawer)
        } else {
            nav_view.inflateMenu(R.menu.activity_main_drawer_logged_out)
        }

    }
    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        val gdaxApiKey = savedInstanceState?.getString(GDAX_API_KEY)
        val gdaxApiSecret = savedInstanceState?.getString(GDAX_API_SECRET)
        val gdaxApiPass = savedInstanceState?.getString(GDAX_API_PASS)
        if(gdaxApiKey != null && gdaxApiSecret != null && gdaxApiPass != null) {
            if (GdaxApi.credentials == null) {
                val prefs = Prefs(this)
                val isApiKeyValid = prefs.isApiKeyValid(gdaxApiKey)

                GdaxApi.credentials = GdaxApi.ApiCredentials(gdaxApiKey, gdaxApiSecret, gdaxApiPass, isApiKeyValid)
            }
        }
//        val accountList = savedInstanceState?.getParcelableArray(ACCOUNT_LIST) as? Array<Account>?
//        if (accountList != null && accountList.isNotEmpty()) {
//            Account.list = accountList.toMutableList()
//        }
    }

    // invoked when the activity may be temporarily destroyed, save the instance state here
    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.run {
            putString(GDAX_API_KEY, GdaxApi.credentials?.apiKey)
            putString(GDAX_API_SECRET, GdaxApi.credentials?.apiSecret)
            putString(GDAX_API_PASS, GdaxApi.credentials?.apiPassPhrase)
//            putParcelableArray(ACCOUNT_LIST, Account.list.toTypedArray() )
        }
        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState)
    }

//    override fun onResume() {
//        super.onResume()
//    }

   // private fun goHome(onFailure: (result: Result.Failure<String, FuelError>) -> Unit) {
   private fun goHome() {
        loopThroughAlerts()
        goToFragment(FragmentType.HOME)
    }

    var progressBarCount = 0
    fun showProgressBar() {
        progressBarCount++
        progressBarLayout.visibility = View.VISIBLE
        val prefs = Prefs(this)
        val color = if (prefs.isDarkModeOn) {
            ContextCompat.getColor(this, R.color.transparent_light_bg)
        } else {
            ContextCompat.getColor(this, R.color.transparent_dark_bg)
        }
        progressBar.setBackgroundColor(color)
    }

    fun dismissProgressBar() {
        progressBarCount--
        if (progressBarCount <= 0) {
            progressBarCount = 0
            progressBarLayout.visibility = View.GONE
        }
    }


    private fun signIn() {
        val prefs = Prefs(this)

        val apiKey = prefs.apiKey
        val apiSecret = prefs.apiSecret
        val passphraseEncrypted  = prefs.passphrase

        val iv = ByteArray(16)
        val encryption = Encryption.getDefault(apiKey, apiSecret + Constants.salt, iv)
        val passphrase = encryption.decryptOrNull(passphraseEncrypted)
        if ((apiKey != null) && (apiSecret != null) && (passphrase != null)) {
            val isApiKeyValid = prefs.isApiKeyValid(apiKey)
            GdaxApi.credentials = GdaxApi.ApiCredentials(apiKey, apiSecret, passphrase, isApiKeyValid)
            showProgressBar()
            GdaxApi.accounts().getAllAccountInfo(this, { _ ->
                toast("Error!")
                dismissProgressBar()
                returnToLogin()
            }, {
                dismissProgressBar()
                setDrawerMenu()
                goHome()
            })
        } else {
            returnToLogin()
        }
    }


    private fun returnToLogin() {
        val intent = Intent(this, com.anyexchange.anyx.Activities.LoginActivity::class.java)
        intent.putExtra(Constants.logout, true)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            //super.onBackPressed()

            if (supportFragmentManager.backStackEntryCount > 1) {
                supportFragmentManager.popBackStack()
                val prevFragmentTag = supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 2).name

                currentFragment = supportFragmentManager.findFragmentByTag(prevFragmentTag) as RefreshFragment
            } else {
//                val intent = Intent(this, LoginActivity::class.java)
//                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
//                intent.putExtra(Constants.exit, true)
//                startActivity(intent)
                finishAffinity()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        // menuInflater.inflate(R.menu.main, menu)
        return false
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

    fun updatePrices(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        var tickersUpdated = 0
        val accountListSize = Account.list.size
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

    fun loopThroughAlerts() {
        val prefs = Prefs(this)
        val alerts = prefs.alerts
        for (alert in alerts) {
            if (!alert.hasTriggered) {
                val currentPrice = Account.forCurrency(alert.currency)?.product?.price
                if (alert.triggerIfAbove && (currentPrice != null) && (currentPrice >= alert.price)) {
                    triggerAlert(alert)
                } else if (!alert.triggerIfAbove && (currentPrice != null) && (currentPrice <= alert.price)) {
                    triggerAlert(alert)
                }
            }
        }
        if (currentFragment is AlertsFragment) {
            (currentFragment as AlertsFragment).alertAdapter?.alerts = prefs.alerts.toList()
            (currentFragment as AlertsFragment).alertAdapter?.notifyDataSetChanged()
        }
    }

    private fun triggerAlert(alert: Alert) {
        val channelId = "Price_Alerts"
        if (notificationManager == null) {
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create the NotificationChannel, but only on API 26+ because
                // the NotificationChannel class is new and not in the support library
                val name = getString(R.string.channel_name)
                val description = getString(R.string.channel_description)
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(channelId, name, importance)
                channel.description = description
                // Register the channel with the system
                notificationManager?.createNotificationChannel(channel)
            }
        }


        val overUnder = when(alert.triggerIfAbove) {
            true  -> "over"
            false -> "under"
        }
        val intent = Intent(this, this.javaClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
//        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationTitle = "${alert.currency.fullName} price alert"
        val notificationText = "${alert.currency} is $overUnder ${alert.price.fiatFormat()}"
        val priceAlertGroupTag = "PriceAlert"

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.anyx_notification_icon)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setGroup(priceAlertGroupTag)
//                .setSound(defaultSoundUri)

        val notificationTag = "PriceAlert_" + alert.currency.toString() + "_" + alert.price
        notificationManager?.notify(notificationTag, 0, notificationBuilder.build())
        val prefs = Prefs(this)
        prefs.removeAlert(alert)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val fragmentType = when (item.itemId) {
            R.id.nav_send -> FragmentType.SEND
            R.id.nav_alerts -> FragmentType.ALERTS
            R.id.nav_deposit -> FragmentType.DEPOSIT
            R.id.nav_withdraw -> FragmentType.WITHDRAW
            R.id.nav_settings -> FragmentType.SETTINGS
            R.id.nav_home -> FragmentType.HOME
            else -> FragmentType.HOME
        }
        val currentFragmentType = when (currentFragment) {
            is SendFragment -> FragmentType.SEND
            is AlertsFragment -> FragmentType.ALERTS
            is TransferInFragment -> FragmentType.DEPOSIT
            is TransferOutFragment -> FragmentType.WITHDRAW
            is TransferInCoinbaseFragment -> FragmentType.DEPOSIT
            is TransferOutCoinbaseFragment -> FragmentType.WITHDRAW
            is SettingsFragment -> FragmentType.SETTINGS
            is HomeFragment -> FragmentType.HOME
            is ChartFragment -> FragmentType.BTC_CHART  //TODO: refine
            is TradeFragment -> FragmentType.TRADE
            else -> FragmentType.OTHER
        }
        if (fragmentType != currentFragmentType) {
            goToFragment(fragmentType)
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun goToChartFragment(currency: Currency) {
        when (currency) {
            Currency.BTC -> goToFragment(FragmentType.BTC_CHART)
            Currency.BCH -> goToFragment(FragmentType.BCH_CHART)
            Currency.ETH -> goToFragment(FragmentType.ETH_CHART)
            Currency.LTC -> goToFragment(FragmentType.LTC_CHART)
            Currency.USD -> {}
        }
    }
    private fun chartFragment(currency: Currency) : ChartFragment? {
        val account = Account.forCurrency(currency)
        return if (account == null) {
            null
        } else {
            ChartFragment.newInstance(account)
        }

    }

    private fun goToFragment(fragmentType: FragmentType) {
        val fragment : RefreshFragment? = when (fragmentType) {
            FragmentType.BTC_CHART -> {
                chartFragment(Currency.BTC)
            }
            FragmentType.BCH_CHART -> {
                chartFragment(Currency.BCH)
            }
            FragmentType.ETH_CHART -> {
                chartFragment(Currency.ETH)
            }
            FragmentType.LTC_CHART -> {
                chartFragment(Currency.LTC)
            }
            FragmentType.ACCOUNT -> AccountsFragment.newInstance()
            FragmentType.SEND -> {
                if (!GdaxApi.isLoggedIn) {
                    //do nothing
                    toast("Please log in")
                    null
                } else if (GdaxApi.credentials?.isValidated != true) {
                    //do nothing
                    toast("Please validate your account in Settings to send crypto assets")
                    null
                } else {
                    SendFragment.newInstance()
                }
            }
            FragmentType.ALERTS -> AlertsFragment.newInstance(this)
            FragmentType.DEPOSIT -> {
                //TODO: get bank data here
                if (!GdaxApi.isLoggedIn) {
                    toast("Please log in")
                } else if (GdaxApi.credentials?.isValidated == null) { //(GdaxApi.credentials?.isValidated == null) {
                    toast("Please validate your account in Settings")
                } else if (GdaxApi.credentials?.isValidated == false) { // (GdaxApi.credentials?.isValidated == false) {
                    toast("Please use an API Key with all permissions.")
                } else {
                    showProgressBar()
                    GdaxApi.coinbaseAccounts().linkToAccounts({
                        dismissProgressBar()
                        toast("Can't access coinbase accounts")
                    }, {
                        //TODO: switch back to regular depositFragment
//                        val depositFragment = TransferInFragment.newInstance()
                        val depositFragment = TransferInCoinbaseFragment.newInstance()

                        val tag = fragmentType.toString()
                        goToFragment(depositFragment, tag)
                    })
                }
                null
            }
            FragmentType.WITHDRAW -> {
                //TODO: get bank data here

                if (!GdaxApi.isLoggedIn) {
                    toast("Please log in")
                } else if (GdaxApi.credentials?.isValidated == null) { //(GdaxApi.credentials?.isValidated == null) {
                    toast("Please validate your account in Settings")
                } else if (GdaxApi.credentials?.isValidated == false) { // (GdaxApi.credentials?.isValidated == false) {
                    toast("Please use an API Key with all permissions.")
                } else {
                    showProgressBar()
                    GdaxApi.coinbaseAccounts().linkToAccounts({
                        dismissProgressBar()
                        toast("Can't access coinbase accounts")
                    }, {
                        //TODO: switch back to regular TransferOutFragment
//                        val withdrawFragment = TransferOutFragment.newInstance()
                        val withdrawFragment = TransferOutCoinbaseFragment.newInstance()

                        val tag = fragmentType.toString()
                        goToFragment(withdrawFragment, tag)
                        //dismissProgressBar()
                    })
                }
                null
            }
            FragmentType.SETTINGS -> SettingsFragment.newInstance()
            FragmentType.HOME -> HomeFragment.newInstance()
            FragmentType.TRADE -> {
                println("Do not use this function for tradeFragments")
                null
            }
            FragmentType.OTHER -> null
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
        if (supportFragmentManager.backStackEntryCount == 0) {
//            if (Companion.fragmentManager.fragments.isEmpty()) {
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container, fragment, tag)
                    .addToBackStack(tag)
                    .commitAllowingStateLoss()
        } else {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment, tag)
                    .addToBackStack(tag)
                    .commitAllowingStateLoss()
        }
    }


    fun hideSoftKeyboard() {
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        if (currentFocus != null && inputManager != null) {
            inputManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
            inputManager.hideSoftInputFromInputMethod(currentFocus.windowToken, 0)
        }
    }

//    private fun showPopup(titleString: String, messageString: String, positiveText: String, positiveAction: () -> Unit, negativeText: String? = null, negativeAction: () -> Unit = {}) {
//        alert {
//            title = titleString
//            message = messageString
//            positiveButton(positiveText) { positiveAction() }
//            if (negativeText != null) {
//                negativeButton(negativeText) { negativeAction() }
//            }
//        }.show()
//    }

}
