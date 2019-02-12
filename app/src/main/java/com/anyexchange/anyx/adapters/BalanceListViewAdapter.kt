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

class BalanceListViewAdapter(val context: Context, var exchange: Exchange?) : BaseAdapter() {
    var sortedAccountList: List<Account>
    init {
        sortedAccountList = sortedAccountList()
    }

    override fun notifyDataSetChanged() {
        sortedAccountList = sortedAccountList()
        super.notifyDataSetChanged()
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
        val allCryptoAccounts = if (exchange == null) {
            Account.allCryptoAccounts()
        } else {
            Account.allCryptoAccounts().filter { it.exchange == exchange }
        }
        val nonEmptyCryptoAccounts = allCryptoAccounts.filter { it.balance > 0 }

        val filteredFiatAccounts = if (exchange == null) {
            Account.fiatAccounts
        } else {
            Account.fiatAccounts.filter { it.exchange == exchange }
        }
        val sortedFiatAccounts = filteredFiatAccounts.sortAccounts().toMutableList()
        val sortedCryptoAccounts = nonEmptyCryptoAccounts.sortAccounts()
        sortedFiatAccounts.addAll(sortedCryptoAccounts)

        return sortedFiatAccounts
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
            when (account.currency.type) {
                Currency.Type.FIAT -> {
                    viewHolder.accountValueText?.text = account.defaultValue.format(Account.defaultFiatCurrency)
                    viewHolder.balanceText?.text = "${account.defaultValue.fiatFormat()} ${account.currency}"
                    viewHolder.percentChangeText?.text = ""
                }
                Currency.Type.STABLECOIN -> {
                    viewHolder.accountValueText?.text = account.defaultValue.format(Account.defaultFiatCurrency)
                    viewHolder.balanceText?.text = account.defaultValue.format(account.currency)
                    viewHolder.percentChangeText?.text = ""
                }
                Currency.Type.CRYPTO -> {
                    viewHolder.balanceText?.text =  account.balance.format(account.currency)

                    val product = Product.map[account.currency.id]
                    val percentChange = product?.percentChange(Timespan.DAY, Account.defaultFiatCurrency) ?: 0.0

                    if (account.defaultValue > 0) {
                        viewHolder.accountValueText?.text = account.defaultValue.format(Account.defaultFiatCurrency)
                        val accountChange = (percentChange * account.defaultValue) / 100
                        val sign = if (percentChange >= 0) { "+" } else { "" }
                        viewHolder.percentChangeText?.text = context.resources.getString(R.string.balances_percent_change_text, percentChange.percentFormat(), sign, accountChange.format(Account.defaultFiatCurrency))
                        viewHolder.accountValueText?.visibility = View.VISIBLE
                        viewHolder.percentChangeText?.visibility = View.VISIBLE
                    } else {
                        viewHolder.accountValueText?.visibility = View.INVISIBLE
                        viewHolder.percentChangeText?.visibility = View.INVISIBLE
                    }
                }
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