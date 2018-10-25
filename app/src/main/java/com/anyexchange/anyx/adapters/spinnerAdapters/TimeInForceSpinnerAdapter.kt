package com.anyexchange.anyx.adapters.spinnerAdapters

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.anyexchange.anyx.classes.inflate
import com.anyexchange.anyx.R
import com.anyexchange.anyx.classes.TimeInForce
import kotlinx.android.synthetic.main.list_row_coinbase_account.view.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.textColor

/**
 * Created by anyexchange on 3/14/2018.
 */
class TimeInForceSpinnerAdapter(context: Context, private var optionList: List<TimeInForce>) :
        ArrayAdapter<TimeInForce>(context, layoutId, R.id.txt_cb_account_name, optionList) {

    companion object {
        const val layoutId = R.layout.list_row_coinbase_account
    }

    internal class ViewHolder {
        var view: View? = null
        var optionText: TextView? = null
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
            viewHolder.optionText = vi.txt_cb_account_name
            viewHolder.view = vi

            vi.tag = viewHolder
            outputView = vi
        } else {
            viewHolder = convertView.tag as ViewHolder
            outputView = convertView
        }

        viewHolder.view?.backgroundColor = context.resources.getColor(R.color.dark_accent, null)
        viewHolder.optionText?.text = optionList[position].label()
        viewHolder.optionText?.textColor = Color.WHITE

        return outputView
    }
}