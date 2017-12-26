package com.jmnbehar.gdax.Fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jmnbehar.gdax.Classes.Account
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_trade.view.*

/**
 * Created by jmnbehar on 11/5/2017.
 */
class TradeFragment : Fragment() {
    private lateinit var inflater: LayoutInflater

    companion object {
        lateinit var account: Account
        fun newInstance(account: Account): TradeFragment {
            this.account = account
            return TradeFragment()
        }
        fun newInstance(): TradeFragment {
            return TradeFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_trade, container, false)

//        rootView.setBackgroundColor(Color.YELLOW)
        this.inflater = inflater

        rootView.txt_trade_name.text = account.currency

        return rootView
    }
}
