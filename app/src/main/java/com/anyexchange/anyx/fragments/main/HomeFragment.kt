package com.anyexchange.anyx.fragments.main

import android.graphics.Color
import android.os.Bundle
import android.view.*
import com.anyexchange.anyx.adapters.HomePagerAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.activities.MainActivity
import com.anyexchange.anyx.views.LockableViewPager
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_home.view.*

/**
 * Created by anyexchange on 11/5/2017.
 */
class HomeFragment : RefreshFragment() {

    lateinit var inflater: LayoutInflater
    private var homePagerAdapter: HomePagerAdapter? = null

    companion object {
        fun newInstance(): HomeFragment
        {
            return HomeFragment()
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

            homePagerAdapter = HomePagerAdapter(it, childFragmentManager)

            viewPager!!.adapter = homePagerAdapter


            viewPager?.setCurrentItem(1)
        }

        setHasOptionsMenu(true)

        return rootView
    }

    override fun refresh(onComplete: (Boolean) -> Unit) { }

    override fun onResume() {
        val currencyList = Product.map.keys.map { Currency(it) }
        showNavSpinner(null, currencyList) { selectedCurrency ->
            (activity as MainActivity).goToChartFragment(selectedCurrency)
        }

        super.onResume()

        homePagerAdapter?.setListeners()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val shouldShowOptions = lifecycle.isCreatedOrResumed
        menu.setGroupVisible(R.id.group_chart_style, false)
        menu.setGroupVisible(R.id.group_home_sort, shouldShowOptions)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        val context = context
        val shouldSortAlphabetical = if (context == null) {
            false
        } else {
            Prefs(context).sortFavoritesAlphabetical
        }
        if (shouldSortAlphabetical) {
            menu?.getItem(2)?.isChecked = true
        } else {
            menu?.getItem(3)?.isChecked = true
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        context?.let {
            val prefs = Prefs(it)
            prefs.sortFavoritesAlphabetical = (item.itemId == R.id.home_sort_alphabetical)
        }
        item.isChecked = true
        homePagerAdapter?.favoritesFragment?.completeRefresh()
        return super.onOptionsItemSelected(item)
    }
}
