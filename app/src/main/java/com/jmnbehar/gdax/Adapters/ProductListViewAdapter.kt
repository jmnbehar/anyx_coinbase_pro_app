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
import android.widget.ImageView
import android.widget.TextView


/**
 * Created by jmnbehar on 11/12/2017.
 */

class ProductListViewAdapter(var inflater: LayoutInflater?, val isLoggedIn: Boolean, var onClick: (Product) -> Unit) : BaseAdapter() {

    override fun getCount(): Int {
        return Account.list.size
    }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    internal class ViewHolder {
        var productNameText: TextView? = null
        var percentChangeText: TextView? = null
        var priceText: TextView? = null
        var balanceText: TextView? = null
        var productIcon: ImageView? = null
        var lineChart:  PriceChart? = null
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        val viewHolder: ViewHolder?
        val outputView: View
        if (convertView == null) {
            viewHolder = ViewHolder()

            val vi = viewGroup.inflate(R.layout.list_row_product)

            viewHolder.productNameText = vi.txt_product_name
            viewHolder.percentChangeText = vi.txt_product_percent_change
            viewHolder.priceText = vi.txt_product_price
            viewHolder.balanceText =  vi.txt_product_amount_owned
            viewHolder.productIcon = vi.img_product_icon
            viewHolder.lineChart = vi.chart_product

            vi.tag = viewHolder
            outputView = vi
        } else {
            viewHolder = convertView.tag as ViewHolder
            outputView = convertView
        }

        val account = when (i) {
            0 -> Account.btcAccount
            1 -> Account.ethAccount
            2 -> Account.ltcAccount
            3 -> Account.bchAccount
            else -> Account.list[i]
        }
        if (account == null) {
            return outputView
        }

        val candles = account.product.dayCandles
        val currentPrice = account.product.price
        val open = if (candles.isNotEmpty()) {
            candles.first().open
        } else {
            0.0
        }
        val change = currentPrice - open
        val weightedChange: Double = (change / open)

        val percentChange: Double = weightedChange * 100.0


        //TODO: someday add ability to select values here
        viewHolder.productIcon?.setImageResource(account.currency.iconId)

        viewHolder.productNameText?.text = account.product.currency.toString()

        viewHolder.percentChangeText?.text = "${percentChange.fiatFormat()}%"
        viewHolder.percentChangeText?.textColor = if (percentChange >= 0) {
            Color.GREEN
        } else {
            Color.RED
        }

        viewHolder.priceText?.text = currentPrice.fiatFormat()

        if (isLoggedIn) {
            viewHolder.balanceText?.text = "Balance: ${account.balance} ${account.currency}"
        } else {
            viewHolder.balanceText?.visibility = View.GONE
        }

        viewHolder.lineChart?.configure(candles, account.currency, false, PriceChart.DefaultDragDirection.Vertical,  TimeInSeconds.oneDay, false) {}

        outputView.setOnClickListener { onClick(account.product) }

        return outputView
    }
}