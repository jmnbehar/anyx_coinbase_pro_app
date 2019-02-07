package com.anyexchange.anyx.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.NavigationView
import android.support.v4.app.FragmentManager
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.anyexchange.anyx.adapters.spinnerAdapters.NavigationSpinnerAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.fragments.main.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.api.ApiInitData
import com.anyexchange.anyx.api.CBProApi
import com.anyexchange.anyx.classes.Constants.CHART_CURRENCY
import com.anyexchange.anyx.classes.Constants.CHART_STYLE
import com.anyexchange.anyx.classes.Constants.CHART_TIMESPAN
import com.anyexchange.anyx.classes.Constants.CHART_TRADING_PAIR
import com.anyexchange.anyx.api.AnyApi
import com.anyexchange.anyx.fragments.login.LoginFragment
import com.anyexchange.anyx.views.searchableSpinner.SearchableSpinner
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.toast
import se.simbio.encryption.Encryption

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    lateinit var spinnerNav: SearchableSpinner

    private var currentFragment: RefreshFragment? = null
    private var dataFragment: DataFragment? = null

    val apiInitData = ApiInitData(this) {
        returnToLogin()
    }

    private lateinit var progressBar: ProgressBar
    private lateinit var progressBarLayout: ConstraintLayout

    companion object {
        const val currentAppVersion = 17
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = object : ActionBarDrawerToggle(
                this, drawer_layout, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            override fun onDrawerOpened(drawerView: View) {
                spinnerNav.hideEdit()
                hideSoftKeyboard()
                super.onDrawerOpened(drawerView)
            }
        }

        dataFragment = supportFragmentManager.findFragmentByTag(Constants.dataFragmentTag) as? DataFragment

        // create the fragment and data the first time
        if (dataFragment == null) {
            // add the fragment
            dataFragment = DataFragment.newInstance()
            supportFragmentManager.beginTransaction().add(dataFragment!!, Constants.dataFragmentTag).commit()
        }
        dataFragment?.restoreData(this)

        if (!AutoStart.hasStarted) {
            AutoStart.scheduleCustomAlertJob(this)
        }
        toolbar?.setNavigationOnClickListener {
            drawer_layout.openDrawer(Gravity.START, true)
        }

        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)

        progressBarLayout = progress_bar_layout
        progressBar = progress_bar

        spinnerNav = toolbar_spinner

//        defaultSpinnerColorFilter = spinnerNav.background.colorFilter

        val currencies = Currency.cryptoList
        val spinnerNavAdapter = NavigationSpinnerAdapter(this, R.layout.list_row_spinner_nav, R.id.txt_currency, currencies)

        spinnerNav.setAdapter(spinnerNavAdapter)


        val prefs = Prefs(this)
        val isAppOutOfDate = (prefs.lastVersionCode < currentAppVersion)

        val onFailure: (Result.Failure<String, FuelError>) -> Unit = {
            //On Failure
            toast(R.string.error_message)
            returnToLogin()
        }
        val anyApi = AnyApi(apiInitData)
        if (savedInstanceState == null) {
            spinnerNav.visibility = View.GONE
            if (Account.areAccountsOutOfDate()) {
                signIn(false, onFailure, {
                    //OnSuccess
                    if (isAppOutOfDate) {
                        showProgressBar()
                        anyApi.getAllProducts(onFailure) {
                            anyApi.getAllAccounts(onFailure, {
                                prefs.lastVersionCode = currentAppVersion
                                dismissProgressBar()
                                goHome()
                                setDrawerMenu()
                            })
                        }
                    } else {
                        goHome()
                        setDrawerMenu()
                    }
                })
            } else if (isAppOutOfDate) {
                showProgressBar()
                anyApi.getAllProducts(onFailure) {
                    anyApi.getAllAccounts(onFailure, {
                        prefs.lastVersionCode = currentAppVersion
                        dismissProgressBar()
                        goHome()
                        setDrawerMenu()
                    })
                }
            } else {
                goHome()
                setDrawerMenu()
            }
            return
        } else {
            (intent?.extras?.get(Constants.GO_TO_CURRENCY) as? String)?.let {
                val currency = Currency(it)
                currency.addToList()
                goToChartFragment(currency)
            }
            checkAllResources(onFailure, { //OnSuccess
                //do something maybe?
            } )
        }
    }

    override fun onResume() {
        super.onResume()
        dataFragment?.restoreData(this)
    }

    override fun onPause() {
        super.onPause()
        dataFragment?.backupData()
    }

    override fun onTouchEvent(event: MotionEvent) : Boolean {
        if (spinnerNav.viewState == SearchableSpinner.ViewState.ShowingEditLayout) {
            spinnerNav.hideEdit()
        }
        return super.onTouchEvent(event);
    }

    private fun hideDrawerMenu() {
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    fun setDrawerMenu() {
        nav_view.menu.clear()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        val prefs = Prefs(this)
        if (prefs.isLoggedIn) {
            nav_view.inflateMenu(R.menu.activity_main_drawer)
            menu_login.visibility = View.GONE
            if (CBProApi.credentials?.isVerified == true) {
                menu_verify.visibility = View.GONE
            } else {
                menu_verify.visibility = View.VISIBLE
                menu_verify.setOnClickListener  { _ ->
                    if (CBProApi.credentials?.isVerified == true) {
                        toast("Already verified!")
                        setDrawerMenu()
                    } else {
                        goToVerify{ setDrawerMenu() }
                    }
                }
            }
        } else {
            nav_view.inflateMenu(R.menu.activity_main_drawer_logged_out)
            menu_login.visibility  = View.VISIBLE
            menu_verify.visibility = View.GONE

            menu_login.setOnClickListener {
                drawer_layout.closeDrawer(GravityCompat.START)
                goToFragment(FragmentType.LOGIN)
            }

        }
    }

    fun goToVerify(onComplete: (Boolean) -> Unit) {
        val intent = Intent(this, VerifyActivity::class.java)
        startActivity(intent)
        VerifyActivity.onComplete = onComplete
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        dataFragment?.restoreData(this)
        val chartCurrencyStr = savedInstanceState?.getString(CHART_CURRENCY) ?: ""
        val chartCurrency = Currency(chartCurrencyStr)
        ChartFragment.currency = chartCurrency
        setDrawerMenu()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        dataFragment?.backupData()
        outState?.putString(CHART_CURRENCY, ChartFragment.currency.toString())
        if (currentFragment is ChartFragment) {
            val chartFragment = currentFragment as ChartFragment
            outState?.putString(CHART_TRADING_PAIR, chartFragment.tradingPair.toString())
            outState?.putString(CHART_STYLE, chartFragment.chartStyle.toString())
            outState?.putLong(CHART_TIMESPAN, chartFragment.timespan.value())
        }
    }

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

    fun signIn(shouldSkipCredentials: Boolean, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        val prefs = Prefs(this)
        if (CBProApi.credentials == null) {
            if (!shouldSkipCredentials) {
                val apiKey = prefs.cbProApiKey
                val apiSecret = prefs.cbProApiSecret
                val passphraseEncrypted  = prefs.cbProPassphrase

                val iv = ByteArray(16)
                val encryption = Encryption.getDefault(apiKey, apiSecret + Constants.salt, iv)
                val passphrase = encryption.decryptOrNull(passphraseEncrypted)

                if ((apiKey != null) && (apiSecret != null) && (passphrase != null)) {
                    val isApiKeyValid = prefs.isApiKeyValid(apiKey)
                    CBProApi.credentials = CBProApi.ApiCredentials(apiKey, apiSecret, passphrase, isApiKeyValid)
                }
            } else if (!Account.areAccountsOutOfDate() && !Product.map.isEmpty()) {
                setDrawerMenu()
                onComplete()
                goHome()
                return
            }
        }
        checkAllResources(onFailure) {
            if (CBProApi.credentials != null) {
                prefs.isLoggedIn = true
            }
            setDrawerMenu()
            onComplete()
            goHome()
        }
    }

    private fun checkAllResources(onFailure: (Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        if (Product.map.isEmpty()) {
            updateAllProducts(onFailure) {
                if (CBProApi.credentials != null) {
                    updateAllAccounts(onFailure, onComplete)
                } else {
                    onComplete()
                }
            }
        } else if (Account.areAccountsOutOfDate() && CBProApi.credentials != null) {
            updateAllAccounts(onFailure, onComplete)
        } else {
            onComplete()
        }

    }

    private fun updateAllProducts(onFailure: (Result.Failure<String, FuelError>) -> Unit, onSuccess: () -> Unit) {
        AnyApi(apiInitData).getAllProducts(onFailure, onSuccess)
    }
    private fun updateAllAccounts(onFailure: (Result.Failure<String, FuelError>) -> Unit, onSuccess: () -> Unit) {
        val prefs = Prefs(this)
        showProgressBar()
        CBProApi.accounts(apiInitData).getAllAccountInfo({ error ->
            dismissProgressBar()
            onFailure(error)
        }, {
            dismissProgressBar()
            prefs.stashedProducts = Product.map.values.toList()
            setDrawerMenu()
            if (CBProApi.credentials == null) {
                val dataFragment = supportFragmentManager.findFragmentByTag(Constants.dataFragmentTag) as? DataFragment
                dataFragment?.destroyData(this)
                prefs.stashedFiatAccountList = mutableListOf()

                prefs.isLoggedIn = false
            } else {
                prefs.isLoggedIn = true
            }
            onSuccess()
        })
    }

    override fun onBackPressed() {
        if (spinnerNav.viewState == SearchableSpinner.ViewState.ShowingEditLayout) {
            spinnerNav.hideEdit()
        } else if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStack()
            val prevFragmentTag = supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 2).name
            currentFragment = supportFragmentManager.findFragmentByTag(prevFragmentTag) as RefreshFragment
            if (currentFragment !is LoginFragment) {
                setDrawerMenu()
            }
        } else {
            finishAffinity()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.chart_menu, menu)
        for (i in 0..(menu.size() - 1)) {
            val item = menu.getItem(i)
            val spanString = SpannableString(item.title.toString())
            spanString.setSpan(ForegroundColorSpan(Color.BLACK),0, spanString.length, 0)
            item.title = spanString
        }
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

    fun updatePrices(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        var tickersUpdated = 0
        for (product in Product.map.values) {
            product.defaultTradingPair?.let { tradingPair ->
                AnyApi(apiInitData).ticker(tradingPair, onFailure) {
                    tickersUpdated++
                    if (tickersUpdated == Product.map.size) {
                        onComplete()
                    }
                }
            } ?: run {
                onFailure(AnyApi.defaultFailure)
            }
        }
    }

    fun loopThroughAlerts() {
        val prefs = Prefs(this)
        val alerts = prefs.alerts
        for (alert in alerts) {
            if (!alert.hasTriggered) {
                val currentPrice = Product.forCurrency(alert.currency)?.defaultPrice
                if (alert.triggerIfAbove && (currentPrice != null) && (currentPrice >= alert.price)) {
                    AlertHub.triggerPriceAlert(alert, this)
                } else if (!alert.triggerIfAbove && (currentPrice != null) && (currentPrice <= alert.price)) {
                    AlertHub.triggerPriceAlert(alert, this)
                }
            }
        }
        (currentFragment as? AlertsFragment)?.updatePagerAdapter()
    }

    private fun returnToLogin() {
        try {
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        } catch (e: Exception) { }
        goToFragment(FragmentType.LOGIN)
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val fragmentType = when (item.itemId) {
            R.id.nav_accounts -> FragmentType.ACCOUNTS
            R.id.nav_alerts -> FragmentType.ALERTS
            R.id.nav_transfer -> FragmentType.TRANSFER
            R.id.nav_settings -> FragmentType.SETTINGS
            R.id.nav_home -> FragmentType.HOME
            R.id.nav_balances -> FragmentType.BALANCES
            else -> FragmentType.HOME
        }
        val currentFragmentType = FragmentType.forFragment(currentFragment)

        if (fragmentType != currentFragmentType) {
            goToFragment(fragmentType)
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun goToChartFragment(currency: Currency) {
        ChartFragment.currency = currency
        goToFragment(FragmentType.CHART)
    }

    fun goToFragment(fragmentType: FragmentType) {
        val prefs = Prefs(this)
        val fragment : RefreshFragment? = when (fragmentType) {
            FragmentType.CHART -> ChartFragment()
            FragmentType.BALANCES -> BalancesFragment()
            FragmentType.ACCOUNTS -> AccountsFragment.newInstance()
            FragmentType.ALERTS -> AlertsFragment.newInstance()
            FragmentType.TRANSFER -> {
                if (!prefs.isLoggedIn) {
                    toast(R.string.toast_please_login_message)
                    null
                } else if (CBProApi.credentials?.isVerified == null) {
                    goToVerify { if (it) { goToFragment(fragmentType) } }
                    null
                } else if (CBProApi.credentials?.isVerified == false) {
                    toast(R.string.toast_missing_permissions_message)
                    null
                } else if (!TransferFragment.hasRelevantData) {
                    showProgressBar()
                    val depositFragment = TransferFragment.newInstance()
                    depositFragment.refresh {didSucceed ->
                        if (didSucceed) {
                            val tag = fragmentType.toString()
                            depositFragment.skipNextRefresh = true
                            goToFragment(depositFragment, tag)
                        } else {
                            toast(R.string.error_message)
                        }
                    }
                    null
                } else {
                    TransferFragment.newInstance()
                }
            }
            FragmentType.SETTINGS -> SettingsFragment.newInstance()
            FragmentType.HOME -> HomeFragment.newInstance()
            FragmentType.TRADE -> {
                null
            }
            FragmentType.LOGIN -> {
                hideDrawerMenu()
                LoginFragment.newInstance()
            }
            FragmentType.EULA -> EulaFragment()
            FragmentType.OTHER -> null
        }
        if (fragment != null) {
            val tag = fragmentType.toString()
            goToFragment(fragment, tag)
        } else if (fragmentType != FragmentType.TRADE
                && fragmentType != FragmentType.TRANSFER) {
            println("Error switching fragments")
        }
    }

    fun goToFragment(fragment: RefreshFragment, tag: String) {
        currentFragment = fragment
        if (supportFragmentManager.backStackEntryCount == 0) {
            //If there are no fragments on the stack, add it
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container, fragment, tag)
                    .addToBackStack(tag)
                    .commitAllowingStateLoss()
        } else {
            //Replace the current fragment
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment, tag)
                    .addToBackStack(tag)
                    .commitAllowingStateLoss()
        }
    }


    fun hideSoftKeyboard() {
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val currentFocus = currentFocus
        if (currentFocus != null && inputManager != null) {
            inputManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }
}
