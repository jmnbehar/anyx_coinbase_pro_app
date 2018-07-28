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
import kotlinx.android.synthetic.main.list_row_history.view.*
import org.jetbrains.anko.backgroundColor

/**
 * Created by anyexchange on 11/12/2017.
 */

class HistoryListViewAdapter(val context: Context, private var isOrderList: Boolean, ordersOrFills: List<Any>, var resources: Resources, private var orderOnClick: (ApiOrder) -> Unit = { }, private var fillOnClick: (ApiFill) -> Unit = { }) : BaseAdapter() {
    var orders: List<ApiOrder> = listOf()
    var fills: List<ApiFill> = listOf()

    init {
        @Suppress("UNCHECKED_CAST")
        if (isOrderList) {
            orders = ordersOrFills as List<ApiOrder>
        } else {
            fills = ordersOrFills as List<ApiFill>
        }
    }

    override fun getCount(): Int {
        return if (isOrderList) {
            if (orders.isEmpty()) {
                1
            } else {
                orders.size
            }
        } else {
            if (fills.isEmpty()) {
                1
            } else {
                fills.size
            }
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

            val vi = viewGroup.inflate(R.layout.list_row_history)

            viewHolder.colorView = vi.img_history_icon
            viewHolder.sideText = vi.txt_history_side
            viewHolder.amountText = vi.txt_history_amount
            viewHolder.priceText = vi.txt_history_price
            viewHolder.currencyText = vi.txt_history_currency
            viewHolder.tradeTypeText = vi.txt_history_trade_type

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
        if (isOrderList) {
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
            tradeSide = TradeSide.fromString(order.side)
            price = order.price.toDoubleOrZero()
            val size = (order.size ?: order.specified_funds).toDoubleOrZero()
            val filled = order.filled_size.toDoubleOrZero()
            val unfilledSize = size - filled
            amount = unfilledSize
            currency = Currency.forString(order.product_id) ?: Currency.USD
            tradeType = TradeType.fromString(order.type)
            outputView.setOnClickListener { orderOnClick(order) }


            viewHolder.sideText?.text = when (tradeSide) {
                TradeSide.BUY -> context.resources.getString(R.string.chart_history_order_side_buy)
                TradeSide.SELL -> context.resources.getString(R.string.chart_history_order_side_sell)
            }
        } else {
            if (fills.isEmpty()) {
                viewHolder.colorView?.visibility = View.INVISIBLE
                viewHolder.sideText?.visibility = View.GONE
                viewHolder.amountText?.text = context.resources.getString(R.string.chart_history_no_fills)
                viewHolder.priceText?.visibility = View.GONE
                viewHolder.currencyText?.visibility = View.GONE
                viewHolder.tradeTypeText?.visibility = View.GONE
                return outputView
            }
            val fill = fills[i]
            tradeSide = TradeSide.fromString(fill.side)
            currency = Currency.forString(fill.product_id) ?: Currency.USD
            price = fill.price.toDoubleOrZero()
            amount = fill.size.toDoubleOrZero()
            tradeType = null
            outputView.setOnClickListener { fillOnClick(fill) }

            viewHolder.sideText?.text = when (tradeSide) {
                TradeSide.BUY -> context.resources.getString(R.string.chart_history_fill_side_buy)
                TradeSide.SELL -> context.resources.getString(R.string.chart_history_fill_side_sell)
            }
        }

        viewHolder.colorView?.backgroundColor = when (tradeSide) {
            TradeSide.BUY -> ResourcesCompat.getColor(resources, R.color.anyx_green, null)
            TradeSide.SELL -> ResourcesCompat.getColor(resources, R.color.anyx_red, null)
        }
//        vi.img_history_icon.setImageResource(currency.iconId)

        viewHolder.amountText?.text = amount.btcFormat()
        viewHolder.currencyText?.text = context.resources.getString(R.string.chart_history_currency_label, currency)
        viewHolder.priceText?.text = price.fiatFormat(Account.fiatCurrency)
        if (tradeType == null) {
            viewHolder.tradeTypeText?.visibility = View.GONE
        } else {
            viewHolder.tradeTypeText?.visibility = View.VISIBLE
            viewHolder.tradeTypeText?.text = context.resources.getString(R.string.chart_history_trade_type_label, tradeType)
        }

        return outputView
    }
}