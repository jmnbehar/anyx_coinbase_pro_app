package com.jmnbehar.anyx.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.list_row_alarm.view.*

/**
 * Created by jmnbehar on 11/12/2017.
 */

class AlertListViewAdapter(var inflater: LayoutInflater?, var alerts: List<Alert>, private var onClick: (Alert) -> Unit) : BaseAdapter() {

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
        var productNameText: TextView? = null
        var triggerPriceText: TextView? = null
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        val viewHolder: ViewHolder?
        val outputView: View
        if (convertView == null) {
            viewHolder = ViewHolder()
            outputView = viewGroup.inflate(R.layout.list_row_alarm)
            viewHolder.productNameText = outputView.txt_alert_product
            viewHolder.triggerPriceText = outputView.txt_alert_value

            outputView.tag = viewHolder

        } else {
            viewHolder = convertView.tag as ViewHolder
            outputView = convertView
        }
        val alert = alerts[i]

        viewHolder.productNameText?.text = alert.currency.toString()
        viewHolder.triggerPriceText?.text = alert.price.fiatFormat()

        outputView.setOnClickListener { onClick(alert) }

        return outputView
    }


}