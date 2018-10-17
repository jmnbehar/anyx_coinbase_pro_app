package com.anyexchange.anyx.adapters

import android.content.Context
import android.content.res.Resources
import android.support.v4.content.res.ResourcesCompat
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
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
        var idDateText: TextView? = null
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

            viewHolder.idDateText = vi.txt_order_id_and_date
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

            val totalPrice = order.funds?.toDoubleOrNull() ?: order.price * order.amount
            val quoteCurrency = order.tradingPair.quoteCurrency

            viewHolder.mainLabelText?.text = context.resources.getString(R.string.chart_fill_main_label, sideString, amount.format(currency), totalPrice.format(quoteCurrency))

            viewHolder.priceText?.text = order.price.fiatFormat(Account.defaultFiatCurrency)

//            val tradeType = TradeType.forString(order.type)
            val tradeTypeString = order.type.capitalize()
            viewHolder.priceText?.text = context.resources.getString(R.string.chart_order_price_label, tradeTypeString, order.price.format(quoteCurrency), currency)

            if (order.showExtraInfo) {
                viewHolder.extraInfoLayout?.visibility = View.VISIBLE

                //TODO: format this better:
                viewHolder.idDateText?.text = order.id + " " + order.time.format(Fill.dateFormat)
                viewHolder.amountText?.text = "Full Amount: ${order.amount}, Filled Amount: ${order.filledAmount}, remaining: $amount"
                viewHolder.timeInForceText?.text = order.timeInForce.capitalize()

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