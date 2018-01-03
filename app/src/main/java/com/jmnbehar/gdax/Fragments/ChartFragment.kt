package com.jmnbehar.gdax.Fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jmnbehar.gdax.Activities.MainActivity
import com.jmnbehar.gdax.Adapters.AccountListViewAdapter
import com.jmnbehar.gdax.Adapters.HistoryListViewAdapter
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_accounts.view.*
import kotlinx.android.synthetic.main.fragment_chart.view.*

/**
 * Created by jmnbehar on 11/5/2017.
 */
class ChartFragment : Fragment() {
    private lateinit var inflater: LayoutInflater

    companion object {
        lateinit var account: Account
        fun newInstance(account: Account): ChartFragment {
            this.account = account
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

        val buyButton = rootView.btn_chart_buy
        val sellButton = rootView.btn_chart_sell

        //TODO: send over more info
        buyButton.setOnClickListener {
            MainActivity.goToFragment(TradeFragment.newInstance(account, TradeType.BUY), "Trade: Buy")
        }

        sellButton.setOnClickListener {
            MainActivity.goToFragment(TradeFragment.newInstance(account, TradeType.SELL), "Trade: Sell")
        }


        GdaxApi.listOrders(productId = account.product.id).executeRequest { result ->
            when (result) {
                is Result.Failure -> {
                    //error
                    println("Error!: ${result.error}")
                }
                is Result.Success -> {
                    val gson = Gson()

                    val apiOrderList: List<ApiOrder> = gson.fromJson(result.value, object : TypeToken<List<ApiOrder>>() {}.type)


                    GdaxApi.fills(productId = account.product.id).executeRequest { result ->
                        when (result) {
                            is Result.Failure -> {
                                //error
                                println("Error!: ${result.error}")
                            }
                            is Result.Success -> {
                                val gson = Gson()

                                val apiFillList: List<ApiFill> = gson.fromJson(result.value, object : TypeToken<List<ApiFill>>() {}.type)


                                rootView.list_history.adapter = HistoryListViewAdapter(inflater, apiOrderList, apiFillList, { })

                            }
                        }
                    }
                }
            }
        }


        return rootView
    }
}
