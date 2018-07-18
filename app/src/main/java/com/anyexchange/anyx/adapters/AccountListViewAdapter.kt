package com.anyexchange.anyx.adapters

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.list_row_account.view.*
import kotlinx.android.synthetic.main.list_row_product.view.*

/**
 * Created by anyexchange on 11/12/2017.
 */

class AccountListViewAdapter(var onClick: (Account) -> Unit) : BaseAdapter() {

    override fun getCount(): Int {
        val listSize = Account.list.size
        return if (Account.usdAccount != null) { listSize + 1 } else { listSize }
    }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    private val sortedAccountList: List<Account>
        get() {
            val sortedAccounts = Account.list.sortedWith(compareBy({ it.value }, { it.currency.orderValue })).reversed().toMutableList()
            val usdAccount = Account.usdAccount
            if (usdAccount != null) {
                sortedAccounts.add(usdAccount)
            }
            return sortedAccounts
        }

    internal class ViewHolder {
        var iconView: ImageView? = null
        var balanceText: TextView? = null
        var accountValueText: TextView? = null
        var percentChangeText: TextView? = null
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        val viewHolder: ViewHolder?
        val outputView: View
        if (convertView == null) {
            viewHolder = ViewHolder()

            val vi = viewGroup.inflate(R.layout.list_row_product)

            viewHolder.iconView = vi.img_account_icon
            viewHolder.balanceText = vi.txt_account_balance
            viewHolder.accountValueText = vi.txt_account_value
            viewHolder.percentChangeText = vi.txt_account_percent_change

            vi.tag = viewHolder
            outputView = vi
        } else {
            viewHolder = convertView.tag as ViewHolder
            outputView = convertView
        }

        val accounts = sortedAccountList
        if(i < accounts.size) {
            val account = accounts[i]

            if (account.currency != Currency.USD) {
                viewHolder.balanceText?.text = account.balance.btcFormat() + " " + account.currency.toString()
                outputView.setOnClickListener { onClick(account) }

                val percentChange = account.product.percentChange(Timespan.DAY)

                if (account.value > 0) {
                    viewHolder.accountValueText?.text = account.value.fiatFormat()
                    val accountChange = (percentChange * account.value) / 100
                    val sign = if (percentChange >= 0) { "+" } else { "" }
                    viewHolder.percentChangeText?.text = percentChange.percentFormat() + "\n($sign${accountChange.fiatFormat()})"
                } else {
                    viewHolder.accountValueText?.visibility = View.INVISIBLE
                    viewHolder.percentChangeText?.visibility = View.INVISIBLE
                }
            } else {
                viewHolder.accountValueText?.text = account.value.fiatFormat()
                viewHolder.balanceText?.text = account.currency.toString()
                viewHolder.balanceText?.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                viewHolder.percentChangeText?.text = ""
            }
            viewHolder.iconView?.setImageResource(account.currency.iconId)
        }

        return outputView
    }
}