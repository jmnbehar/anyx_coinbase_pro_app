package com.jmnbehar.gdax.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.jmnbehar.gdax.Classes.ApiProduct
import com.jmnbehar.gdax.Classes.Product
import com.jmnbehar.gdax.R


import kotlinx.android.synthetic.main.list_row_product.view.*

/**
 * Created by jmnbehar on 11/12/2017.
 */

class ProductListViewAdapter(var inflater: LayoutInflater?, var products: List<Product>, var onClick: (Product) -> Unit) : BaseAdapter() {

    override fun getCount(): Int {
        return products.size
    }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        var vi = inflater!!.inflate(R.layout.list_row_product, null)

        vi.txt_product_name.text = products[i].currency
        vi.setOnClickListener { onClick(products[i]) }

        return vi
    }
}