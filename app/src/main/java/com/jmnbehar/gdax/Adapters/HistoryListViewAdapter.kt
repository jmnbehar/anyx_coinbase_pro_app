package com.jmnbehar.gdax.Adapters

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.list_row_history.view.*

/**
 * Created by jmnbehar on 11/12/2017.
 */

class HistoryListViewAdapter(ordersOrFills: List<Any>, private var orderOnClick: (ApiOrder) -> Unit = { }, private var fillOnClick: (ApiFill) -> Unit = { }) : BaseAdapter() {
    var isOrderList: Boolean? = null
    var orders: List<ApiOrder> = listOf()
    var fills: List<ApiFill> = listOf()

    init {
        val firstOrderOrFill = ordersOrFills.firstOrNull()
        when (firstOrderOrFill) {
            is ApiOrder -> {
                isOrderList = true
                orders = ordersOrFills as List<ApiOrder>
            }
            is ApiFill -> {
                isOrderList = false
                fills = ordersOrFills as List<ApiFill>
            }
            else -> isOrderList = null
        }
    }

    override fun getCount(): Int {
        return when (isOrderList) {
            null -> 0
            true -> orders.size
            false -> fills.size
        }
    }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        val tradeSide: TradeSide
        val price: Double
        val amount: Double
        val tradeType: TradeType?
        val currency: Currency

        val vi = viewGroup.inflate(R.layout.list_row_history)
        val sideTextView = vi.txt_history_side
        val amountTextView = vi.txt_history_amount
        val priceTextView = vi.txt_history_price
        val currencyTextView = vi.txt_history_currency
        val tradeTypeTextView = vi.txt_history_trade_type


        if (isOrderList == true) {
            val order = orders[i]
            tradeSide = TradeSide.fromString(order.side)
            price = order.price.toDoubleOrZero()
            val size = (order.size ?: order.specified_funds ?: "0.0").toDoubleOrZero()
            val filled = order.filled_size.toDoubleOrZero()
            val unfilledSize = size - filled
            amount = unfilledSize
            currency = Currency.fromString(order.product_id)
            tradeType = TradeType.fromString(order.type)
            vi.setOnClickListener { orderOnClick(order) }


            sideTextView.text = when (tradeSide) {
                TradeSide.BUY -> "Buying "
                TradeSide.SELL -> "Selling "
            }
        } else {
            val fill = fills[i]
            tradeSide = TradeSide.fromString(fill.side)
            currency = Currency.fromString(fill.product_id)
            price = fill.price.toDoubleOrZero()
            amount = fill.size.toDoubleOrZero()
            tradeType = null
            vi.setOnClickListener { fillOnClick(fill) }


            sideTextView.text = when (tradeSide) {
                TradeSide.BUY -> "Bought "
                TradeSide.SELL -> "Sold "
            }
        }
        amountTextView.text = amount.toString()
        currencyTextView.text = " $currency for "
        priceTextView.text = price.fiatFormat()
        tradeTypeTextView.text = "(${tradeType.toString()})"

        return vi

//        if (isOrderList == true) {
//            val order = orders[i]
//            val vi = viewGroup.inflate(R.layout.list_row_history)
//            vi.txt_fill_fee.text = "order item"
//
//            val size = (order.size ?: order.specified_funds ?: "0.0").toDoubleOrZero()
//            val filled = order.filled_size.toDoubleOrZero()
//            val unfilledSize = size - filled
//            vi.txt_fill_size.text = unfilledSize.btcFormat()
//
//            val price = order.price.toDouble()
//            vi.txt_fill_price.text = price.fiatFormat()
//            vi.txt_fill_fee.text = order.fill_fees
//            vi.txt_fill_time.text = order.created_at
//
//            val subtype = TradeType.fromString(order.type)
//            vi.txt_fill_type.text = when (subtype) {
//                TradeType.MARKET -> subtype.toString()
//                TradeType.LIMIT -> subtype.toString()
//                TradeType.STOP ->"$subtype: ${order.price}"
//            }
//
//            val textColor = if (order.side == TradeSide.BUY.toString()) {
//                Color.GREEN
//            } else {
//                Color.RED
//            }
//            vi.txt_fill_size.setTextColor(textColor)
//            vi.txt_fill_price.setTextColor(textColor)
//            vi.txt_fill_fee.setTextColor(textColor)
//            vi.txt_fill_time.setTextColor(textColor)
//
//            vi.setOnClickListener { orderOnClick(order) }
//            return vi
//        } else {
//            val fill = fills[i]
//            val vi = viewGroup.inflate(R.layout.list_row_history)
//            vi.txt_fill_size.text = fill.size
//
//            val price = fill.price.toDouble()
//            vi.txt_fill_price.text = price.fiatFormat()
//            vi.txt_fill_fee.text = fill.fee
//            vi.txt_fill_time.text = fill.created_at
//            vi.txt_fill_type.text = ""
//
//            val textColor = if (fill.side == TradeSide.BUY.toString()) {
//                Color.GREEN
//            } else {
//                Color.RED
//            }
//
//            vi.txt_fill_size.setTextColor(textColor)
//            vi.txt_fill_price.setTextColor(textColor)
//            vi.txt_fill_fee.setTextColor(textColor)
//            vi.txt_fill_time.setTextColor(textColor)
//
//            vi.setOnClickListener { fillOnClick(fill) }
//            return vi
//        }

    }
}