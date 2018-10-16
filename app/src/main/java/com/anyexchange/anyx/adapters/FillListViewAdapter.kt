package com.anyexchange.anyx.adapters

import android.content.Context
import android.content.res.Resources
import android.support.v4.content.res.ResourcesCompat
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.list_row_fill.view.*
import org.jetbrains.anko.backgroundColor

/**
 * Created by anyexchange on 11/12/2017.
 */

class FillListViewAdapter(val context: Context, private var fills: List<Fill>, var resources: Resources, private var fillOnClick: (Fill) -> Unit = { }) : BaseAdapter() {


    override fun getCount(): Int {
        return if (fills.isEmpty()) {
            1
        } else {
            fills.size
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

        var feeText: TextView? = null

        var dateText: TextView? = null

        var extraInfoLayout: LinearLayout? = null
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        val viewHolder: ViewHolder?
        val outputView: View
        if (convertView == null) {
            viewHolder = ViewHolder()

            val vi = viewGroup.inflate(R.layout.list_row_fill)

            viewHolder.colorView = vi.img_fill_icon
            viewHolder.mainLabelText = vi.txt_fill_label

            viewHolder.priceText = vi.txt_fill_price
            viewHolder.feeText = vi.txt_fill_fees
            viewHolder.dateText = vi.txt_fill_date

            viewHolder.extraInfoLayout = vi.layout_fill_extra_info

            vi.tag = viewHolder
            outputView = vi
        } else {
            viewHolder = convertView.tag as ViewHolder
            outputView = convertView
        }

        val tradeSide: TradeSide
        val price: Double
        val amount: Double

        val currency: Currency

        if (fills.isEmpty()) {
            viewHolder.colorView?.visibility = View.INVISIBLE
            viewHolder.mainLabelText?.visibility = View.VISIBLE
            viewHolder.priceText?.visibility = View.GONE
            viewHolder.feeText?.visibility = View.GONE
            viewHolder.dateText?.visibility = View.GONE
            viewHolder.extraInfoLayout?.visibility = View.GONE
            viewHolder.mainLabelText?.text = context.resources.getString(R.string.chart_history_no_fills)
            return outputView
        } else {
            viewHolder.colorView?.visibility = View.VISIBLE
            viewHolder.mainLabelText?.visibility = View.VISIBLE
            viewHolder.priceText?.visibility = View.VISIBLE
            viewHolder.feeText?.visibility = View.VISIBLE
            viewHolder.dateText?.visibility = View.VISIBLE
            val fill = fills[i]
            tradeSide = fill.side
            currency = fill.tradingPair.baseCurrency
            price = fill.price
            amount = fill.amount

            outputView.setOnClickListener {
                fill.showExtraInfo = !fill.showExtraInfo
                fillOnClick(fill)
            }

            viewHolder.colorView?.backgroundColor = when (tradeSide) {
                TradeSide.BUY -> ResourcesCompat.getColor(resources, R.color.anyx_green, null)
                TradeSide.SELL -> ResourcesCompat.getColor(resources, R.color.anyx_red, null)
            }

            val sideString = when (tradeSide) {
                TradeSide.BUY -> context.resources.getString(R.string.chart_history_fill_side_buy)
                TradeSide.SELL -> context.resources.getString(R.string.chart_history_fill_side_sell)
            }

            val totalPrice = fill.price * fill.amount
            val quoteCurrency = fill.tradingPair.quoteCurrency

            viewHolder.mainLabelText?.text = context.resources.getString(R.string.chart_fill_main_label, sideString, amount.format(currency), totalPrice.format(quoteCurrency))
            viewHolder.priceText?.text = context.resources.getString(R.string.chart_fill_price_label, price.format(quoteCurrency), currency)

            if (fill.showExtraInfo) {
                viewHolder.extraInfoLayout?.visibility = View.VISIBLE
                val feeString = fill.feeAsset?.let {
                    fill.fee.format(it)
                } ?: run { fill.fee.format(quoteCurrency) }
                viewHolder.feeText?.text = context.resources.getString(R.string.chart_fill_fee_label, feeString)

                if (fill is CombinedFill && fill.timeList.size > 1) {
                    var dateString = ""
                    for (date in fill.timeList) {
                        dateString += "\n" + date.format(Fill.dateFormat)
                    }
                    viewHolder.dateText?.text = context.resources.getString(R.string.chart_fill_date_multiple, dateString)
                } else {
                    val dateString = fill.time.format(Fill.dateFormat)
                    viewHolder.dateText?.text = context.resources.getString(R.string.chart_fill_date_single, dateString)
                }
            } else {
                viewHolder.extraInfoLayout?.visibility = View.GONE
            }

            return outputView
        }
    }
}