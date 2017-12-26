package com.jmnbehar.gdax.Fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import com.jmnbehar.gdax.Adapters.AccountListViewAdapter
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_accounts.view.*

/**
 * Created by jmnbehar on 11/5/2017.
 */
class AccountsFragment : Fragment() {
    lateinit var listView: ListView
    lateinit var totalValueTextView: TextView
    lateinit var inflater: LayoutInflater

    companion object {
        var products = listOf<Product>()
        fun newInstance(products: List<Product>): AccountsFragment {
            this.products = products
            return AccountsFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_accounts, container, false)

        listView = rootView.list_accounts
        totalValueTextView = rootView.txt_accounts_total_value
        this.inflater = inflater

        val selectGroup = lambda@ { account: Account ->

        }


        rootView.list_accounts.adapter = AccountListViewAdapter(inflater, selectGroup )

        val updateList = lambda@ {
            refreshData()
        }

        AccountList.getAccountInfo(updateList)

        return rootView
    }

    fun refreshData() {
        (listView.adapter as AccountListViewAdapter).notifyDataSetChanged()
    }


}
