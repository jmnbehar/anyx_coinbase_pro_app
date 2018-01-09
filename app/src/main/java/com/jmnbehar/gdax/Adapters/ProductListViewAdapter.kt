package com.jmnbehar.gdax.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.jmnbehar.gdax.Classes.Account
import com.jmnbehar.gdax.Classes.Product
import com.jmnbehar.gdax.R


import kotlinx.android.synthetic.main.list_row_product.view.*

/**
 * Created by jmnbehar on 11/12/2017.
 */

class ProductListViewAdapter(var inflater: LayoutInflater?, var onClick: (Product) -> Unit) : BaseAdapter() {

    override fun getCount(): Int {
        return Account.list.size
    }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        var vi = inflater!!.inflate(R.layout.list_row_product, null)

        val account = Account.list[i]

        val candles = account.product.candles
        val now = candles.first().close
        val open = candles.last().open
        val change = now - open
        val weightedChange: Double = (change / open)

        val percentChange: Double = weightedChange * 100.0
        vi.txt_product_name.text = account.product.currency.toString()
        vi.txt_product_percent_change.text = "%.2f".format(percentChange) + "%"
        vi.txt_product_price.text = "$now"

        vi.txt_product_amount_owned.text = "${account.balance}"
        vi.txt_product_account_value.text = "${account.value}"

        vi.setOnClickListener { onClick(account.product) }

        return vi
    }
}