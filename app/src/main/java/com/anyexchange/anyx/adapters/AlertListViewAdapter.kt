package com.anyexchange.anyx.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.list_row_alert.view.*

/**
 * Created by anyexchange on 11/12/2017.
 */

class AlertListViewAdapter(val context: Context, var inflater: LayoutInflater?, var alerts: List<PriceAlert>, private var onClick: (View, PriceAlert) -> Unit) : BaseAdapter() {

    override fun getCount(): Int {
        return alerts.size
    }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    internal class ViewHolder {
        var currencyIcon: ImageView? = null
        var productNameText: TextView? = null
        var alertSideText: TextView? = null
        var triggerPriceText: TextView? = null
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        val viewHolder: ViewHolder?
        val outputView: View
        if (convertView == null) {
            viewHolder = ViewHolder()
            outputView = viewGroup.inflate(R.layout.list_row_alert)

            viewHolder.currencyIcon = outputView.img_alert_currency
            viewHolder.productNameText = outputView.txt_alert_product
            viewHolder.triggerPriceText = outputView.txt_alert_value
            viewHolder.alertSideText = outputView.txt_alert_side
            outputView.tag = viewHolder

        } else {
            viewHolder = convertView.tag as ViewHolder
            outputView = convertView
        }
        val alert = alerts[i]

        viewHolder.currencyIcon?.setImageResource(alert.currency.iconId)
        viewHolder.productNameText?.text = alert.currency.toString()
        viewHolder.triggerPriceText?.text = alert.price.fiatFormat(Account.defaultFiatCurrency)
        if (alert.triggerIfAbove) {
            viewHolder.alertSideText?.text = context.resources.getString(R.string.alert_above)
        } else {
            viewHolder.alertSideText?.text = context.resources.getString(R.string.alert_below)
        }

        outputView.setOnClickListener { onClick(outputView, alert) }

        return outputView
    }


}