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
import org.jetbrains.anko.support.v4.toast
import java.util.*


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

    private val verificationFundSource: VerificationFundSource?
        get() {
            return if (activity is VerifyActivity) {
                val activity = (activity as VerifyActivity)
                activity.verificationFundSource
            } else {
                null
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_verify_send, container, false)

        sendInfoText = rootView.txt_verify_send_info

        progressBar = rootView.progress_bar_verify_send
        verifySendButton = rootView.btn_verify_send

        //TODO: don't crash here
        val verifyAccount = Account.forCurrency(currency)!!
        verifySendButton.setOnClickListener  {
            progressBar.visibility = View.VISIBLE
            when (verificationFundSource!!) {
                VerificationFundSource.GDAX -> {
                    val productId = verifyAccount.product.id
                    GdaxApi.orderLimit(TradeSide.BUY, productId, 1.0, 1.0, timeInForce = GdaxApi.TimeInForce.ImmediateOrCancel, cancelAfter = null).executePost({ result ->
                        //TODO: check the error, check up on this
                        //TODO: handle if no payment methods are available
                        val errorMessage = GdaxApi.ErrorMessage.forString(result.errorMessage)
                        goToVerificationComplete(VerificationStatus.NoTradePermission)
                    }, { result ->
                        sendCryptoToVerify()
                    })
                }
                VerificationFundSource.Coinbase -> {
                    val coinbaseAccount = verifyAccount.coinbaseAccount!!
                    val amount = currency.minSendAmount //TODO: change to min CB send amount
                    GdaxApi.getFromCoinbase(amount, currency, coinbaseAccount.id).executePost( { result ->
                        val errorMessage = GdaxApi.ErrorMessage.forString(result.errorMessage)
                        when (errorMessage) {
                            GdaxApi.ErrorMessage.TransferAmountTooLow -> goToVerificationComplete(VerificationStatus.Success)
                            GdaxApi.ErrorMessage.Forbidden -> goToVerificationComplete(VerificationStatus.NoTransferPermission)
                            GdaxApi.ErrorMessage.InsufficientFunds -> goToVerificationComplete(VerificationStatus.UnknownError)//TODO: change this
                            GdaxApi.ErrorMessage.InvalidCryptoAddress -> goToVerificationComplete(VerificationStatus.UnknownError)
                            else -> goToVerificationComplete(VerificationStatus.UnknownError)
                        }//TODO: transfer funds back from coinbase
                        toast("Coinbase Error")
                    } , {
                        sendCryptoToVerify()
                    })
                }
                VerificationFundSource.Buy -> {
                    //TODO: don't crash here
                    val productId = verifyAccount.product.id
                    val buyAmount = currency.minBuyAmount
                    GdaxApi.orderMarket(TradeSide.BUY, productId, size = buyAmount, funds = null).executePost({ result ->
                        //TODO: check the error, check up on this
                        val errorMessage = GdaxApi.ErrorMessage.forString(result.errorMessage)
                        when (errorMessage) {
                            GdaxApi.ErrorMessage.BuyAmountTooSmallBtc,
                            GdaxApi.ErrorMessage.BuyAmountTooSmallEth,
                            GdaxApi.ErrorMessage.BuyAmountTooSmallBch,
                            GdaxApi.ErrorMessage.BuyAmountTooSmallLtc  -> assert(false)
                            GdaxApi.ErrorMessage.InsufficientFunds -> {
                                //TODO: distinguish between payment methods and insufficient funds
                                goToVerificationComplete(VerificationStatus.NoPaymentMethods)
                            }
                            else -> goToVerificationComplete(VerificationStatus.NoTradePermission)
                        }
                    }, {
                        sendCryptoToVerify()
                    })
                }
            }
        }

        updateViews()
        return rootView
    }

    private fun sendCryptoToVerify() {
        GdaxApi.coinbaseAccounts().linkToAccounts({
            toast("Unknown Error: Try again later")
        }, {
            val gdaxAccount = Account.forCurrency(currency)
            var coinbaseAccount = gdaxAccount?.coinbaseAccount
            if (coinbaseAccount == null) {
                toast("Unknown Error: Try again later")
            } else {

                val transferAmount = currency.minSendAmount
                GdaxApi.sendToCoinbase(transferAmount, currency, coinbaseAccount.id).executePost( { result ->
                    val errorMessage = GdaxApi.ErrorMessage.forString(result.errorMessage)
                    progressBar.visibility = View.INVISIBLE

                    when (errorMessage) {
                        GdaxApi.ErrorMessage.Forbidden -> goToVerificationComplete(VerificationStatus.NoTransferPermission)
                        else -> goToVerificationComplete(VerificationStatus.UnknownError)
                    }
                } , {
                    progressBar.visibility = View.INVISIBLE

                    val sendAmount = 0.000001
                    GdaxApi.sendCrypto(sendAmount, currency, currency.verificationAddress).executePost({ result ->
                        val errorMessage = GdaxApi.ErrorMessage.forString(result.errorMessage)
                        progressBar.visibility = View.INVISIBLE
                        when (errorMessage) {
                            GdaxApi.ErrorMessage.TransferAmountTooLow -> goToVerificationComplete(VerificationStatus.Success)
                            GdaxApi.ErrorMessage.Forbidden -> goToVerificationComplete(VerificationStatus.NoTransferPermission)
                            GdaxApi.ErrorMessage.InsufficientFunds -> goToVerificationComplete(VerificationStatus.UnknownError)//TODO: change this
                            GdaxApi.ErrorMessage.InvalidCryptoAddress -> goToVerificationComplete(VerificationStatus.UnknownError)
                            else -> goToVerificationComplete(VerificationStatus.UnknownError)
                        }
                        val returnAmount = transferAmount
                        GdaxApi.getFromCoinbase(returnAmount, currency, coinbaseAccount.id).executePost({},{})
                    }, {
                        //we should never get here
                        goToVerificationComplete(VerificationStatus.Success)
                        GdaxApi.getFromCoinbase(transferAmount, currency, coinbaseAccount.id).executePost({},{})
                    })
                    //TODO: add spinner to verification activity
                    //TODO: consider shooting off a request to the AnyX server
                })
            }
        })

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
        val price = account?.product?.price
        //TODO: overhaul this text
        val amount = currency.minSendAmount
        when (verificationFundSource) {
            VerificationFundSource.GDAX -> {
                sendInfoText.text = "To verify your account we will test to ensure that your API Key has all required permissions." +
                        "\n\nTo do this we will send a small amount of $currency to your own Coinbase account and then send it right back."
                var amountString = "${amount.btcFormatShortened()} $currency"
                if (price != null) {
                    val value = price * amount
                    amountString += ", about ${value.fiatFormat()},"
                }
                progressBar.visibility = View.INVISIBLE
            }
            VerificationFundSource.Coinbase -> {
                sendInfoText.text = "To verify your account we will test to ensure that your API Key has all required permissions." +
                        "\n\nTo do this we will send a small amount of $currency to your own Coinbase account and then send it right back."
                progressBar.visibility = View.INVISIBLE
            }
            VerificationFundSource.Buy -> {
                var amountString =  "To verify your account we will test to ensure that your API Key has all required permissions." +
                        "\n\nTo do this we will send a small amount of cryptocurrency to your own Coinbase account and then send it right back. " +
                        "\n\nBecause your account currently does not hold any crypto assets, we will buy ${currency.minBuyAmount.btcFormatShortened()} " +
                        "$currency (Gdax's minimum purchase amount) for market price"
                if (price != null) {
                    val value = price * amount
                    amountString += ", about ${value.fiatFormat()},"
                }
                amountString += " so we can send it to your Coinbase account and back."
                sendInfoText.text = amountString
                progressBar.visibility = View.INVISIBLE
            }
            else -> {
                goToVerificationComplete(VerificationStatus.UnknownError)
            }
        }
    }
}