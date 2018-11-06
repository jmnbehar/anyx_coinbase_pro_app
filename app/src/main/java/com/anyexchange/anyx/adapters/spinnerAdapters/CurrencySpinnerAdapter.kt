package com.anyexchange.anyx.adapters.spinnerAdapters

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.anyexchange.anyx.classes.inflate
import com.anyexchange.anyx.R
import com.anyexchange.anyx.classes.Currency
import kotlinx.android.synthetic.main.list_row_default_spinner.view.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.textColor

/**
 * Created by anyexchange on 3/14/2018.
 */
class CurrencySpinnerAdapter(context: Context, var currencyList: List<Currency>) :
        ArrayAdapter<Currency>(context, layoutId, textResId, currencyList) {

    //TODO: merge this with NavigationSpinner
    companion object {
        const val layoutId = R.layout.list_row_default_spinner
        const val textResId = R.id.txt_spinner_label
    }

    internal class ViewHolder {
        var view: View? = null
        var currencyText: TextView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        return getViewGeneric(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getViewGeneric(position, convertView, parent)
    }

    private fun getViewGeneric(position: Int, convertView: View?, parent: ViewGroup): View {
        val viewHolder: ViewHolder?
        val outputView: View
        if (convertView == null) {
            viewHolder = ViewHolder()

            val vi = parent.inflate(layoutId)

            viewHolder.view = vi
            viewHolder.currencyText = vi.txt_spinner_label

            vi.tag = viewHolder
            outputView = vi
        } else {
            viewHolder = convertView.tag as ViewHolder
            outputView = convertView
        }

        val currency = currencyList[position]
        viewHolder.currencyText?.textColor = Color.WHITE

        viewHolder.currencyText?.text = currency.id

        viewHolder.view?.backgroundColor = context.resources.getColor(R.color.dark_accent, null)

        return outputView
    }
}