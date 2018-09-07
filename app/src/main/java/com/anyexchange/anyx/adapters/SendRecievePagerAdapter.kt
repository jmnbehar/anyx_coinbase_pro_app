package com.anyexchange.anyx.adapters

import android.content.Context
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import com.anyexchange.anyx.R
import com.anyexchange.anyx.fragments.main.ReceiveFragment
import com.anyexchange.anyx.fragments.main.SendFragment


/**
 * Created by josephbehar on 2/17/18.
 */

// Since this is an object collection, use a FragmentStatePagerAdapter,
// and NOT a FragmentPagerAdapter.
class SendRecievePagerAdapter(val context: Context, fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

    private val sendFragment = SendFragment()
    private val receiveFragment = ReceiveFragment()

    override fun getItem(i: Int): Fragment {
        return when (i) {
            0 -> sendFragment
            1 -> receiveFragment
            else -> sendFragment
        }
    }

    override fun getCount(): Int {
        return 2
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> context.resources.getString(R.string.send_title)
            1 -> context.resources.getString(R.string.receive_title)
            else -> context.resources.getString(R.string.send_title)
        }
    }

    fun switchCurrency() {
        sendFragment.switchCurrency()
        receiveFragment.switchCurrency(false)
    }
}