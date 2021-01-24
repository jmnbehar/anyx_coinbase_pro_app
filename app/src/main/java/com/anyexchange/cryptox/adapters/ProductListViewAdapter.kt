package com.anyexchange.cryptox.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.anyexchange.cryptox.classes.*
import com.anyexchange.cryptox.R
import kotlinx.android.synthetic.main.list_row_product.view.*
import org.jetbrains.anko.textColor
import android.widget.ImageView
import android.widget.TextView


/**
 * Created by anyexchange on 11/12/2017.
 */

class ProductListViewAdapter(var inflater: LayoutInflater?, var productList: List<Product>, var isFavorites: Boolean) : BaseAdapter() {
    var size = 20

    init {
        if (productList.size < size) {
            size = productList.size
        }
    }
    companion object {
        const val sizeChangeAmount = 5
    }

    override fun getCount(): Int {
        return if (isFavorites) {
            productList.size
        } else {
            size
        }
    }

    fun increaseSize() {
        if (size < productList.size) {
            if ((size + sizeChangeAmount) <= productList.size) {
                size += sizeChangeAmount
            } else {
                size = productList.size
            }
            notifyDataSetChanged()
        }
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

        if (i >= productList.size) {
            return outputView
        }
        val product = productList[i]

        //TODO: someday add ability to select values here
        product.currency.iconId?.let {
            viewHolder.productIcon?.visibility = View.VISIBLE
            viewHolder.productIcon?.setImageResource(it)
        } ?: run {
            viewHolder.productIcon?.visibility = View.INVISIBLE
        }

        viewHolder.productNameText?.text = product.currency.toString()

        val timespan = Timespan.DAY

        val granularity = Candle.granularityForTimespan(timespan)

            // TODO: scale down prices for default currency if needed
        val quoteCurrency = product.defaultTradingPair?.quoteCurrency ?: Account.defaultFiatCurrency

        if (isFavorites) {
            val tradingPair = product.defaultTradingPair ?: TradingPair(Exchange.CBPro, product.currency, Currency.USD)
            viewHolder.lineChart?.configure(product.defaultDayCandles, granularity, timespan, tradingPair, false, DefaultDragDirection.Vertical) {}
            viewHolder.lineChart?.visibility = View.VISIBLE
            viewHolder.priceText?.visibility = View.VISIBLE
            viewHolder.percentChangeText?.visibility = View.VISIBLE

            val percentChange = product.percentChange(timespan, quoteCurrency)
            viewHolder.percentChangeText?.text = percentChange.percentFormat()
            viewHolder.percentChangeText?.textColor = if (percentChange > 0) {
                Color.GREEN
            } else if (percentChange == 0.0) {
                Color.WHITE
            } else {
                Color.RED
            }
        } else {
            viewHolder.lineChart?.visibility = View.GONE
            viewHolder.priceText?.visibility = View.VISIBLE
            viewHolder.percentChangeText?.visibility = View.GONE
        }
        viewHolder.priceText?.text = product.priceForQuoteCurrency(quoteCurrency).format(quoteCurrency)

        return outputView
    }
}