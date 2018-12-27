package com.anyexchange.anyx.fragments.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.anyexchange.anyx.adapters.HomePagerAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
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
        var refresh: (pageIndex: Int, onComplete: (Boolean) -> Unit) -> Unit = { _, _ ->  }
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

            MarketFragment.updateAccountsFragment = { homePagerAdapter?.accountsFragment?.completeRefresh() }
            MarketFragment.updateFavoritesFragment = { homePagerAdapter?.favoritesFragment?.completeRefresh() }

            Companion.refresh = { pageIndex, onComplete ->  refresh(pageIndex, onComplete) }

            viewPager?.setCurrentItem(1)
        }

        return rootView
    }

    override fun refresh(onComplete: (Boolean) -> Unit) {
        /// after completing the first page run the other 2, only hide the spinner once all 3 are completed
        val currentPage = viewPager?.currentItem ?: 1

        refresh(currentPage, onComplete)
    }

    fun refresh(currentPageIndex: Int, onComplete: (Boolean) -> Unit) {
        val onFailure: (result: Result.Failure<String, FuelError>) -> Unit = { result ->
            toast("Error: ${result.errorMessage}")
            onComplete(false)
        }

        when (currentPageIndex) {
            0 -> {
                marketRefresh(onFailure, {
                    favoritesRefresh({ }, { })
                    accountsRefresh({ }, { })
                    onComplete(it)
                })
            }
            1 -> {
                favoritesRefresh(onFailure, {
                    marketRefresh({ }, { })
                    accountsRefresh({ }, { })
                    onComplete(it)
                })
            }
            2 -> {
                accountsRefresh(onFailure, {
                    marketRefresh({ }, { })
                    favoritesRefresh({ }, { })
                    onComplete(it)
                })
            }
        }
    }


    private fun marketRefresh(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (Boolean) -> Unit) {
        AnyApi(apiInitData).updateAllTickers(onFailure) {
            //Complete Market Refresh
            homePagerAdapter?.marketFragment?.completeRefresh()
            onComplete(true)
        }
    }

    private fun accountsRefresh(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (Boolean) -> Unit) {
        val context = context
        if (context != null && Prefs(context).isLoggedIn) {
            CBProApi.accounts(apiInitData).updateAllAccounts(onFailure) {
                //Complete accounts refresh
                homePagerAdapter?.accountsFragment?.completeRefresh()
                onComplete(true)
            }
        } else {
            onComplete(true)
        }
    }
    private fun favoritesRefresh(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (Boolean) -> Unit) {
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
                                homePagerAdapter?.favoritesFragment?.completeRefresh()
                                onComplete(true)
                            }
                        } else {
                            AnyApi(apiInitData).ticker(tradingPair, onFailure) {
                                productsUpdated++
                                if (productsUpdated == count) {
                                    //update Favorites Tab
                                    homePagerAdapter?.favoritesFragment?.completeRefresh()
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

    override fun onResume() {
        //be smarter about only showing this when necessary, and maybe only refresh when necessary as well
        swipeRefreshLayout?.isRefreshing = true

        super.onResume()

        autoRefresh = Runnable {
            if (!skipNextRefresh) {
                refresh {}
            }
            skipNextRefresh = false
            handler.postDelayed(autoRefresh, TimeInMillis.halfMinute)
        }
        handler.postDelayed(autoRefresh, TimeInMillis.halfMinute)

        refresh { endRefresh() }
    }

    override fun onPause() {
        handler.removeCallbacks(autoRefresh)
        super.onPause()
    }
}
