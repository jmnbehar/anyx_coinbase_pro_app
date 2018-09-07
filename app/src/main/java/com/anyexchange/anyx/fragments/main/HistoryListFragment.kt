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

    private var orders = listOf<ApiOrder>()
    private var onOrderClick: (ApiOrder) -> Unit = { }
    private var fills = listOf<ApiFill>()
    private var onFillClick: (ApiFill) -> Unit = { }
    private var isOrderList = true

    companion object {
        fun newOrderInstance(orders: List<ApiOrder>, onClick: (ApiOrder) -> Unit) : HistoryListFragment {
            val newFragment = HistoryListFragment()
            newFragment.orders = orders
            newFragment.onOrderClick = onClick
            newFragment.isOrderList = true
            return newFragment
        }
        fun newFillInstance(fills: List<ApiFill>, onClick: (ApiFill) -> Unit) : HistoryListFragment {
            val newFragment = HistoryListFragment()
            newFragment.fills = fills
            newFragment.onFillClick = onClick
            newFragment.isOrderList = false
            return newFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.recycler_view, container, false)
        viewManager = LinearLayoutManager(context)
        context?.let {
            historyListAdapter = if (isOrderList) {
                HistoryListViewAdapter(it, true, orders, resources, orderOnClick = { order -> onOrderClick(order) })
            } else {
                HistoryListViewAdapter(it, false, fills, resources, fillOnClick = { fill -> onFillClick(fill) })
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
        }
        return rootView
    }
}
