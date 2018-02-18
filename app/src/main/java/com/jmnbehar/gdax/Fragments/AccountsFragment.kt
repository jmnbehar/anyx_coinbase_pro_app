package com.jmnbehar.gdax.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.jmnbehar.gdax.Activities.MainActivity
import com.jmnbehar.gdax.Adapters.AccountListViewAdapter
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_accounts.view.*
import org.jetbrains.anko.support.v4.toast

/**
 * Created by jmnbehar on 11/5/2017.
 */
class AccountsFragment : RefreshFragment() {
    lateinit var listView: ListView
    lateinit var inflater: LayoutInflater

    companion object {
        fun newInstance(): AccountsFragment {
            return AccountsFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_accounts, container, false)

        listView = rootView.list_accounts

        this.inflater = inflater

        val selectGroup = lambda@ { account: Account ->
            MainActivity.goToChartFragment(account.currency)
        }

        if (GdaxApi.credentials != null) {
            Account.updateAllAccounts({ toast("error!")}) {
                rootView.list_accounts.adapter = AccountListViewAdapter(inflater, selectGroup )
            }
        } else {
            rootView.list_accounts.visibility = View.GONE
        }

        return rootView
    }

}
