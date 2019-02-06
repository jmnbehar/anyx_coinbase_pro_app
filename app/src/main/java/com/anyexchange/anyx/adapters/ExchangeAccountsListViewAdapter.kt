package com.anyexchange.anyx.adapters

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.api.BinanceApi
import com.anyexchange.anyx.api.CBProApi
import com.anyexchange.anyx.classes.*
import kotlinx.android.synthetic.main.list_row_exchange_account.view.*

/**
 * Created by anyexchange on 11/12/2017.
 */

class ExchangeAccountsListViewAdapter(val context: Context, var exchanges: Array<Exchange>, var resources: Resources) : BaseAdapter() {

    override fun getCount(): Int {
        return exchanges.size
    }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    internal class ViewHolder {
        var exchangeLogoView: ImageView? = null
        var exchangeNameView: TextView? = null

        var apiKeyLabelView: TextView? = null
        var apiKeyView: TextView? = null

        //TODO: consider adding a permissions view here
        //TODO: consider adding a test button here

        var loginButton: Button? = null
        var logoutButton: Button? = null
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        val viewHolder: ViewHolder?
        val outputView: View
        if (convertView == null) {
            viewHolder = ViewHolder()

            val vi = viewGroup.inflate(R.layout.list_row_exchange_account)

            viewHolder.exchangeLogoView = vi.img_exchange_logo
            viewHolder.exchangeNameView = vi.txt_exchange_name

            viewHolder.apiKeyLabelView = vi.txt_exchange_account_api_key_label
            viewHolder.apiKeyView = vi.txt_exchange_account_api_key

            viewHolder.loginButton = vi.btn_exchange_account_login
            viewHolder.logoutButton = vi.btn_exchange_account_logout

            vi.tag = viewHolder
            outputView = vi
        } else {
            viewHolder = convertView.tag as ViewHolder
            outputView = convertView
        }

        val exchange = exchanges[i]

        viewHolder.exchangeLogoView?.setImageResource(R.drawable.fail_icon)

        viewHolder.exchangeNameView?.text = exchange.name


        var apiKey: String? = null
        when (exchange) {
            Exchange.CBPro -> {
                if (CBProApi.credentials != null) {
                    apiKey = CBProApi.credentials!!.apiKey
                }
                viewHolder.logoutButton?.setOnClickListener {
                    CBProApi.credentials = null
                    genericLogOut()
                }
            }
            Exchange.Binance -> {
                if (BinanceApi.credentials != null) {
                    apiKey = BinanceApi.credentials!!.apiKey
                }

                viewHolder.logoutButton?.setOnClickListener {
                    BinanceApi.credentials = null
                    genericLogOut()
                }
            }
        }

        if (apiKey != null) {
            viewHolder.apiKeyLabelView?.visibility = View.VISIBLE
            viewHolder.apiKeyView?.visibility = View.VISIBLE
            viewHolder.apiKeyLabelView?.text =  "API Key:"
            viewHolder.apiKeyView?.text = apiKey
            viewHolder.logoutButton?.visibility = View.VISIBLE

            viewHolder.loginButton?.text = "Change Log In Info"
        } else {
            viewHolder.apiKeyLabelView?.visibility = View.INVISIBLE
            viewHolder.apiKeyView?.visibility = View.INVISIBLE
            viewHolder.logoutButton?.visibility = View.GONE

            viewHolder.loginButton?.text = "Log In"
        }


        return outputView
    }


    private fun genericLogOut() {
        val prefs = Prefs(context)
        prefs.isLoggedIn = false
        for (product in Product.map.values) {
            product.accounts = mapOf()
        }
        prefs.stashedProducts = Product.map.values.toList()
        prefs.nukeStashedOrders()
        prefs.nukeStashedFills()
    }
}