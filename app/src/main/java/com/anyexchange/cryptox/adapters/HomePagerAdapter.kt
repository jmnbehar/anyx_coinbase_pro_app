package com.anyexchange.cryptox.adapters

import android.content.Context
import android.os.Bundle
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import com.anyexchange.cryptox.R
import com.anyexchange.cryptox.classes.RefreshFragment
import com.anyexchange.cryptox.fragments.main.BalancesFragment
import com.anyexchange.cryptox.fragments.main.FavoritesFragment
import com.anyexchange.cryptox.fragments.main.MarketFragment
import android.view.ViewGroup


/**
 * Created by josephbehar on 2/17/18.
 */

// Since this is an object collection, use a FragmentStatePagerAdapter,
// and NOT a FragmentPagerAdapter.
class HomePagerAdapter(val context: Context, fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
    private var marketFragment : MarketFragment? = null
    var favoritesFragment : FavoritesFragment? = null

    init {
        MarketFragment.resetHomeListeners = { setListeners() }
        BalancesFragment.resetHomeListeners = { setListeners() }
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
            }
        })
        favoritesFragment?.setRefreshListener(object : MarketFragment.RefreshCompleteListener {
            override fun refreshComplete() {
                marketFragment?.refresh(false) {  }
            }
        })
    }

    override fun getItem(i: Int): Fragment {
        val args = Bundle()
        args.putInt(RefreshFragment.ARG_OBJECT, i + 1)
        when (i) {
            0 -> {
                return FavoritesFragment()
            }
            1 -> {
                return MarketFragment()
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
            0 -> favoritesFragment = createdFragment as FavoritesFragment
            1 -> marketFragment = createdFragment as MarketFragment
        }
        setListeners()
        return createdFragment
    }

    override fun getCount(): Int {
        return 2
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> context.resources.getString(R.string.home_favorites_tab)
            1 -> context.resources.getString(R.string.home_market_tab)
            else -> context.resources.getString(R.string.home_other_tab, position + 1)
        }
    }
}