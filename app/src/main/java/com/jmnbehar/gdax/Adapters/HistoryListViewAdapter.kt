package com.jmnbehar.gdax.Adapters

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.list_header.view.*
import kotlinx.android.synthetic.main.list_row_fill.view.*

/**
 * Created by jmnbehar on 11/12/2017.
 */

class FillHistoryListViewAdapter(var fills: List<ApiFill>, var onClick: (ApiFill) -> Unit) : BaseAdapter() {

    override fun getCount(): Int {
        return fills.size
    }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        val fill = fills[i]
        val vi = viewGroup.inflate(R.layout.list_row_fill)
        vi.txt_fill_size.text = fill.size

        val price = fill.price.toDouble()
        vi.txt_fill_price.text = price.fiatFormat()
        vi.txt_fill_fee.text = fill.fee
        vi.txt_fill_time.text = fill.created_at
        vi.txt_fill_type.text = ""

        val textColor = if (fill.side == TradeSide.BUY.toString()) {
            Color.GREEN
        } else {
            Color.RED
        }
        vi.txt_fill_size.setTextColor(textColor)
        vi.txt_fill_price.setTextColor(textColor)
        vi.txt_fill_fee.setTextColor(textColor)
        vi.txt_fill_time.setTextColor(textColor)

        vi.setOnClickListener { onClick(fill) }
        return vi
    }
}


class OrderHistoryListViewAdapter(var orders: List<ApiOrder>, var orderOnClick: (ApiOrder) -> Unit) : BaseAdapter() {

    override fun getCount(): Int {
        return orders.size
    }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        val index = i - 1
        val order = orders[index]
        val vi = viewGroup.inflate(R.layout.list_row_fill)
        vi.txt_fill_fee.text = "order item"

        val size = (order.size ?: order.specified_funds ?: "0.0").toDoubleOrZero()
        val filled = order.filled_size.toDoubleOrZero()
        val unfilledSize = size - filled
        vi.txt_fill_size.text = unfilledSize.btcFormat()

        val price = order.price.toDouble()
        vi.txt_fill_price.text = price.fiatFormat()
        vi.txt_fill_fee.text = order.fill_fees
        vi.txt_fill_time.text = order.created_at

        val subtype = TradeType.fromString(order.type)
        vi.txt_fill_type.text = when (subtype) {
            TradeType.MARKET -> subtype.toString()
            TradeType.LIMIT -> subtype.toString()
            TradeType.STOP ->"$subtype: ${order.price}"
        }

        val textColor = if (order.side == TradeSide.BUY.toString()) {
            Color.GREEN
        } else {
            Color.RED
        }
        vi.txt_fill_size.setTextColor(textColor)
        vi.txt_fill_price.setTextColor(textColor)
        vi.txt_fill_fee.setTextColor(textColor)
        vi.txt_fill_time.setTextColor(textColor)

        vi.setOnClickListener { orderOnClick(order) }
        return vi
    }
}