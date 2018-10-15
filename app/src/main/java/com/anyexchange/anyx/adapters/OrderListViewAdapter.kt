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
        var sideText: TextView? = null
        var amountText: TextView? = null
        var currencyText: TextView? = null
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
            viewHolder.sideText = vi.txt_order_side
            viewHolder.amountText = vi.txt_order_amount
            viewHolder.priceText = vi.txt_order_price
            viewHolder.currencyText = vi.txt_order_currency
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
            viewHolder.sideText?.visibility = View.GONE
            viewHolder.amountText?.text = context.resources.getString(R.string.chart_history_no_orders)
            viewHolder.priceText?.visibility = View.GONE
            viewHolder.currencyText?.visibility = View.GONE
            viewHolder.tradeTypeText?.visibility = View.GONE
            return outputView
        }
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


        viewHolder.sideText?.text = when (tradeSide) {
            TradeSide.BUY -> context.resources.getString(R.string.chart_history_order_side_buy)
            TradeSide.SELL -> context.resources.getString(R.string.chart_history_order_side_sell)
        }

        viewHolder.colorView?.backgroundColor = when (tradeSide) {
            TradeSide.BUY -> ResourcesCompat.getColor(resources, R.color.anyx_green, null)
            TradeSide.SELL -> ResourcesCompat.getColor(resources, R.color.anyx_red, null)
        }
//        vi.img_history_icon.setImageResource(currency.iconId)

        viewHolder.amountText?.text = amount.btcFormat()
        viewHolder.currencyText?.text = context.resources.getString(R.string.chart_history_currency_label, currency)
        viewHolder.priceText?.text = price.fiatFormat(Account.defaultFiatCurrency)

        if (tradeType == null) {
            viewHolder.tradeTypeText?.visibility = View.GONE
        } else {
            viewHolder.tradeTypeText?.visibility = View.VISIBLE
            viewHolder.tradeTypeText?.text = context.resources.getString(R.string.chart_history_trade_type_label, tradeType)
        }

        return outputView
    }
}