package com.jmnbehar.gdax.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
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

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        var vi = inflater!!.inflate(R.layout.list_row_alarm, null)

        val alert = alerts[i]

        vi.txt_alert_product.text = alert.currency.toString()
        vi.txt_alert_value.text = alert.price.fiatFormat()

        vi.setOnClickListener { onClick(alert) }

        return vi
    }


}