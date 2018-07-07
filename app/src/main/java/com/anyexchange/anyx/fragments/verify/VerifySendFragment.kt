package com.anyexchange.anyx.fragments.verify

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.anyexchange.anyx.activities.VerifyActivity
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.fragment_verify_send.view.*
import org.jetbrains.anko.support.v4.alert
import com.anyexchange.anyx.classes.Currency
import org.jetbrains.anko.support.v4.toast


/**
 * Created by josephbehar on 1/20/18.
 */

class VerifySendFragment : Fragment() {
    companion object {
        fun newInstance(): VerifySendFragment
        {
            return VerifySendFragment()
        }
    }

    private lateinit var sendInfoText: TextView

    private lateinit var progressBar: ProgressBar

    private lateinit var verifySendButton: Button

    private val currency: Currency
        get() {
            return if (activity is VerifyActivity) {
                val activity = (activity as VerifyActivity)
                activity.currency
            } else {
                Currency.BTC
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_verify_send, container, false)

        sendInfoText = rootView.txt_verify_send_info

        progressBar = rootView.progress_bar_verify_send
        verifySendButton = rootView.btn_verify_send

        val verifyAccount = Account.forCurrency(currency)!!
        verifySendButton.setOnClickListener  {
            progressBar.visibility = View.VISIBLE
            val productId = verifyAccount.product.id
            CBProApi.orderLimit(TradeSide.BUY, productId, 1.0, 1.0,
                    timeInForce = CBProApi.TimeInForce.ImmediateOrCancel, cancelAfter = null).executePost({ result->
                val errorMessage = CBProApi.ErrorMessage.forString(result.errorMessage)
                when (errorMessage) {
                    CBProApi.ErrorMessage.InsufficientFunds -> sendCryptoToVerify()
                    else -> goToVerificationComplete(VerificationStatus.NoTradePermission)
                }
            }, { _ ->
                sendCryptoToVerify()
            })
        }

        updateViews()
        return rootView
    }

    private fun sendCryptoToVerify() {
        CBProApi.coinbaseAccounts().linkToAccounts({
            toast("Unknown Error: Try again later")
        }, {
            val coinbaseAccount = Account.forCurrency(currency)?.coinbaseAccount
            if (coinbaseAccount == null) {
                toast("Unknown Error: Try again later")
            } else {

                val sendAmount = 0.000001
                CBProApi.sendCrypto(sendAmount, currency, currency.verificationAddress).executePost({ result ->
                    val errorMessage = CBProApi.ErrorMessage.forString(result.errorMessage)
                    progressBar.visibility = View.INVISIBLE
                    when (errorMessage) {
                        CBProApi.ErrorMessage.TransferAmountTooLow -> goToVerificationComplete(VerificationStatus.Success)
                        CBProApi.ErrorMessage.Forbidden -> goToVerificationComplete(VerificationStatus.NoTransferPermission)
                        CBProApi.ErrorMessage.InsufficientFunds -> goToVerificationComplete(VerificationStatus.Success)
                        CBProApi.ErrorMessage.InvalidCryptoAddress -> goToVerificationComplete(VerificationStatus.UnknownError)
                        else -> goToVerificationComplete(VerificationStatus.UnknownError)
                    }
                }, {
                    //we should never get here
                    goToVerificationComplete(VerificationStatus.Success)
                })
            }
        })

    }
    private fun goToVerificationComplete(verificationStatus: VerificationStatus) {
        val prefs = Prefs(context!!)
        val apiKey = CBProApi.credentials!!.apiKey
        if (verificationStatus.isVerified) {
            prefs.approveApiKey(apiKey)
        } else {
            prefs.rejectApiKey(apiKey)
        }
        (activity as VerifyActivity).verificationComplete(verificationStatus)
    }

    private fun showPopup(titleString: String, messageString: String, positiveText: String = "OK", positiveAction: () -> Unit, negativeText: String? = null, negativeAction: () -> Unit = {}) {
        alert {
            title = titleString
            message = messageString
            positiveButton(positiveText) { positiveAction() }
            if (negativeText != null) {
                negativeButton(negativeText) { negativeAction() }
            }
        }.show()
    }

    override fun onResume() {
        updateViews()
        super.onResume()
    }


    fun updateViews() {
        val account = Account.forCurrency(currency)

        var explanationText = "To verify your account we will test to ensure that your API Key has all required permissions."
        if (account?.balance ?: 0.0 > 0.0) {
            explanationText += "\n\nA very small amount of $currency may get sent to your Coinbase account."
        }
        sendInfoText.text = explanationText

        progressBar.visibility = View.INVISIBLE
    }
}