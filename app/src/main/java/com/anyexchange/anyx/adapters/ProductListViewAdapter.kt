package com.anyexchange.anyx.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.list_row_product.view.*
import org.jetbrains.anko.textColor
import android.widget.ImageView
import android.widget.TextView


/**
 * Created by anyexchange on 11/12/2017.
 */

class ProductListViewAdapter(var inflater: LayoutInflater?, var onClick: (Product) -> Unit) : BaseAdapter() {

    override fun getCount(): Int {
        return Account.cryptoAccounts.size
    }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    private val sortedProductList: List<Product>
        get() {
            val sortedAccounts = Account.cryptoAccounts.sortedWith(compareBy({ it.defaultValue }, { it.currency.orderValue })).reversed()
            return sortedAccounts.map { a -> a.product }
        }

    internal class ViewHolder {
        var productNameText: TextView? = null
        var percentChangeText: TextView? = null
        var priceText: TextView? = null
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
            //viewHolder.balanceText =  vi.txt_product_amount_owned
            viewHolder.productIcon = vi.img_product_icon
            viewHolder.lineChart = vi.chart_product

            vi.tag = viewHolder
            outputView = vi
        } else {
            viewHolder = convertView.tag as ViewHolder
            outputView = convertView
        }

        if (i >= sortedProductList.size) {
            return outputView
        }
        val product = sortedProductList[i]

        //TODO: someday add ability to select values here
        viewHolder.productIcon?.setImageResource(product.currency.iconId)

        viewHolder.productNameText?.text = product.currency.toString()

        val percentChange = product.percentChange(Timespan.DAY, Account.defaultFiatCurrency)
        viewHolder.percentChangeText?.text = percentChange.percentFormat()
        viewHolder.percentChangeText?.textColor = if (percentChange >= 0) {
            Color.GREEN
        } else {
            Color.RED
        }

        viewHolder.priceText?.text = product.defaultPrice.fiatFormat(Account.defaultFiatCurrency)

        val dayCandleOutliers = product.defaultDayCandles.filter { it.tradingPair.id != product.id }
        if (dayCandleOutliers.isEmpty()) {
            println("yay")
        } else {
            println("oh no")
        }
        viewHolder.lineChart?.configure(product.defaultDayCandles, product.currency, false, PriceChart.DefaultDragDirection.Vertical) {}

        outputView.setOnClickListener { onClick(product) }

        return outputView
    }
}