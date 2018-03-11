package com.jmnbehar.anyx.Fragments.Verify

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import com.jmnbehar.anyx.Activities.VerifyActivity
import com.jmnbehar.anyx.Classes.Currency
import com.jmnbehar.anyx.Classes.btcFormat
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_settings.view.*
import kotlinx.android.synthetic.main.fragment_verify_send.view.*

/**
 * Created by josephbehar on 1/20/18.
 */

class VerififySendFragment : Fragment() {
    companion object {
        var email = ""
        var amount = 0.0001
        var currency = Currency.BTC

        fun newInstance(email: String, amount: Double, currency: Currency): VerififySendFragment
        {
            this.email = email
            this.amount = amount
            this.currency = currency
            return VerififySendFragment()
        }
    }

    private lateinit var sendInfoText: TextView
    private lateinit var sendAmountText: TextView
    private lateinit var sendInfo2Text: TextView

    private lateinit var progressBar: ProgressBar

    private lateinit var verifySendButton: Button


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_verify_send, container, false)

        sendInfoText = rootView.txt_verify_send_info
        sendAmountText = rootView.txt_verify_send_amount
        sendInfo2Text = rootView.txt_verify_send_info_2

        progressBar = rootView.progress_bar_verify_send
        verifySendButton = rootView.btn_verify_send

        updateViews()

        return rootView
    }

    override fun onResume() {
        updateViews()
        super.onResume()
    }

    private fun getDataFromActivity() {
        if (activity is VerifyActivity) {
            Companion.email = (activity as VerifyActivity).email
            Companion.amount = (activity as VerifyActivity).amount
            Companion.currency = (activity as VerifyActivity).currency
        }
    }

    fun updateViews() {
        getDataFromActivity()
        sendInfoText.text = "To verify your account we will send"
        sendAmountText.text = "${Companion.amount.btcFormat()} ${Companion.currency}"
        sendInfo2Text.text = "to AnyX, which we will send right back to your coinbase account with email ${Companion.email}."
        progressBar.visibility = View.GONE
    }
}