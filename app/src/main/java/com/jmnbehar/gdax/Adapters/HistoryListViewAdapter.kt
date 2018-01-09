package com.jmnbehar.gdax.Adapters

import android.graphics.Color
import android.view.LayoutInflater
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

class HistoryListViewAdapter(var inflater: LayoutInflater?, var orders: List<ApiOrder>, var fills: List<ApiFill>, var onClick: (Account) -> Unit) : BaseAdapter() {

    override fun getCount(): Int {
        var offset = 1
        if (orders.isNotEmpty()) { offset++ }
        else if (fills.isNotEmpty()) { offset++ }

        return orders.size + fills.size + offset
    }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {

        if ((i == 0) && (orders.isNotEmpty() || fills.isNotEmpty())) {
            var vi = inflater!!.inflate(R.layout.list_header, null)
            if (orders.isNotEmpty()) {
                vi.txt_header.text = "ORDERS"
            } else {
                vi.txt_header.text = "FILLS"
            }
            return vi
        } else if ((i <= orders.size) && orders.isNotEmpty()) {
            val index = i - 1
            val order = orders[index]
            var vi = inflater!!.inflate(R.layout.list_row_fill, null)
            vi.txt_fill_fee.text = "order item"

            val size = (order.size ?: order.specified_funds ?: "0.0").toDoubleOrZero()
            val filled = order.filled_size.toDoubleOrZero()
            val unfilledSize = size - filled
            vi.txt_fill_size.text = "%.8f".format(unfilledSize)
            vi.txt_fill_price.text = order.price
            vi.txt_fill_fee.text = order.fill_fees
            vi.txt_fill_time.text = order.created_at

            val subtype = TradeSubType.fromString(order.type)
            vi.txt_fill_type.text = when (subtype) {
                TradeSubType.MARKET -> subtype.toString()
                TradeSubType.LIMIT -> subtype.toString()
                TradeSubType.STOP ->"$subtype: ${order.price}"
            }

            var textColor = if (order.side == TradeType.BUY.toString()) {
                Color.GREEN
            } else {
                Color.RED
            }
            vi.txt_fill_size.setTextColor(textColor)
            vi.txt_fill_price.setTextColor(textColor)
            vi.txt_fill_fee.setTextColor(textColor)
            vi.txt_fill_time.setTextColor(textColor)

            return vi
        } else if ((i == (orders.size + 1)) && orders.isNotEmpty() && fills.isNotEmpty()) {
            var vi = inflater!!.inflate(R.layout.list_header, null)
            vi.txt_header.text = "FILLS"

            return vi
        } else if (fills.isNotEmpty()) {
            val offset = if (orders.isEmpty()) 1 else  2
            val index = i - orders.size -  offset

            val fill = fills[index]
            var vi = inflater!!.inflate(R.layout.list_row_fill, null)
            vi.txt_fill_size.text = fill.size
            vi.txt_fill_price.text = fill.price
            vi.txt_fill_fee.text = fill.fee
            vi.txt_fill_time.text = fill.created_at
            vi.txt_fill_type.text = ""

            var textColor = if (fill.side == TradeType.BUY.toString()) {
                Color.GREEN
            } else {
                Color.RED
            }
            vi.txt_fill_size.setTextColor(textColor)
            vi.txt_fill_price.setTextColor(textColor)
            vi.txt_fill_fee.setTextColor(textColor)
            vi.txt_fill_time.setTextColor(textColor)

            return vi
        } else {
            var vi = inflater!!.inflate(R.layout.list_header, null)
            vi.txt_header.text = "You have no orders or fills"
            return vi
        }

    }
}