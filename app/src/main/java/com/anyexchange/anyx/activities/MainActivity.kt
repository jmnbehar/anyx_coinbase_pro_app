package com.anyexchange.anyx.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.ColorFilter
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.NavigationView
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.Spinner
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.anyexchange.anyx.adapters.NavigationSpinnerAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.fragments.main.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.toast
import se.simbio.encryption.Encryption

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var notificationManager: NotificationManager? = null
    lateinit var spinnerNav: Spinner
    var defaultSpinnerColorFilter: ColorFilter? = null

    enum class FragmentType {
        //TODO: only use 1 chart thing
        BTC_CHART,
        BCH_CHART,
        ETH_CHART,
        LTC_CHART,
        ACCOUNT,
        SEND,
        ALERTS,
        TRANSFER_IN,
        TRANSFER_OUT,
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
                TRANSFER_IN -> "TRANSFER_IN"
                TRANSFER_OUT -> "TRANSFER_OUT"
                SETTINGS -> "SETTINGS"
                TRADE -> "TRADE"
                HOME -> "HOME"
                OTHER -> "OTHER"
            }
        }

        companion object {
            fun forString(tag: String) : FragmentType {
                return when (tag) {
                    BTC_CHART.toString() -> BTC_CHART
                    ACCOUNT.toString() -> ACCOUNT
                    SEND.toString() -> SEND
                    ALERTS.toString() -> ALERTS
                    TRANSFER_IN.toString() -> TRANSFER_IN
                    TRANSFER_OUT.toString() -> TRANSFER_OUT
                    SETTINGS.toString() -> SETTINGS
                    TRADE.toString() -> TRADE
                    HOME.toString() -> HOME
                    else -> OTHER
                }
            }

            fun fromFragment(fragment: RefreshFragment?) : FragmentType {
                return when (fragment) {
                    is ChartFragment -> {
                        when (ChartFragment.account?.currency ?: Currency.BTC) {
                            Currency.BTC -> BTC_CHART
                            Currency.BCH -> BCH_CHART
                            Currency.ETH -> ETH_CHART
                            Currency.LTC -> LTC_CHART
                            Currency.USD,
                            Currency.EUR,
                            Currency.GBP -> BTC_CHART   //error
                        }
                    }
                    is AccountsFragment -> ACCOUNT
                    is SendFragment -> SEND
                    is AlertsFragment -> ALERTS
                    is TransferInFragment -> TRANSFER_IN
                    is TransferOutFragment -> TRANSFER_OUT
                    is SettingsFragment -> SETTINGS
                    is TradeFragment -> TRADE
                    is HomeFragment -> HOME
                    else -> OTHER
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }

    private var currentFragment: RefreshFragment? = null

    private var dataFragment: DataFragment? = null

    private lateinit var progressBar: ProgressBar
    private lateinit var progressBarLayout: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = object : ActionBarDrawerToggle(
                this, drawer_layout, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                hideSoftKeyboard()
            }
        }

        dataFragment = supportFragmentManager.findFragmentByTag(Constants.dataFragmentTag) as? DataFragment

        // create the fragment and data the first time
        if (dataFragment == null) {
            // add the fragment
            dataFragment = DataFragment.newInstance()
            supportFragmentManager.beginTransaction().add(dataFragment, Constants.dataFragmentTag).commit()
        }
        dataFragment?.restoreData(this)

        toolbar.setNavigationOnClickListener {
            drawer_layout.openDrawer(Gravity.START, true)
        }
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)

        progressBarLayout = progress_bar_layout
        progressBar = progress_bar

        spinnerNav = toolbar_spinner

        val prefs = Prefs(this)
        prefs.shouldSavePassphrase = true

        defaultSpinnerColorFilter = spinnerNav.background.colorFilter
        val currencies = Currency.cryptoList
        val spinnerNavAdapter = NavigationSpinnerAdapter(this, R.layout.list_row_coinbase_account, currencies)
        spinnerNavAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNav.adapter = spinnerNavAdapter

        if (savedInstanceState == null) {
            spinnerNav.visibility = View.GONE
            if (!prefs.shouldAutologin) {
                returnToLogin()
            } else if (Account.cryptoAccounts.isNotEmpty() && Account.fiatAccounts.isNotEmpty()) {
                goHome()
                setDrawerMenu()
            } else {
                signIn()
            }
        } else {
            setDrawerMenu()
        }
    }

    private fun setDrawerMenu() {
        nav_view.menu.clear()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val prefs = Prefs(this)
        if (prefs.isLoggedIn) {
            nav_view.inflateMenu(R.menu.activity_main_drawer)
        } else {
            nav_view.inflateMenu(R.menu.activity_main_drawer_logged_out)
        }

    }
    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        dataFragment?.restoreData(this)
        setDrawerMenu()
        if (savedInstanceState != null) {
            super.onRestoreInstanceState(savedInstanceState)
            val fragmentTag = savedInstanceState.getString("FRAGMENT_TAG") ?: ""
            val fragmentType = FragmentType.forString(fragmentTag)
            goToFragment(fragmentType)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)

        val fragmentTag = supportFragmentManager.fragments.lastOrNull()?.tag ?: ""
        outState?.putString("FRAGMENT_TAG", fragmentTag)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        val fragmentTag = supportFragmentManager.fragments.lastOrNull()?.tag ?: ""
        outState?.putString("FRAGMENT_TAG", fragmentTag)
    }

    override fun onPause() {
        super.onPause()
        dataFragment?.backupData()
    }

    override fun onDestroy() {
        super.onDestroy()
        dataFragment?.backupData()
    }

   // private fun goHome(onFailure: (result: Result.Failure<String, FuelError>) -> Unit) {
   private fun goHome() {
        loopThroughAlerts()
        goToFragment(FragmentType.HOME)
    }

    private var progressBarCount = 0
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
            CBProApi.credentials = CBProApi.ApiCredentials(apiKey, apiSecret, passphrase, isApiKeyValid)
            showProgressBar()
            CBProApi.accounts().getAllAccountInfo(this, { _ ->
                toast(R.string.error_message)
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
        val intent = Intent(this, com.anyexchange.anyx.activities.LoginActivity::class.java)
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
        val accountListSize = Account.cryptoAccounts.size
        for (account in Account.cryptoAccounts) {
            CBProApi.ticker(account.product.id).get(onFailure) {
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
                val currentPrice = Account.forCurrency(alert.currency)?.product?.defaultPrice
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
        val notificationText = "${alert.currency} is $overUnder ${alert.price.fiatFormat(Account.defaultFiatCurrency)}"
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
            R.id.nav_deposit -> FragmentType.TRANSFER_IN
            R.id.nav_withdraw -> FragmentType.TRANSFER_OUT
            R.id.nav_settings -> FragmentType.SETTINGS
            R.id.nav_home -> FragmentType.HOME
            else -> FragmentType.HOME
        }
        val currentFragmentType = FragmentType.fromFragment(currentFragment)

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
            Currency.USD, Currency.EUR, Currency.GBP -> {}
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
        val prefs = Prefs(this)
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

                if (!prefs.isLoggedIn) {
                    toast(R.string.toast_please_login_message)
                    null
                } else if (CBProApi.credentials?.isValidated == null) {
                    toast(R.string.toast_please_validate_message)
                    null
                } else if (CBProApi.credentials?.isValidated == false) {
                    toast(R.string.toast_missing_permissions_message)
                    null
                } else {
                    SendFragment.newInstance()
                }
            }
            FragmentType.ALERTS -> AlertsFragment.newInstance()
            FragmentType.TRANSFER_IN -> {
                if (!prefs.isLoggedIn) {
                    toast(R.string.toast_please_login_message)
                } else if (CBProApi.credentials?.isValidated == null) {
                    toast(R.string.toast_please_validate_message)
                } else if (CBProApi.credentials?.isValidated == false) {
                    toast(R.string.toast_missing_permissions_message)
                } else {
                    showProgressBar()
                    val depositFragment = TransferInFragment.newInstance()
                    depositFragment.refresh {didSucceed ->
                        if (didSucceed) {
                            val tag = fragmentType.toString()
                            depositFragment.skipNextRefresh = true
                            goToFragment(depositFragment, tag)
                        } else {
                            toast(R.string.error_message)
                        }
                    }
                }
                null
            }
            FragmentType.TRANSFER_OUT -> {
                if (!prefs.isLoggedIn) {
                    toast(R.string.toast_please_login_message)
                } else if (CBProApi.credentials?.isValidated == null) {
                    toast(R.string.toast_please_validate_message)
                } else if (CBProApi.credentials?.isValidated == false) {
                    toast(R.string.toast_missing_permissions_message)
                } else {
                    showProgressBar()
                    val withdrawFragment = TransferOutFragment.newInstance()
                    withdrawFragment.refresh {didSucceed ->
                        if (didSucceed) {
                            val tag = fragmentType.toString()
                            withdrawFragment.skipNextRefresh = true
                            goToFragment(withdrawFragment, tag)
                        } else {
                            toast(R.string.error_message)
                        }
                    }
                }
                null
            }
            FragmentType.SETTINGS -> SettingsFragment.newInstance()
            FragmentType.HOME -> HomeFragment.newInstance()
            FragmentType.TRADE -> {
                null
            }
            FragmentType.OTHER -> null
        }
        if (fragment != null) {
            val tag = fragmentType.toString()
            goToFragment(fragment, tag)
        } else {
            toast("Error switching fragments")
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
