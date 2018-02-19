package com.jmnbehar.gdax.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.list_row_account.view.*

/**
 * Created by jmnbehar on 11/12/2017.
 */

class AccountListViewAdapter(var inflater: LayoutInflater?, var onClick: (Account) -> Unit) : BaseAdapter() {

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
        var vi = inflater!!.inflate(R.layout.list_row_account, null)
        val accounts = Account.list.toMutableList()
        val usdAccount = Account.usdAccount
        if (usdAccount != null) {
            accounts.add(usdAccount)
        }
        if(i < accounts.size) {
            val account = accounts[i]
            vi.txt_account_name.text = account.currency.toString()
            if (account.currency != Currency.USD) {
                vi.txt_account_balance.text = account.balance.btcFormat() + " " + account.currency.toString()
                vi.setOnClickListener { onClick(account) }


                val candles = account.product.dayCandles
                val currentPrice = account.product.price
                val open = if (candles.isNotEmpty()) {
                    candles.first().open
                } else {
                    0.0
                }
                val change = currentPrice - open
                val weightedChange: Double = (change / open)
                val percentChange: Double = weightedChange * 100.0
                if (account.value > 0) {
                    vi.txt_account_value.text = "$${account.value.fiatFormat()}"
                    vi.txt_account_percent_change.text = percentChange.percentFormat()
                } else {
                    vi.txt_account_value.visibility = View.INVISIBLE
                    vi.txt_account_percent_change.visibility = View.INVISIBLE
                }
            } else {
                vi.txt_account_value.text = "$${account.value.fiatFormat()}"
                vi.txt_account_balance.visibility = View.INVISIBLE
//                vi.txt_account_percent_change.visibility = View.INVISIBLE
                vi.txt_account_percent_change.text = ""
            }
            vi.img_account_icon.setImageResource(account.currency.iconId)
        }

        return vi
    }
}