package com.anyexchange.anyx.adapters.spinnerAdapters

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filterable
import android.widget.TextView
import com.anyexchange.anyx.classes.Currency
import com.anyexchange.anyx.classes.inflate
import com.anyexchange.anyx.R
import gr.escsoft.michaelprimez.searchablespinner.interfaces.ISpinnerSelectedView
import kotlinx.android.synthetic.main.list_row_spinner_nav.view.*

/**
 * Created by anyexchange on 3/14/2018.
 */
class NavigationSpinnerAdapter(context: Context, var resource: Int, textViewId: Int, var currencyList: List<Currency>) :
        ArrayAdapter<Currency>(context, resource, textViewId, currencyList), Filterable, ISpinnerSelectedView {


    internal class ViewHolder {
        var currencyTxt: TextView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val view = getViewGeneric(position, convertView, parent)
        view.setBackgroundColor(ContextCompat.getColor(context, R.color.dark_accent))

        return getViewGeneric(position, convertView, parent)
    }

    override fun getSelectedView(position: Int): View {
        var view: View? = null
        if (position == 0) {
            view = noSelectionView
        } else {
            view = View.inflate(context, R.layout.list_row_spinner_nav, null)
            val displayName = view!!.findViewById(R.id.txt_currency) as TextView
            val currency = currencyList[position]
            displayName.text = "$currency - ${currency.fullName}"
        }
        return view
    }

    override fun getNoSelectionView(): View {
        return View.inflate(context, R.layout.view_list_no_selection_item, null)
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