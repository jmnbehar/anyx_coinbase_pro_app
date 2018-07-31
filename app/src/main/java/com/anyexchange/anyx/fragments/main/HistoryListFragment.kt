package com.anyexchange.anyx.fragments.main

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.anyexchange.anyx.adapters.HistoryListViewAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.recycler_view.view.*

/**
 * Created by anyexchange on 11/5/2017.
 */
class HistoryListFragment : Fragment() {
    lateinit var historyList: RecyclerView
    private lateinit var historyListAdapter: HistoryListViewAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    var orders = listOf<ApiOrder>()
    var onOrderClick: (ApiOrder) -> Unit = { }
    var fills = listOf<ApiFill>()
    var onFillClick: (ApiFill) -> Unit = { }
    var isOrderList = true
    fun newOrderInstance(orders: List<ApiOrder>, onClick: (ApiOrder) -> Unit) : HistoryListFragment {
        this.orders = orders
        this.onOrderClick = onClick
        isOrderList = true
        return HistoryListFragment()
    }
    fun newFillInstance(fills: List<ApiFill>, onClick: (ApiFill) -> Unit) : HistoryListFragment {
        this.fills = fills
        this.onFillClick = onClick
        isOrderList = false
        return HistoryListFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.recycler_view, container, false)
        viewManager = LinearLayoutManager(context)
        historyListAdapter = if (isOrderList) {
            HistoryListViewAdapter(context!!, true, orders, resources, orderOnClick =  { order -> onOrderClick(order)})
        } else {
            HistoryListViewAdapter(context!!,false, fills, resources, fillOnClick = { fill -> onFillClick(fill) })
        }

        historyList = rootView.recycler_view.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = historyListAdapter

        }



//        val listHeight = historyList.setHeightBasedOnChildren()
//
//        val params = rootView.layoutParams
//        params.height = listHeight
//        rootView.layoutParams = params
//        (parentFragment as ChartFragment).setHistoryPagerHeight(listHeight)
        return rootView
    }
}
