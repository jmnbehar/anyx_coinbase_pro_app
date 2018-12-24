package com.anyexchange.anyx.fragments.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.anyexchange.anyx.adapters.HomePagerAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.adapters.ProductListViewAdapter
import com.anyexchange.anyx.classes.api.AnyApi
import com.anyexchange.anyx.classes.api.CBProApi
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import kotlinx.android.synthetic.main.fragment_home.view.*

/**
 * Created by anyexchange on 11/5/2017.
 */
class HomeFragment : RefreshFragment() {

    lateinit var inflater: LayoutInflater
    private var homePagerAdapter: HomePagerAdapter? = null

    companion object {
        fun newInstance(): HomeFragment
        {
            return HomeFragment()
        }
        var viewPager: LockableViewPager? = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)

        val tabLayout = rootView.home_tab_layout

        viewPager = rootView.home_view_pager

        context?.let {
            if (Prefs(it).isDarkModeOn) {
                tabLayout.setTabTextColors(Color.LTGRAY, Color.WHITE)
            } else {
                tabLayout.setTabTextColors(Color.DKGRAY, Color.BLACK)
            }

            homePagerAdapter = HomePagerAdapter(it, childFragmentManager)
            viewPager?.adapter = homePagerAdapter

            homePagerAdapter?.marketFragment?.updateAccountsFragment = { homePagerAdapter?.accountsFragment?.refreshComplete() }
        }



        return rootView
    }

    override fun refresh(onComplete: (Boolean) -> Unit) {
        skipNextRefresh = true

        val onFailure: (result: Result.Failure<String, FuelError>) -> Unit = { result ->
            toast("Error!: ${result.errorMessage}")
            onComplete(false)
        }

        ///TODO: run all 3 sub-refreshes, preferably run the active page's first
        /// after completing the first page run the other 2, only hide the spinner once all 3 are completed

        super.refresh(onComplete)

    }

    fun marketRefresh(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (Boolean) -> Unit) {
        AnyApi(apiInitData).updateAllTickers(onFailure) {
            //Complete Market Refresh
        }
    }

    fun accountsRefresh(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (Boolean) -> Unit) {
        val context = context
        if (context != null && Prefs(context).isLoggedIn) {
            CBProApi.accounts(apiInitData).updateAllAccounts(onFailure) {
                //Complete accounts refresh
            }
        } else {
            onComplete(true)
        }
    }
    fun favoritesRefresh(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (Boolean) -> Unit) {
        val onFailure: (result: Result.Failure<String, FuelError>) -> Unit = { result -> toast("Error!: ${result.errorMessage}") }
        var productsUpdated = 0
        val time = Timespan.DAY
        val favoriteProducts = Product.favorites()
        val count = favoriteProducts.count()
        for (product in favoriteProducts) {
            //always check multiple exchanges?
            product.defaultTradingPair?.let { tradingPair ->
                product.updateCandles(time, tradingPair, apiInitData, {
                    //OnFailure
                }) { didUpdate ->
                    //OnSuccess
                    if (lifecycle.isCreatedOrResumed) {
                        if (didUpdate) {
                            productsUpdated++
                            if (productsUpdated == count) {
                                context?.let {
                                    Prefs(it).stashedProducts = Product.map.values.toList()
                                }
                                //update Favorites Tab
                                onComplete(true)
                            }
                        } else {
                            AnyApi(apiInitData).ticker(tradingPair, onFailure) {
                                productsUpdated++
                                if (productsUpdated == count) {
                                    //update Favorites Tab
                                    onComplete(true)
                                }
                            }
                        }
                    }
                }
            } ?: run {
                onFailure(Result.Failure(FuelError(Exception())))
            }
        }
    }


    override fun onPause() {
        handler.removeCallbacks(autoRefresh)
        super.onPause()
    }
}
