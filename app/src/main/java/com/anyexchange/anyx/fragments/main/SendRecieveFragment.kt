package com.anyexchange.anyx.fragments.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.anyexchange.anyx.adapters.HomePagerAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.adapters.SendRecievePagerAdapter
import kotlinx.android.synthetic.main.fragment_home.view.*

/**
 * Created by anyexchange on 11/5/2017.
 */
class SendRecieveFragment : RefreshFragment() {

    lateinit var inflater: LayoutInflater
    private var sendReceivePagerAdapter: SendRecievePagerAdapter? = null

    companion object {
        fun newInstance(): SendRecieveFragment
        {
            return SendRecieveFragment()
        }
        var viewPager: LockableViewPager? = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)

        val tabLayout = rootView.home_tab_layout

        viewPager = rootView.home_view_pager

        context?.let {
            if (Prefs(it).isDarkModeOn) {
                tabLayout.setTabTextColors(Color.LTGRAY, Color.WHITE)
            } else {
                tabLayout.setTabTextColors(Color.DKGRAY, Color.BLACK)
            }

            sendReceivePagerAdapter = SendRecievePagerAdapter(it, childFragmentManager)
            viewPager?.adapter = sendReceivePagerAdapter
        }


        return rootView
    }


    override fun onResume() {
        super.onResume()
        showNavSpinner(ChartFragment.currency, Currency.cryptoList) { selectedCurrency ->
            ChartFragment.currency = selectedCurrency
            sendReceivePagerAdapter?.switchCurrency()
        }
    }
    override fun onPause() {
        handler.removeCallbacks(autoRefresh)
        super.onPause()
    }
}
