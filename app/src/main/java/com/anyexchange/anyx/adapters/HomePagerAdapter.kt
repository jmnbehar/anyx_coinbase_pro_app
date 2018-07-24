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

    override fun getItem(i: Int): Fragment {
        when (i) {
            0 -> {
                val fragment = MarketFragment()
                val args = Bundle()
                args.putInt(RefreshFragment.ARG_OBJECT, i + 1)
                fragment.arguments = args
                return fragment
            }
            1 -> {
                val fragment = AccountsFragment()
                val args = Bundle()
                args.putInt(RefreshFragment.ARG_OBJECT, i + 1)
                fragment.arguments = args
                return fragment
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
        return 2
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            //TODO: consider using string resources
            0 -> context.resources.getString(R.string.home_market_tab)
            1 -> context.resources.getString(R.string.home_account_tab)
            else -> context.resources.getString(R.string.home_other_tab, position + 1)
        }
    }
}