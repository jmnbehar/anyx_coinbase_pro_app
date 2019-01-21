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
import com.anyexchange.anyx.classes.api.CBProApi
import kotlinx.android.synthetic.main.fragment_verify_send.view.*
import com.anyexchange.anyx.classes.Currency


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

        verifySendButton.setOnClickListener  {
            progressBar.visibility = View.VISIBLE

            val productId = Product.map[currency.id]?.defaultTradingPair?.idForExchange(Exchange.CBPro)
            if (productId != null) {
                CBProApi.orderLimit(null, TradeSide.BUY, productId, 1.0, 1.0,
                        timeInForce = TimeInForce.ImmediateOrCancel, cancelAfter = null).executePost({ result->
                    val errorMessage = CBProApi.ErrorMessage.forString(result.errorMessage)
                    when (errorMessage) {
                        CBProApi.ErrorMessage.InsufficientFunds -> sendCryptoToVerify()
                        else -> goToVerificationComplete(VerificationStatus.NoTradePermission)
                    }
                }, {
                    sendCryptoToVerify()
                })
            } else {
                goToVerificationComplete(VerificationStatus.UnknownError)
            }
        }

        updateViews()
        return rootView
    }

    private fun sendCryptoToVerify() {
        CBProApi.coinbaseAccounts(null).linkToAccounts({
            toast(R.string.verify_unknown_error)
        }, {
            val coinbaseAccount = Account.forCurrency(currency, Exchange.CBPro)?.coinbaseAccount
            val devAddress = currency.developerAddress
            if (coinbaseAccount == null) {
                toast(R.string.verify_unknown_error)
            } else if (devAddress != null){
                val sendAmount = 0.000001
                CBProApi.sendCrypto(null, sendAmount, currency, devAddress).executePost({ result ->
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
        (activity as? VerifyActivity)?.let { verifyActivity ->
            val prefs = Prefs(verifyActivity)
            CBProApi.credentials?.apiKey?.let { apiKey ->
                if (verificationStatus.isVerified) {
                    prefs.approveApiKey(apiKey)
                } else {
                    prefs.rejectApiKey(apiKey)
                }
            }
            verifyActivity.verificationComplete(verificationStatus)
        }
    }

    override fun onResume() {
        updateViews()
        super.onResume()
    }


    fun updateViews() {
        val account = Account.forCurrency(currency, Exchange.CBPro)
        sendInfoText.text = if (account?.balance ?: 0.0 > 0.0) {
            resources.getString(R.string.verify_explanation_message, currency.fullName)
        } else {
            resources.getString(R.string.verify_explanation_message_no_risk)
        }
        progressBar.visibility = View.INVISIBLE
    }
}