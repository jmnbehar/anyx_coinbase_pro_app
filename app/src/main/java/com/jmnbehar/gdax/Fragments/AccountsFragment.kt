package com.jmnbehar.gdax.Fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
    var accounts = mutableListOf<Account>()

    companion object {
        lateinit var products: List<Product>
        fun newInstance(products: List<Product>): Fragment {
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


        rootView.list_accounts.adapter = AccountListViewAdapter(inflater, accounts, selectGroup )

        val updateList = lambda@ {
            (listView.adapter as AccountListViewAdapter).notifyDataSetChanged()
            var totalValue = 0.0
            for (account in accounts) {
                totalValue += account.value
            }
            totalValueTextView.text = "Total Value: $totalValue"
        }

        getAccountInfo(updateList)

        return rootView
    }


    fun getAccountInfo(updateList: () -> Unit) {
        Fuel.request(GdaxApi.accounts()).responseString { request, _, result ->
            //do something with response
            println("url: " + request.url)
            when (result) {
                is Result.Failure -> {
                    //error
                    println("Error!: ${result.error}")
                }
                is Result.Success -> {
                    val gson = Gson()

                    val apiAccountList: List<ApiAccount> = gson.fromJson(result.value, object : TypeToken<List<ApiAccount>>() {}.type)
                    for (apiAccount in apiAccountList) {
                        val relevantProduct = products.filter { p -> p.currency == apiAccount.currency }.firstOrNull()
                        if (relevantProduct != null) {
                            accounts.add(Account(relevantProduct, apiAccount))
                        }
                    }
                    updateList()
                }
            }
        }
    }

}
