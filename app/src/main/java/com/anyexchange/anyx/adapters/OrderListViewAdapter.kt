package com.anyexchange.anyx.adapters

import android.content.Context
import android.content.res.Resources
import android.support.v4.content.res.ResourcesCompat
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.classes.api.CBProApi
import kotlinx.android.synthetic.main.list_row_order.view.*
import org.jetbrains.anko.backgroundColor

/**
 * Created by anyexchange on 11/12/2017.
 */

class OrderListViewAdapter(val context: Context, val orders: List<Order>, var resources: Resources, private var orderOnClick: (Order) -> Unit, private var cancelButtonClicked: (Order) -> Unit) : BaseAdapter() {

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

        var extraInfoLayout: LinearLayout? = null

        var timeInForceText: TextView? = null
        var dateText: TextView? = null
        var amountText: TextView? = null

        var cancelButton: Button? = null
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

            viewHolder.extraInfoLayout = vi.layout_order_extra_info

            viewHolder.dateText = vi.txt_order_date
            viewHolder.amountText = vi.txt_order_amount
            viewHolder.timeInForceText = vi.txt_order_time_in_force
            viewHolder.cancelButton = vi.btn_order_cancel

            vi.tag = viewHolder
            outputView = vi
        } else {
            viewHolder = convertView.tag as ViewHolder
            outputView = convertView
        }

        if (orders.isEmpty()) {
            viewHolder.colorView?.visibility = View.INVISIBLE
            viewHolder.mainLabelText?.text = context.resources.getString(R.string.chart_history_no_orders)
            viewHolder.priceText?.visibility = View.GONE
            viewHolder.extraInfoLayout?.visibility = View.GONE
            return outputView
        } else {
            val order = orders[i]
//            val tradeSide = order.side
//            val price = order.price

            val size = order.amount
            val filled = order.filledAmount
            val amount = size - filled
            val currency = order.tradingPair.baseCurrency
            outputView.setOnClickListener { orderOnClick(order) }


            viewHolder.colorView?.backgroundColor = when (order.side) {
                TradeSide.BUY -> ResourcesCompat.getColor(resources, R.color.anyx_green, null)
                TradeSide.SELL -> ResourcesCompat.getColor(resources, R.color.anyx_red, null)
            }

            val sideString = when (order.side) {
                TradeSide.BUY -> context.resources.getString(R.string.chart_history_order_side_buy)
                TradeSide.SELL -> context.resources.getString(R.string.chart_history_order_side_sell)
            }

//            val totalPrice = order.funds?.toDoubleOrNull() ?: order.price * order.amount
            val quoteCurrency = order.tradingPair.quoteCurrency

            viewHolder.mainLabelText?.text = order.summary(resources)

            val tradeTypeString = order.type.toString().capitalize()
            viewHolder.priceText?.visibility = View.GONE

            if (order.showExtraInfo) {
                viewHolder.extraInfoLayout?.visibility = View.VISIBLE

                viewHolder.dateText?.text = "Date Created: ${order.time.format(Fill.dateFormat)}"

                if (order.filledAmount > 0) {
                    viewHolder.amountText?.text = "Original Amount: ${order.amount}, Filled Amount: ${order.filledAmount}"
                    viewHolder.amountText?.visibility = View.VISIBLE
                } else {
                    viewHolder.amountText?.visibility = View.GONE
                }

                viewHolder.timeInForceText?.text = when (order.exchange) {
                    Exchange.CBPro -> {
                        val timeInForce = TimeInForce.forString(order.timeInForce)
                        timeInForce?.userFriendlyString(order.expireTime) ?: ""
                    }
                    Exchange.Binance -> ""
                }

                viewHolder.cancelButton?.setOnClickListener {
                    cancelButtonClicked(order)
                }
            } else {
                viewHolder.extraInfoLayout?.visibility = View.GONE
            }

            return outputView
        }
    }
}