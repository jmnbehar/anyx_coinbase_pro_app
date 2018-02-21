package com.jmnbehar.gdax.Adapters

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.text.BoringLayout
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.Fragments.HistoryListFragment

/**
 * Created by josephbehar on 2/17/18.
 */

class HistoryPagerAdapter(fm: FragmentManager, var orders: List<ApiOrder>, var fills: List<ApiFill>, private var orderOnClick: (ApiOrder) -> Unit, private var fillOnClick: (ApiFill) -> Unit) : FragmentStatePagerAdapter(fm) {

    override fun getItem(i: Int): Fragment {
        when (i) {
            0 -> {
                val fragment = HistoryListFragment()
                fragment.newOrderInstance(orders, orderOnClick)
                val args = Bundle()
                args.putInt(RefreshFragment.ARG_OBJECT, i + 1)
                fragment.arguments = args
                return fragment
            }
            else -> {
                val fragment = HistoryListFragment()
                fragment.newFillInstance(fills, fillOnClick)
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
            0 -> "Orders"
            1 -> "Fills"
            else -> (position + 1).toString()
        }
    }
}