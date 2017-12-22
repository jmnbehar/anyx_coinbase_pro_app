package com.jmnbehar.gdax.Fragments

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
    lateinit var listView: ListView
    lateinit var totalValueTextView: TextView
    lateinit var inflater: LayoutInflater

    companion object {
        lateinit var account: Account
        fun newInstance(account: Account): Fragment {
            this.account = account
            return ChartFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_chart, container, false)

        listView = rootView.list_history
        rootView.txt_chart_name.text = account.currency
        rootView.txt_chart_ticker.text = account.currency
        rootView.txt_chart_account_balance.text = "${account.balance}"
        rootView.txt_chart_account_value.text = "${account.value}"


        this.inflater = inflater

        return inflater?.inflate(R.layout.fragment_chart, container, false)
    }
}
