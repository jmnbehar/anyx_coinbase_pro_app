package com.anyexchange.anyx.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.list_row_account.view.*

/**
 * Created by anyexchange on 11/12/2017.
 */

class AccountListViewAdapter(val context: Context, var onClick: (Account) -> Unit) : BaseAdapter() {
    private var sortedAccountList: List<Account>
    init {
        sortedAccountList = sortedAccountList()
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
        sortedAccountList = sortedAccountList()
    }

    override fun getCount(): Int {
        return sortedAccountList.size
    }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    private fun sortedAccountList(): List<Account> {
        val allCryptoAccounts = Account.allCryptoAccounts()
        val nonEmptyCryptoAccounts = allCryptoAccounts.filter { it.balance > 0 }

        val sortedAccounts = nonEmptyCryptoAccounts.sortedWith(compareBy({ it.defaultValue }, { it.currency.orderValue })).reversed().toMutableList()
        val sortedFiatAccounts = Account.fiatAccounts.sortedWith(compareBy({ it.defaultValue }, { it.currency.orderValue })).reversed()
        sortedAccounts.addAll(sortedFiatAccounts)

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

            val vi = viewGroup.inflate(R.layout.list_row_account)

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

            if (!account.currency.isFiat) {
                viewHolder.balanceText?.text = context.resources.getString(R.string.accounts_balance_text, account.balance.btcFormat(), account.currency.toString())
                outputView.setOnClickListener { onClick(account) }

                val product = Product.map[account.currency.id]
                val percentChange = product?.percentChange(Timespan.DAY, Account.defaultFiatCurrency) ?: 0.0

                if (account.defaultValue > 0) {
                    viewHolder.accountValueText?.text = account.defaultValue.fiatFormat(Account.defaultFiatCurrency)
                    val accountChange = (percentChange * account.defaultValue) / 100
                    val sign = if (percentChange >= 0) { "+" } else { "" }
                    viewHolder.percentChangeText?.text = context.resources.getString(R.string.accounts_percent_change_text, percentChange.percentFormat(), sign, accountChange.fiatFormat(Account.defaultFiatCurrency))
                    viewHolder.accountValueText?.visibility = View.VISIBLE
                    viewHolder.percentChangeText?.visibility = View.VISIBLE
                } else {
                    viewHolder.accountValueText?.visibility = View.INVISIBLE
                    viewHolder.percentChangeText?.visibility = View.INVISIBLE
                }
            } else {
                viewHolder.accountValueText?.text = account.defaultValue.fiatFormat(Account.defaultFiatCurrency)
                viewHolder.balanceText?.text = account.currency.toString()
                viewHolder.balanceText?.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                viewHolder.percentChangeText?.text = ""
            }
            account.currency.iconId?.let {
                viewHolder.iconView?.visibility = View.VISIBLE
                viewHolder.iconView?.setImageResource(it)
            } ?: run {
                viewHolder.iconView?.visibility = View.INVISIBLE
            }
        }

        return outputView
    }
}