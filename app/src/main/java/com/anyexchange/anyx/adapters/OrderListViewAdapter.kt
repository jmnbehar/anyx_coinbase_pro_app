package com.anyexchange.anyx.adapters

import android.content.Context
import android.content.res.Resources
import android.support.v4.content.res.ResourcesCompat
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.list_row_order.view.*
import org.jetbrains.anko.backgroundColor

/**
 * Created by anyexchange on 11/12/2017.
 */

class OrderListViewAdapter(val context: Context, val orders: List<Order>, var resources: Resources, private var orderOnClick: (Order) -> Unit = { }) : BaseAdapter() {

    override fun getCount(): Int {
        return if (orders.isEmpty()) {
            1
        } else {
            orders.size
        }
    }
    
    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }


    internal class ViewHolder {
        var colorView: ImageView? = null
        var mainLabelText: TextView? = null

        var priceText: TextView? = null
        var tradeTypeText: TextView? = null
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        val viewHolder: ViewHolder?
        val outputView: View
        if (convertView == null) {
            viewHolder = ViewHolder()

            val vi = viewGroup.inflate(R.layout.list_row_order)

            viewHolder.colorView = vi.img_order_icon
            viewHolder.mainLabelText = vi.txt_order_label

            viewHolder.priceText = vi.txt_order_price
            viewHolder.tradeTypeText = vi.txt_order_trade_type

            vi.tag = viewHolder
            outputView = vi
        } else {
            viewHolder = convertView.tag as ViewHolder
            outputView = convertView
        }

        val tradeSide: TradeSide
        val price: Double
        val amount: Double
        val tradeType: TradeType?
        val currency: Currency

        if (orders.isEmpty()) {
            viewHolder.colorView?.visibility = View.INVISIBLE
            viewHolder.mainLabelText?.text = context.resources.getString(R.string.chart_history_no_orders)
            viewHolder.priceText?.visibility = View.GONE
            viewHolder.tradeTypeText?.visibility = View.GONE
            return outputView
        } else {
            val order = orders[i]
            tradeSide = order.side
            price = order.price

            val size = order.amount
            val filled = order.filledAmount
            val unfilledSize = size - filled
            amount = unfilledSize
            currency = order.tradingPair.baseCurrency
            tradeType = TradeType.forString(order.type)
            outputView.setOnClickListener { orderOnClick(order) }


            viewHolder.colorView?.backgroundColor = when (tradeSide) {
                TradeSide.BUY -> ResourcesCompat.getColor(resources, R.color.anyx_green, null)
                TradeSide.SELL -> ResourcesCompat.getColor(resources, R.color.anyx_red, null)
            }


            val sideString = when (tradeSide) {
                TradeSide.BUY -> context.resources.getString(R.string.chart_history_order_side_buy)
                TradeSide.SELL -> context.resources.getString(R.string.chart_history_order_side_sell)
            }

            val totalPrice = order.funds?.toDoubleOrNull() ?: order.price * order.amount
            val quoteCurrency = order.tradingPair.quoteCurrency

            viewHolder.mainLabelText?.text = context.resources.getString(R.string.chart_fill_main_label, sideString, amount.format(currency), totalPrice.format(quoteCurrency))

            viewHolder.priceText?.text = price.fiatFormat(Account.defaultFiatCurrency)

            val tradeTypeString = order.type.capitalize()
            viewHolder.priceText?.text = context.resources.getString(R.string.chart_order_price_label, tradeTypeString, price.format(quoteCurrency), currency)


            viewHolder.tradeTypeText?.visibility = View.VISIBLE
            viewHolder.tradeTypeText?.text = context.resources.getString(R.string.chart_history_trade_type_label, tradeType)

            return outputView
        }
    }
}