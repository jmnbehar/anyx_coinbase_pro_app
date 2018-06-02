package com.anyexchange.anyx.adapters

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.list_row_account.view.*
import kotlinx.coroutines.experimental.channels.produce

/**
 * Created by anyexchange on 11/12/2017.
 */

class AccountListViewAdapter(var onClick: (Account) -> Unit) : BaseAdapter() {

    override fun getCount(): Int {
        var listSize = Account.list.size
        return if (Account.usdAccount != null) { listSize + 1 } else { listSize }
    }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        //TODO: quash this warning
        val vi = viewGroup.inflate(R.layout.list_row_account)
        val accounts = Account.list.toMutableList()
        val usdAccount = Account.usdAccount
        if (usdAccount != null) {
            accounts.add(usdAccount)
        }
        if(i < accounts.size) {
            val account = accounts[i]
           // vi.txt_account_name.text = account.currency.toString()
            if (account.currency != Currency.USD) {
                vi.txt_account_balance.text = account.balance.btcFormat() + " " + account.currency.toString()
                vi.setOnClickListener { onClick(account) }

                val percentChange = account.product.percentChange(Timespan.DAY)

                if (account.value > 0) {
                    vi.txt_account_value.text = account.value.fiatFormat()

                    val accountChange = (percentChange * account.value) / 100
                    val sign = if (percentChange >= 0) { "+" } else { "" }
                    vi.txt_account_percent_change.text = percentChange.percentFormat() + "\n($sign${accountChange.fiatFormat()})"
                } else {
                    vi.txt_account_value.visibility = View.INVISIBLE
                    vi.txt_account_percent_change.visibility = View.INVISIBLE
                }
            } else {
                vi.txt_account_value.text = account.value.fiatFormat()
                vi.txt_account_balance.text = account.currency.toString()
                vi.txt_account_balance.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
//                vi.txt_account_percent_change.visibility = View.INVISIBLE
                vi.txt_account_percent_change.text = ""
            }
            vi.img_account_icon.setImageResource(account.currency.iconId)
        }

        return vi
    }
}