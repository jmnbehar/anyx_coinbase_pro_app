package com.jmnbehar.gdax.Activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.Fragments.AccountsFragment
import com.jmnbehar.gdax.Fragments.ChartFragment
import com.jmnbehar.gdax.Fragments.PricesFragment
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    var currentFragment: Fragment? = null

    companion object {
        lateinit var apiProductList: List<ApiProduct>
        var productList = mutableListOf<Product>()
        fun newIntent(context: Context, result: String): Intent {
            val intent = Intent(context, MainActivity::class.java)

            val gson = Gson()

            val unfilteredApiProductList: List<ApiProduct> = gson.fromJson(result, object : TypeToken<List<ApiProduct>>() {}.type)

            apiProductList = unfilteredApiProductList.filter {
                s -> s.quote_currency == "USD" && s.base_currency != "BCH"
            }

            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        if (savedInstanceState == null) {
            getProductInfo()
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
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun getProductInfo() {
        for (product in apiProductList) {
            Fuel.request(GdaxApi.candles(product.id)).responseString { request, _, result ->
                //do something with response
                println("url: " + request.url)
                when (result) {
                    is Result.Failure -> {
                        //error
                        println("Error!: ${result.error}")
                    }
                    is Result.Success -> {
                        addToProductList(product, result.value)
                    }
                }
            }
        }
    }

    fun addToProductList(apiProduct: ApiProduct, candles: String) {
        val gson = Gson()

        val candleLongList: List<List<Double>> = gson.fromJson(candles, object : TypeToken<List<List<Double>>>() {}.type)
        var candleList = mutableListOf<Candle>()
        for (list in candleLongList) {
            candleList.add(Candle(list[0], list[1], list[2], list[3], list[4], list[5]))
        }
        val newProduct = Product(apiProduct,candleList)

        productList.add(newProduct)
        println("pl size: ${productList.size},    apl size: ${apiProductList.size}")
        if (productList.size == apiProductList.size) {
            goToFragment(PricesFragment.newInstance(productList), "chart")
        }
    }



    fun goToFragment(fragment: Fragment, tag: String) {
        currentFragment = fragment
        if (supportFragmentManager.fragments.isEmpty()) {
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container, fragment, tag)
                    .commit()
        } else {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment, tag)
                    .commit()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_btc -> {
                if (AccountList.btcAccount != null) {
                    goToFragment(ChartFragment.newInstance(AccountList.btcAccount!!), "BTC Chart")
                } else {
                    AccountList.getAccountInfo { goToFragment(ChartFragment.newInstance(AccountList.btcAccount!!), "BTC Chart") }
                }
            }
            R.id.nav_eth -> {
                if (AccountList.ethAccount != null) {
                    goToFragment(ChartFragment.newInstance(AccountList.ethAccount!!), "ETH Chart")
                } else {
                    AccountList.getAccountInfo { goToFragment(ChartFragment.newInstance(AccountList.ethAccount!!), "BTC Chart") }
                }
            }
            R.id.nav_ltc -> {
                if (AccountList.ltcAccount != null) {
                    goToFragment(ChartFragment.newInstance(AccountList.ltcAccount!!), "ETH Chart")
                } else {
                    AccountList.getAccountInfo { goToFragment(ChartFragment.newInstance(AccountList.ltcAccount!!), "BTC Chart") }
                }
            }
            R.id.nav_accounts -> {
                goToFragment(AccountsFragment.newInstance(productList), "AccountList")
            }
            R.id.nav_send -> {

            }
            R.id.nav_alerts -> {

            }
            R.id.nav_settings -> {

            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
