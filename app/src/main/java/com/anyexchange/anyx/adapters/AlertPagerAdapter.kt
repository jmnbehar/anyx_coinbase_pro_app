package com.anyexchange.anyx.adapters

import android.content.Context
import android.os.Bundle
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import com.anyexchange.anyx.classes.Currency
import com.anyexchange.anyx.classes.RefreshFragment
import com.anyexchange.anyx.fragments.main.AlertListFragment


/**
 * Created by josephbehar on 2/17/18.
 */

// Since this is an object collection, use a FragmentStatePagerAdapter,
// and NOT a FragmentPagerAdapter.
class AlertPagerAdapter(val context: Context, fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
    private val currencies = Currency.cryptoList
    override fun getItem(i: Int): Fragment {
        val fragment = AlertListFragment.newInstance(currencies[i])
        val args = Bundle()
        args.putInt(RefreshFragment.ARG_OBJECT, i + 1)
        fragment.arguments = args
        return fragment
    }

    override fun getCount(): Int {
        return currencies.size
    }

    override fun getItemPosition(`object`: Any): Int {
        val f = `object` as AlertListFragment
        f.refresh {  }
        return super.getItemPosition(`object`)
    }
    override fun getPageTitle(position: Int): CharSequence {
        return currencies[position].toString()
    }
}