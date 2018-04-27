package com.anyexchange.anyx.Adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.anyexchange.anyx.Classes.Account
import com.anyexchange.anyx.Classes.inflate
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.list_row_coinbase_account.view.*

/**
 * Created by anyexchange on 3/14/2018.
 */
class RelatedAccountSpinnerAdapter(context: Context, var resource: Int, var relatedAccountList: List<Account.RelatedAccount>) :
        ArrayAdapter<Account.RelatedAccount>(context, resource, relatedAccountList) {

    internal class ViewHolder {
        var cbAccountNameText: TextView? = null
        var cbAccountBalanceText: TextView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val viewHolder: ViewHolder?
        val outputView: View

        if (convertView == null) {
            viewHolder = ViewHolder()

            val vi = parent.inflate(R.layout.list_row_coinbase_account)

            viewHolder.cbAccountNameText = vi.txt_cb_account_name
            viewHolder.cbAccountBalanceText = vi.txt_cb_account_balance

            vi.tag = viewHolder
            outputView = vi
        } else {
            viewHolder = convertView.tag as ViewHolder
            outputView = convertView
        }

        val account = relatedAccountList[position]
        viewHolder.cbAccountNameText?.text = account.toString()
        viewHolder.cbAccountBalanceText?.text = "" //""${cbAccount.balance.btcFormat()} ${cbAccount.currency}"

        return outputView
    }
}