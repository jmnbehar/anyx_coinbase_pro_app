package com.jmnbehar.gdax.Fragments

import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import com.jmnbehar.gdax.Classes.Account
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_chart.view.*

/**
 * Created by jmnbehar on 11/5/2017.
 */
class ChartFragment : Fragment() {
    private lateinit var listView: ListView
    private lateinit var inflater: LayoutInflater

    companion object {
        lateinit var account: Account
        fun newInstance(account: Account): ChartFragment {
            this.account = account
            return ChartFragment()
        }
        fun newInstance(): ChartFragment {
            return ChartFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_chart, container, false)

//        rootView.setBackgroundColor(Color.YELLOW)
        this.inflater = inflater

//        listView = rootView.list_history
        rootView.txt_chart_name.text = account.currency
        rootView.txt_chart_ticker.text = account.currency
        rootView.txt_chart_account_balance.text = "${account.balance}"
        rootView.txt_chart_account_value.text = "${account.value}"

        return rootView
    }
}
