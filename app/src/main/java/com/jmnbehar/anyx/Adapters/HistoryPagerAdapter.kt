package com.jmnbehar.anyx.Adapters

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.Fragments.Main.HistoryListFragment

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

    override fun getItemPosition(`object`: Any): Int {
        val f = `object` as HistoryListFragment
        (f.historyList.adapter as HistoryListViewAdapter).orders = orders
        (f.historyList.adapter as HistoryListViewAdapter).fills  = fills
        (f.historyList.adapter as HistoryListViewAdapter).notifyDataSetChanged()
        return super.getItemPosition(`object`)
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