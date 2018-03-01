package com.jmnbehar.anyx.Adapters

import android.os.Bundle
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import com.jmnbehar.anyx.Classes.RefreshFragment
import com.jmnbehar.anyx.Fragments.Login.LoginHelpFragment
import com.jmnbehar.anyx.Fragments.Main.AccountsFragment
import com.jmnbehar.anyx.Fragments.Main.MarketFragment


/**
 * Created by josephbehar on 2/17/18.
 */

// Since this is an object collection, use a FragmentStatePagerAdapter,
// and NOT a FragmentPagerAdapter.
class LoginHelpPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {


    override fun getItem(i: Int): Fragment {
        return when (i) {
            0 ->    LoginHelpFragment().newInstance(true)
            else -> LoginHelpFragment().newInstance(false)
        }
    }

    override fun getCount(): Int {
        return 2
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> "Mobile"
            1 -> "Desktop"
            else -> "Screen " + (position + 1)
        }
    }
}