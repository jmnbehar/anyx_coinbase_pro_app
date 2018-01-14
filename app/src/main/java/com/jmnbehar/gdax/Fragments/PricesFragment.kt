package com.jmnbehar.gdax.Fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.jmnbehar.gdax.Activities.MainActivity
import com.jmnbehar.gdax.Adapters.ProductListViewAdapter
import com.jmnbehar.gdax.Classes.Product
import com.jmnbehar.gdax.Classes.RefreshFragment
import com.jmnbehar.gdax.Classes.setHeightBasedOnChildren
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_home.view.*
import org.jetbrains.anko.support.v4.toast

/**
 * Created by jmnbehar on 11/5/2017.
 */
class PricesFragment : RefreshFragment() {
    var currentProduct: Product? = null
    lateinit var listView: ListView
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

        val selectGroup = lambda@ { product: Product ->
            currentProduct = product
        }

        listView.adapter = ProductListViewAdapter(inflater, selectGroup )
        listView.setHeightBasedOnChildren()

        return rootView
    }

    override fun refresh(onComplete: () -> Unit) {
        (activity as MainActivity).updatePrices()
        onComplete()
    }

}
