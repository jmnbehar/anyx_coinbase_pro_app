package com.jmnbehar.gdax.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.jmnbehar.gdax.Classes.Account
import com.jmnbehar.gdax.Classes.Currency
import com.jmnbehar.gdax.Classes.btcFormat
import com.jmnbehar.gdax.Classes.fiatFormat
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.list_row_account.view.*

/**
 * Created by jmnbehar on 11/12/2017.
 */

class AccountListViewAdapter(var inflater: LayoutInflater?, var onClick: (Account) -> Unit) : BaseAdapter() {

    override fun getCount(): Int {
        var listSize = Account.list.size + 1
        return if (Account.usdAccount != null) { listSize + 1 } else { listSize }
    }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        var vi = inflater!!.inflate(R.layout.list_row_account, null)
        var accounts = Account.list.toMutableList()
        val usdAccount = Account.usdAccount
        if (usdAccount != null) {
            accounts.add(usdAccount)
        }
        if(i < accounts.size) {
            val account = accounts[i]
            vi.txt_account_name.text = account.currency.fullName
            vi.txt_account_value.text = account.value.fiatFormat()
            if (account.currency != Currency.USD) {
                vi.txt_account_balance.text = account.balance.btcFormat()
                vi.setOnClickListener { onClick(account) }
            } else {
                vi.txt_account_balance.text = ""
            }
            vi.img_account_icon.setImageResource(account.currency.iconId)

        } else {
            vi.txt_account_name.text = "TOTAL"
            val totalValue = accounts.map { a -> a.value }.sum()
            vi.txt_account_value.text = totalValue.fiatFormat()
            vi.txt_account_balance.text = ""
        }

        return vi
    }
}