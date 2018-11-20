package com.anyexchange.anyx.adapters

import android.content.Context
import android.os.Bundle
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import com.anyexchange.anyx.R
import com.anyexchange.anyx.classes.RefreshFragment
import com.anyexchange.anyx.fragments.main.AccountsFragment
import com.anyexchange.anyx.fragments.main.MarketFragment


/**
 * Created by josephbehar on 2/17/18.
 */

// Since this is an object collection, use a FragmentStatePagerAdapter,
// and NOT a FragmentPagerAdapter.
class HomePagerAdapter(val context: Context, fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

    val marketFragment = MarketFragment.newInstance(false)
    val favoritesFragment = MarketFragment.newInstance(true)
    val accountsFragment = AccountsFragment()

    override fun getItem(i: Int): Fragment {
        when (i) {
            0 -> {
                val args = Bundle()
                args.putInt(RefreshFragment.ARG_OBJECT, i + 1)
                marketFragment.arguments = args
                return marketFragment
            }
            1 -> {
                val args = Bundle()
                args.putInt(RefreshFragment.ARG_OBJECT, i + 1)
                favoritesFragment.arguments = args
                return favoritesFragment
            }
            2 -> {
                val args = Bundle()
                args.putInt(RefreshFragment.ARG_OBJECT, i + 1)
                accountsFragment.arguments = args
                return accountsFragment
            }
            else -> {
                //do something here
                val fragment = MarketFragment()
                val args = Bundle()
                args.putInt(RefreshFragment.ARG_OBJECT, i + 1)
                fragment.arguments = args
                return fragment
            }
        }
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