package com.anyexchange.anyx.adapters.spinnerAdapters

import android.content.Context
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filterable
import android.widget.TextView
import com.anyexchange.anyx.classes.Currency
import com.anyexchange.anyx.classes.inflate
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.list_row_spinner_nav.view.*
import android.text.TextUtils
import android.widget.Filter
import com.anyexchange.anyx.views.searchableSpinner.ISpinnerSelectedView


/**
 * Created by anyexchange on 3/14/2018.
 */
class NavigationSpinnerAdapter(context: Context, resource: Int, textViewId: Int, private var currencyList: List<Currency>) :
        ArrayAdapter<Currency>(context, resource, textViewId, currencyList), Filterable, ISpinnerSelectedView {
    private var backupCurrencyList = currencyList
    private val mStringFilter = StringFilter()

    internal class ViewHolder {
        var currencyTxt: TextView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = getViewGeneric(position, convertView, parent)
        view.setBackgroundColor(ContextCompat.getColor(context, R.color.dark_accent))

        return view
    }

    override fun getSelectedView(position: Int): View {
        val view = View.inflate(context, R.layout.list_row_spinner_nav, null)
        val displayName = view.findViewById(R.id.txt_currency) as TextView
        val currency = currencyList[position]
        displayName.text = currencyDisplayName(currency)

        displayName.setTextColor(Color.WHITE)

        return view
    }

    override fun getNoSelectionView(): View {
        return View.inflate(context, R.layout.view_list_no_selection_item, null)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
//        val currency = backupCurrencyList[position]
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

        if (currencyList.size > position) {
            val currency = currencyList[position]

            viewHolder.currencyTxt?.text = currencyDisplayName(currency)
            viewHolder.currencyTxt?.visibility = View.VISIBLE
        } else {
            viewHolder.currencyTxt?.visibility = View.GONE
        }
        return outputView
    }
    override fun getFilter(): Filter {
        return mStringFilter
    }

    private fun currencyDisplayName(currency: Currency) : String {
        val fullName = currency.fullName
        return if (fullName != null) {
            context.getString(R.string.currency_id_and_fullname, currency.id, fullName)
        } else {
            currency.id
        }
    }

    override fun getCount(): Int {
        return currencyList.size
    }

    override fun getItem(position: Int): Currency? {
        return currencyList[position]
    }

    inner class StringFilter : Filter() {

        override fun performFiltering(constraint: CharSequence): FilterResults {
            val filterResults = FilterResults()
            if (TextUtils.isEmpty(constraint)) {
                filterResults.count = backupCurrencyList.size
                filterResults.values = backupCurrencyList
                return filterResults
            }
            val filteredCurrencies = mutableListOf<Currency>()
            val searchTerm = constraint.toString().toLowerCase()
            for (currency in backupCurrencyList) {
                if (currency.id.toLowerCase().contains(searchTerm)) {
                    filteredCurrencies.add(currency)
                } else if (currency.fullName?.toLowerCase()?.contains(searchTerm) == true) {
                    filteredCurrencies.add(currency)
                } else if (currency.symbol.toLowerCase().contains(searchTerm)) {
                    filteredCurrencies.add(currency)
                }
            }
            filterResults.count = filteredCurrencies.size
            filterResults.values = filteredCurrencies
            return filterResults
        }

        override fun publishResults(constraint: CharSequence, results: FilterResults) {
            val resultList = results.values as List<*>
            currencyList = resultList.filterIsInstance<Currency>()
            notifyDataSetChanged()
        }
    }

}