package com.jmnbehar.anyx.Adapters

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.list_row_history.view.*
import org.jetbrains.anko.backgroundColor

/**
 * Created by jmnbehar on 11/12/2017.
 */

class HistoryListViewAdapter(private var isOrderList: Boolean, ordersOrFills: List<Any>, private var orderOnClick: (ApiOrder) -> Unit = { }, private var fillOnClick: (ApiFill) -> Unit = { }) : BaseAdapter() {
    var orders: List<ApiOrder> = listOf()
    var fills: List<ApiFill> = listOf()

    init {
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
        val imageView = vi.img_history_icon


        if (isOrderList == true) {
            if (orders.isEmpty()) {
                imageView.visibility = View.INVISIBLE
                sideTextView.visibility = View.GONE
                amountTextView.text = "You have no open orders"
                priceTextView.visibility = View.GONE
                currencyTextView.visibility = View.GONE
                tradeTypeTextView.visibility = View.GONE
                return vi
            }
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
            if (fills.isEmpty()) {
                imageView.visibility = View.INVISIBLE
                sideTextView.visibility = View.GONE
                amountTextView.text = "You have no open orders"
                priceTextView.visibility = View.GONE
                currencyTextView.visibility = View.GONE
                tradeTypeTextView.visibility = View.GONE
                return vi
            }
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

        imageView.backgroundColor = when (tradeSide) {  //TODO: change to Ellie Approved Colors
            TradeSide.BUY -> Color.GREEN
            TradeSide.SELL -> Color.RED
        }
//        vi.img_history_icon.setImageResource(currency.iconId)

        amountTextView.text = amount.btcFormat()
        currencyTextView.text = " $currency for "
        priceTextView.text = price.fiatFormat()
        if (tradeType == null) {
            tradeTypeTextView.visibility = View.GONE
        } else {
            tradeTypeTextView.visibility = View.VISIBLE
            tradeTypeTextView.text = "($tradeType order)"
        }

        return vi

    }
}