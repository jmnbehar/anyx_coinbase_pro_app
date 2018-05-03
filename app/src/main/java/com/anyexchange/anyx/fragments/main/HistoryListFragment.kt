package com.anyexchange.anyx.fragments.main

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.anyexchange.anyx.adapters.HistoryListViewAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.list_view.view.*

/**
 * Created by anyexchange on 11/5/2017.
 */
class HistoryListFragment() : Fragment() {
    lateinit var historyList: ListView
    lateinit var inflater: LayoutInflater

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
        val rootView = inflater.inflate(R.layout.list_view, container, false)
        historyList = rootView.list_view
        if (isOrderList) {
            historyList.adapter = HistoryListViewAdapter(true, orders, resources, orderOnClick =  { order -> onOrderClick(order)})
        } else {
            historyList.adapter = HistoryListViewAdapter(false, fills, resources, fillOnClick = { fill -> onFillClick(fill) })
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
