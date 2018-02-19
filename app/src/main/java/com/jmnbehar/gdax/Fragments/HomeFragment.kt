package com.jmnbehar.gdax.Fragments

import android.graphics.Color
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jmnbehar.gdax.Adapters.HomePagerAdapter
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_home.view.*

/**
 * Created by jmnbehar on 11/5/2017.
 */
class HomeFragment : RefreshFragment() {

    lateinit var inflater: LayoutInflater
    var collectionPagerAdapter: HomePagerAdapter? = null

    companion object {
        fun newInstance(): HomeFragment
        {
            return HomeFragment()
        }
        var viewPager: LockableViewPager? = null
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_home, container, false)

        val prefs = Prefs(context)
        val tabLayout = rootView.tab_layout

        if (prefs.isDarkModeOn) {
            tabLayout.setTabTextColors(Color.LTGRAY, Color.WHITE)
        } else {
            tabLayout.setTabTextColors(Color.DKGRAY, Color.BLACK)
        }

        viewPager = rootView.view_pager

        collectionPagerAdapter = HomePagerAdapter(childFragmentManager)
        viewPager?.adapter = collectionPagerAdapter

        return rootView
    }

//    override fun onResume() {
//        super.onResume()
//    }

    override fun onPause() {
        handler.removeCallbacks(autoRefresh)
        super.onPause()
    }
}
