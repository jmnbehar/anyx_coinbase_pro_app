package com.jmnbehar.gdax.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.jmnbehar.gdax.Classes.Account
import com.jmnbehar.gdax.Classes.ApiProduct
import com.jmnbehar.gdax.Classes.Product
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.list_row_account.view.*


import kotlinx.android.synthetic.main.list_row_product.view.*

/**
 * Created by jmnbehar on 11/12/2017.
 */

class AccountListViewAdapter(var inflater: LayoutInflater?, var accounts: List<Account>, var onClick: (Account) -> Unit) : BaseAdapter() {

    override fun getCount(): Int {
        return accounts.size
    }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        var vi = inflater!!.inflate(R.layout.list_row_account, null)


        vi.txt_account_name.text = accounts[i].currency
        vi.txt_account_value.text = "${accounts[i].value}"
        vi.txt_account_balance.text = "${accounts[i].balance}"

        vi.setOnClickListener { onClick(accounts[i]) }

        return vi
    }

    fun update(accounts: List<Account>) {
        this.accounts = accounts
        notifyDataSetChanged()
    }
}