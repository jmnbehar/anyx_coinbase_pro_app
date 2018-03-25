package com.jmnbehar.anyx.Fragments.Verify

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.jmnbehar.anyx.Activities.VerifyActivity
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_verify_send.view.*
import org.jetbrains.anko.support.v4.alert
import com.jmnbehar.anyx.Classes.Currency
import java.util.*


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
            val prefs = Prefs(context!!)
            val apiKey = GdaxApi.credentials!!.apiKey

            //TODO: Test Buy/Sell permission by creating an order for 1 BTC for 1 USD and then cancelling it
            GdaxApi.sendCrypto(amount, currency, currency.verificationAddress).executePost({
                progressBar.visibility = View.INVISIBLE
                prefs.rejectApiKey(apiKey)
                goToVerificationComplete(VerificationStatus.NoTransferPermission)
            }, {
                prefs.approveApiKey(apiKey)
                progressBar.visibility = View.INVISIBLE

                val timestamp = (Date().timeInSeconds()).toString()
                //TODO: add spinner to verification activity
                AnyxApi.VerificationSent(apiKey, email).executePost({ error ->
                    showPopup("Your account is verified, but we had a problem with our servers. To ensure repayment, press OK to send an email with your verification details", "OK",  {
                        sendVerificationEmail(timestamp)
                        goToVerificationComplete(VerificationStatus.RepayErrorEmailed)
                    }, "Cancel", {
                        showPopup("Are you sure you don't want to send an email? Your $amount $currency might not be repaid.",
                                "OK", {
                                    goToVerificationComplete(VerificationStatus.RepayError)
                                },
                                "Send Email", {
                                    sendVerificationEmail(timestamp)
                                    goToVerificationComplete(VerificationStatus.RepayErrorEmailed)
                                })
                    })
                }, { result ->
                    goToVerificationComplete(VerificationStatus.Success)
                }, 3)
            })
        }
        updateViews()
        return rootView
    }

    private fun goToVerificationComplete(verificationStatus: VerificationStatus) {
        val prefs = Prefs(context!!)
        val apiKey = GdaxApi.credentials!!.apiKey
        if (verificationStatus.isVerified) {
            prefs.approveApiKey(apiKey)
        } else {
            prefs.rejectApiKey(apiKey)
        }
        (activity as VerifyActivity).verificationComplete(verificationStatus)
    }

    private fun showPopup(string: String, positiveText: String = "OK", positiveAction: () -> Unit, negativeText: String? = null, negativeAction: () -> Unit = {}) {
        alert {
            title = string
            positiveButton(positiveText) { positiveAction() }
            if (negativeText != null) {
                negativeButton(negativeText) { negativeAction() }
            }
        }.show()
    }

    private fun sendVerificationEmail(timestamp: String) {
        val credentials = GdaxApi.credentials!!.apiKey
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "anyx.verification@gmail.com", null))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "AnyX Verification")
        emailIntent.putExtra(Intent.EXTRA_TEXT, "AnyX Account was properly verified, but we had trouble with our repayment system. Please do not edit the details in this email or we will not be able to process repayment. " +
                "Do not edit: Sent $amount $currency at $timestamp from $credentials, repay to $email")
        startActivity(Intent.createChooser(emailIntent, "Send email..."))
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