package com.jmnbehar.gdax.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import com.jmnbehar.gdax.Activities.MainActivity
import com.jmnbehar.gdax.Adapters.ProductListViewAdapter
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_home.view.*

/**
 * Created by jmnbehar on 11/5/2017.
 */
class PricesFragment : RefreshFragment() {
    private var currentProduct: Product? = null
    lateinit var listView: ListView

    lateinit var totalValueText: TextView
    lateinit var totalValueLabelText: TextView

    lateinit var inflater: LayoutInflater

    companion object {
        fun newInstance(): PricesFragment
        {
            return PricesFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_home, container, false)

        listView = rootView.list_products
        this.inflater = inflater
        totalValueText = rootView.txt_home_total_value
        totalValueLabelText = rootView.txt_home_total_value_label

        val selectGroup = lambda@ { product: Product ->
            currentProduct = product
            var equivalentMenuItem = when(product.currency) {
                Currency.BTC -> R.id.nav_btc
                Currency.ETH -> R.id.nav_eth
                Currency.LTC -> R.id.nav_ltc
                Currency.BCH -> R.id.nav_btc
                Currency.USD -> R.id.nav_btc
            }
            MainActivity.goToNavigationId(equivalentMenuItem, activity)
        }

        totalValueText.text = "$${Account.totalBalance.fiatFormat()}"
        totalValueLabelText.text = "Account total value"

        listView.adapter = ProductListViewAdapter(inflater, selectGroup )
        listView.setHeightBasedOnChildren()

        return rootView
    }

    override fun refresh(onComplete: () -> Unit) {
        (activity as MainActivity).getCandles {
            (activity as MainActivity).updatePrices {
                (listView.adapter as ProductListViewAdapter).notifyDataSetChanged()
                onComplete()
            }
        }
    }

}
