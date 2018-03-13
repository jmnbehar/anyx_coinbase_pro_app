package com.jmnbehar.anyx.Fragments.Verify

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.jmnbehar.anyx.Activities.LoginActivity
import com.jmnbehar.anyx.Activities.VerifyActivity
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_verify_send.view.*

/**
 * Created by josephbehar on 1/20/18.
 */

class VerifySendFragment : Fragment() {
    companion object {
        var email = ""
        var amount = 0.0
        var currency: Currency? = null

        fun newInstance(email: String, amount: Double, currency: Currency?): VerifySendFragment
        {
            this.email = email
            this.amount = amount
            this.currency = currency
            return VerifySendFragment()
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

        verifySendButton.setOnClickListener  {
            progressBar.visibility = View.VISIBLE
            val currency = currency ?: Currency.BTC

            //TODO: Test Buy/Sell permission by creating an order for 1 BTC for 1 USD and then cancelling it
            GdaxApi.sendCrypto(amount, currency, GdaxApi.developerAddress(currency)).executeRequest({
                (activity as VerifyActivity).verificationComplete(false)

            }, {
                progressBar.visibility = View.INVISIBLE
                (activity as VerifyActivity).verificationComplete(true)
                val apiKey = GdaxApi.credentials!!.apiKey
                AnyxApi.VerificationSent(apiKey, email).executePost({
                    //TODO: create a queue

                }, {
                    /* succeed silently */
                })
            })
        }
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
        if (currency == null) {
            sendInfoText.text = "To verify your account we will buy "
            sendAmountText.text = "${Companion.amount.btcFormat()} ${Currency.BTC} for PRICE"
            sendInfo2Text.text = "and send it to AnyX, which we will send right back to your Coinbase account with email ${Companion.email}."
            progressBar.visibility = View.GONE
        } else {
            sendInfoText.text = "To verify your account we will send"
            sendAmountText.text = "${Companion.amount.btcFormat()} ${Companion.currency}"
            sendInfo2Text.text = "to AnyX, which we will send right back to your Coinbase account with email ${Companion.email}."
            progressBar.visibility = View.GONE
        }
    }
}