package com.anyexchange.anyx.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.anyexchange.anyx.R
import com.anyexchange.anyx.adapters.ExchangeAccountsListViewAdapter
import com.anyexchange.anyx.classes.*
import kotlinx.android.synthetic.main.list_view.view.*

class AccountsFragment : RefreshFragment() {
    private var exchangeAccountList: ListView? = null
    private var exchangeAccountListAdapter: ExchangeAccountsListViewAdapter? = null

    companion object {

        fun newInstance() : AccountsFragment {
            return AccountsFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.list_view, container, false)
        setupSwipeRefresh(rootView.swipe_refresh_layout)

        exchangeAccountList = rootView.list_view
        context?.let {
            exchangeAccountListAdapter = ExchangeAccountsListViewAdapter(it, Exchange.values(), resources)
            exchangeAccountList?.adapter = exchangeAccountListAdapter
        }
        return rootView
    }

    override fun refresh(onComplete: (Boolean) -> Unit) {
        super.refresh(onComplete)
        exchangeAccountListAdapter?.notifyDataSetChanged()
    }
}
