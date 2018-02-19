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


        if (GdaxApi.credentials != null) {

            val selectGroup = lambda@ { account: Account ->
                MainActivity.goToChartFragment(account.currency)
            }

            val accountTotalCandles = sumAccountCandles()
            val totalValue = Account.list.map { a -> a.value }.sum()
            rootView.txt_accounts_total_value.text = totalValue.fiatFormat()
            val open = if (accountTotalCandles.isNotEmpty()) {
                accountTotalCandles.first().open
            } else {
                0.0
            }
            val change = totalValue - open
            val weightedChange: Double = (change / open)
            val percentChange: Double = weightedChange * 100.0

            rootView.txt_all_accounts_label.text = "All accounts"
            rootView.txt_accounts_percent_change.text = "$percentChange%"

            rootView.chart_accounts.configure(accountTotalCandles, Currency.USD, true, PriceChart.DefaultDragDirection.Horizontal, TimeInSeconds.oneDay,true) {
                swipeRefreshLayout?.isEnabled = false
                LockableScrollView.scrollLocked = true
            }

            Account.updateAllAccounts({ toast("error!")}) {
                rootView.list_accounts.adapter = AccountListViewAdapter(inflater, selectGroup)
                rootView.account_text.visibility = View.GONE
            }
        } else {
            rootView.list_accounts.visibility = View.GONE
            rootView.chart_accounts.visibility = View.GONE
            //TODO: put a login button here
            rootView.account_text.text = "Sign in to view account info"
        }

        return rootView
    }

    fun sumAccountCandles() : List<Candle> {
        val btcAccount = Account.btcAccount?.product
        if (btcAccount != null) {
            var accountTotalCandleList: MutableList<Candle> = mutableListOf()
            for (i in 0..btcAccount.dayCandles.size) {
                var totalCandleValue = 0.0
                val time = btcAccount.dayCandles[i].time
                for (account in Account.list) {
                    val accountCandleValue = if (account.product.dayCandles.size >= (i - 1)) {
                        account.product.dayCandles[i].close
                    } else {
                        1.0
                    }
                    totalCandleValue += (accountCandleValue * account.balance)
                }
                val newCandle = Candle(time, 0.0, 0.0, totalCandleValue, totalCandleValue, 0.0)
                accountTotalCandleList.add(newCandle)
            }
            return accountTotalCandleList
        }
        return listOf()
    }
}
