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
        return Product.map.size
    }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    private val sortedProductList: List<Product>
        get() {
            val productList = Product.map.map { it.value }.toList()
            return productList.sortedWith(compareBy({ it.totalDefaultValueOfRelevantAccounts() }, { it.currency.orderValue })).reversed()
        }

    internal class ViewHolder {
        var productNameText: TextView? = null
        var percentChangeText: TextView? = null
        var priceText: TextView? = null
        var productIcon: ImageView? = null
        var lineChart:  PriceLineChart? = null
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

        val timespan = Timespan.DAY
        val percentChange = product.percentChange(timespan, Account.defaultFiatCurrency)
        viewHolder.percentChangeText?.text = percentChange.percentFormat()
        viewHolder.percentChangeText?.textColor = if (percentChange >= 0) {
            Color.GREEN
        } else {
            Color.RED
        }

        val granularity = Candle.granularityForTimespan(timespan)
        viewHolder.priceText?.text = product.defaultPrice.fiatFormat(Account.defaultFiatCurrency)

        if (product.isFavorite && product.defaultDayCandles.isNotEmpty()) {
            viewHolder.lineChart?.configure(product.defaultDayCandles, granularity, product.currency, false, DefaultDragDirection.Vertical) {}
            viewHolder.lineChart?.visibility = View.VISIBLE
        } else {
            viewHolder.lineChart?.visibility = View.GONE
        }
        outputView.setOnLongClickListener {
            product.isFavorite = !product.isFavorite
            notifyDataSetChanged()
            true
        }
        outputView.setOnClickListener { onClick(product) }

        return outputView
    }
}