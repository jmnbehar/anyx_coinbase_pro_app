package com.anyexchange.anyx.adapters.spinnerAdapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.anyexchange.anyx.classes.Currency
import com.anyexchange.anyx.classes.inflate
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.list_row_spinner_nav.view.*

/**
 * Created by anyexchange on 3/14/2018.
 */
class NavigationSpinnerAdapter(context: Context, var resource: Int, textViewId: Int, var currencyList: List<Currency>) :
        ArrayAdapter<Currency>(context, resource, textViewId, currencyList) {


    internal class ViewHolder {
        var currencyTxt: TextView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        return getViewGeneric(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
//        val currency = currencyList[position]
//        val dropDownView = getViewGeneric(position, convertView, parent)
//        dropDownView.backgroundColor = currency.colorFade(context)
        return getViewGeneric(position, convertView, parent)
    }

    private fun getViewGeneric(position: Int, convertView: View?, parent: ViewGroup): View {
        val viewHolder: ViewHolder?
        val outputView: View

        if (convertView == null) {
            viewHolder = ViewHolder()
            val vi = parent.inflate(R.layout.list_row_spinner_nav)
            viewHolder.currencyTxt = vi.txt_currency
            vi.tag = viewHolder
            outputView = vi
        } else {
            viewHolder = convertView.tag as ViewHolder
            outputView = convertView
        }

        val currency = currencyList[position]

        viewHolder.currencyTxt?.text = "$currency - ${currency.fullName}"

        return outputView
    }
}