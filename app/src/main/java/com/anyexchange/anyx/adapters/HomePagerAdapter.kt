package com.anyexchange.anyx.adapters

import android.content.Context
import android.os.Bundle
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import com.anyexchange.anyx.R
import com.anyexchange.anyx.classes.RefreshFragment
import com.anyexchange.anyx.fragments.main.AccountsFragment
import com.anyexchange.anyx.fragments.main.FavoritesFragment
import com.anyexchange.anyx.fragments.main.MarketFragment
import android.view.ViewGroup


/**
 * Created by josephbehar on 2/17/18.
 */

// Since this is an object collection, use a FragmentStatePagerAdapter,
// and NOT a FragmentPagerAdapter.
class HomePagerAdapter(val context: Context, fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
    private var marketFragment : MarketFragment? = null
    var favoritesFragment : FavoritesFragment? = null
    private var accountsFragment : AccountsFragment? = null

    init {
        MarketFragment.resetHomeListeners = { setListeners() }
        AccountsFragment.resetHomeListeners = { setListeners() }
    }

    fun setListeners() {
        marketFragment?.setFavoritesListener(object : MarketFragment.FavoritesUpdateListener {
            override fun favoritesUpdated() {
                favoritesFragment?.completeRefresh()
            }
        })
        marketFragment?.setRefreshListener(object : MarketFragment.RefreshCompleteListener {
            override fun refreshComplete() {
                favoritesFragment?.refresh(false) {  }
                accountsFragment?.refresh(false) {  }
            }
        })
        favoritesFragment?.setRefreshListener(object : MarketFragment.RefreshCompleteListener {
            override fun refreshComplete() {
                marketFragment?.refresh(false) {  }
                accountsFragment?.refresh(false) {  }
            }
        })
        accountsFragment?.setRefreshListener(object : MarketFragment.RefreshCompleteListener {
            override fun refreshComplete() {
                favoritesFragment?.refresh(false) {  }
                marketFragment?.refresh(false) {  }
            }
        })
    }

    override fun getItem(i: Int): Fragment {
        val args = Bundle()
        args.putInt(RefreshFragment.ARG_OBJECT, i + 1)
        when (i) {
            0 -> {
                return MarketFragment()
            }
            1 -> {
                return FavoritesFragment()
            }
            2 -> {
                return AccountsFragment()
            }
            else -> {
                //do something here
                val fragment = MarketFragment()
                fragment.arguments = args
                return fragment
            }
        }
    }

    // Save new Fragment, created from either getItem() or FragmentManger.
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val createdFragment = super.instantiateItem(container, position) as Fragment
        // save the appropriate reference depending on position
        when (position) {
            0 -> marketFragment = createdFragment as MarketFragment
            1 -> favoritesFragment = createdFragment as FavoritesFragment
            2 -> accountsFragment = createdFragment as AccountsFragment
        }
        setListeners()
        return createdFragment
    }

    override fun getCount(): Int {
        return 3
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> context.resources.getString(R.string.home_market_tab)
            1 -> context.resources.getString(R.string.home_favorites_tab)
            2 -> context.resources.getString(R.string.home_account_tab)
            else -> context.resources.getString(R.string.home_other_tab, position + 1)
        }
    }
}