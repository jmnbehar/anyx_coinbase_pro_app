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

class HistoryListViewAdapter(var inflater: LayoutInflater, val isLoggedIn: Boolean, var orders: List<ApiOrder>, var fills: List<ApiFill>, var orderOnClick: (ApiOrder) -> Unit, var fillOnClick: (ApiFill) -> Unit) : BaseAdapter() {

    override fun getCount(): Int {
        return orders.size + fills.size + offset
    }

    private var offset : Int = 1
        get() = if (orders.isNotEmpty() && fills.isNotEmpty()) { 2 } else { 1 }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        if ((i == 0) && (orders.isNotEmpty() || fills.isNotEmpty())) {
            val vi = viewGroup.inflate(R.layout.list_header)
            if (orders.isNotEmpty()) {
                vi.txt_header.text = "ORDERS"
            } else {
                vi.txt_header.text = "FILLS"
            }
            return vi
        } else if ((i <= orders.size) && orders.isNotEmpty()) {
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
        } else if ((i == (orders.size + 1)) && orders.isNotEmpty() && fills.isNotEmpty()) {
            val vi = viewGroup.inflate(R.layout.list_header)
            vi.txt_header.text = "FILLS"

            return vi
        } else if (fills.isNotEmpty()) {
            val index = i - (orders.size + offset)

            val fill = fills[index]
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

            vi.setOnClickListener { fillOnClick(fill) }
            return vi
        } else {
            val vi = viewGroup.inflate(R.layout.list_header)

            vi.txt_header.text = if (isLoggedIn) {
                "You have no orders or fills"
            } else {
                "Log in to see account history"
            }
            return vi
        }

    }
}