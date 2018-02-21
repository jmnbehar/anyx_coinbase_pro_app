package com.jmnbehar.gdax.Adapters

import android.support.v4.view.PagerAdapter
import android.view.ViewGroup
import android.view.View
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.list_view.view.*


/**
 * Created by josephbehar on 2/17/18.
 */

// Since this is an object collection, use a FragmentStatePagerAdapter,
// and NOT a FragmentPagerAdapter.
class HistoryPagerAdapter(private var isLoggedIn: Boolean, private var orders: List<ApiOrder>, private var fills: List<ApiFill>, private var orderOnClick: (ApiOrder) -> Unit, private var fillOnClick: (ApiFill) -> Unit) : PagerAdapter() {

    override fun instantiateItem(viewGroup: ViewGroup, position: Int): Any {
        val vi = viewGroup.inflate(R.layout.list_view)
        val historyList = vi.list_view
        when (position) {
            0 -> historyList.adapter = HistoryListViewAdapter(isLoggedIn, orders, fills,
                    { order -> orderOnClick(order)}, { fill -> fillOnClick(fill) })
            1 -> historyList.adapter = HistoryListViewAdapter(isLoggedIn, orders, fills,
                    { order -> orderOnClick(order)}, { fill -> fillOnClick(fill) })
            else -> historyList.visibility = View.GONE
        }
        historyList.setHeightBasedOnChildren()
        return vi
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }


    override fun getCount(): Int {
        return 2
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> "Orders"
            1 -> "Fill"
            else -> (position + 1).toString()
        }
    }
}