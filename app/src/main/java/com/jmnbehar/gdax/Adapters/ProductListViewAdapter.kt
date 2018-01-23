package com.jmnbehar.gdax.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.list_row_product.view.*
import org.jetbrains.anko.textColor

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

        val account = when (i) {
            0 -> Account.btcAccount
            1 -> Account.ethAccount
            2 -> Account.ltcAccount
            3 -> Account.bchAccount
            else -> Account.list[i]
        } ?: null
        account ?: return vi

        val candles = account.product.candles
        val currentPrice = account.product.price
        val open = candles.first().open
        val change = currentPrice - open
        val weightedChange: Double = (change / open)

        val percentChange: Double = weightedChange * 100.0

        var productNameText = vi.txt_product_name
        var percentChangeText = vi.txt_product_percent_change
        var priceText = vi.txt_product_price
        var balanceText =  vi.txt_product_amount_owned


        vi.img_product_icon.setImageResource(account.currency.iconId)

        productNameText.text = account.product.currency.toString()

        percentChangeText.text = percentChange.fiatFormat() + "%"
        percentChangeText.textColor = if (percentChange >= 0) {
            Color.GREEN
        } else {
            Color.RED
        }

        priceText.text = currentPrice.fiatFormat()

        balanceText.text = "Balance: ${account.balance} ${account.currency}"

        var lineChart = vi.chart_product
        lineChart.configure(candles, account.currency, false,  TimeInSeconds.oneDay, false)

        vi.setOnClickListener { onClick(account.product) }

        return vi
    }
}