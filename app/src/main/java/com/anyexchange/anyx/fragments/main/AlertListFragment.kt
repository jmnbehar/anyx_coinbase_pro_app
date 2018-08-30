package com.anyexchange.anyx.fragments.main

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.PopupMenu
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.activities.MainActivity
import com.anyexchange.anyx.adapters.AlertListViewAdapter
import kotlinx.android.synthetic.main.list_view.view.*

/**
 * Created by anyexchange on 11/5/2017.
 */
class AlertListFragment : RefreshFragment() {
    private var alertList: ListView? = null
    private var alertAdapter: AlertListViewAdapter? = null
    private lateinit var viewManager: RecyclerView.LayoutManager

    var currency: Currency = Currency.OTHER

    private val sortedAlerts : List<Alert>
        get() {
            context?.let { context ->
                val alerts = Prefs(context).alerts.filter { it.currency == currency }
                return alerts.sortedWith(compareBy { it.price })
            } ?: run {
                return listOf()
            }
        }

    companion object {

        fun newInstance(currency: Currency) : AlertListFragment {
            val newFragment = AlertListFragment()
            newFragment.currency = currency
            return newFragment
        }

        var blockRefresh = false
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.list_view, container, false)
        setupSwipeRefresh(rootView.swipe_refresh_layout)

        viewManager = LinearLayoutManager(context)
        alertList = rootView.list_view
        context?.let {
            alertAdapter = AlertListViewAdapter(it, inflater, sortedAlerts) { view, alert ->
                val popup = PopupMenu(activity, view)
                //Inflating the Popup using xml file
                popup.menuInflater.inflate(R.menu.alert_popup_menu, popup.menu)

                popup.setOnMenuItemClickListener { item: MenuItem? ->
                    when (item?.itemId ?: R.id.delete_alert) {
                        R.id.delete_alert -> {
                            deleteAlert(alert)
                        }
                    }
                    true
                }
                popup.show()
            }
            alertList?.adapter = alertAdapter
        }
        //This prevents each of the fragments from refreshing on initial load:
        skipNextRefresh = true
        return rootView
    }

    override fun refresh(onComplete: (Boolean) -> Unit) {
        super.refresh(onComplete)
        alertAdapter?.alerts = sortedAlerts
        alertAdapter?.notifyDataSetChanged()

        if (currency == ChartFragment.currency && !blockRefresh) {
            (activity as? MainActivity)?.let { mainActivity ->
                mainActivity.updatePrices({ onComplete(false) }, {
                    mainActivity.loopThroughAlerts()
                    onComplete(true)
                })
            } ?: run {
                onComplete(true)
            }
        } else {
            onComplete(true)
        }
    }

    private fun deleteAlert(alert: Alert) {
        context?.let {
            Prefs(it).removeAlert(alert)
            alertAdapter?.alerts = sortedAlerts
            alertAdapter?.notifyDataSetChanged()
            alertList?.adapter = alertAdapter
        }
    }
}
