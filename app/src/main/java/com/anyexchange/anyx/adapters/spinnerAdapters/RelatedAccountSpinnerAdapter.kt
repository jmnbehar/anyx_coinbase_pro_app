package com.anyexchange.anyx.adapters.spinnerAdapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.anyexchange.anyx.classes.inflate
import com.anyexchange.anyx.R
import com.anyexchange.anyx.classes.BaseAccount
import kotlinx.android.synthetic.main.list_row_coinbase_account.view.*
import org.jetbrains.anko.backgroundColor

/**
 * Created by anyexchange on 3/14/2018.
 */
class RelatedAccountSpinnerAdapter(context: Context, var relatedAccountList: List<BaseAccount>) :
        ArrayAdapter<BaseAccount>(context, layoutId, R.id.txt_cb_account_name, relatedAccountList) {

    companion object {
        const val layoutId = R.layout.list_row_coinbase_account
    }

    internal class ViewHolder {
        var view: View? = null
        var accountNameText: TextView? = null
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
            viewHolder.accountNameText = vi.txt_cb_account_name
            viewHolder.view = vi

            vi.tag = viewHolder
            outputView = vi
        } else {
            viewHolder = convertView.tag as ViewHolder
            outputView = convertView
        }

        val account = relatedAccountList[position]
        viewHolder.accountNameText?.text = account.toString()
        viewHolder.view?.backgroundColor = context.resources.getColor(R.color.dark_accent, null)

        return outputView
    }
}