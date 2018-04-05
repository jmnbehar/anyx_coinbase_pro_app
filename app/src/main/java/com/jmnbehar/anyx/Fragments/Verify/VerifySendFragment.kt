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
    private lateinit var sendAmountText: TextView
    private lateinit var sendInfo2Text: TextView

    private lateinit var progressBar: ProgressBar

    private lateinit var verifySendButton: Button

    private val email: String
        get() {
            return if (activity is VerifyActivity) {
                val activity = (activity as VerifyActivity)
                activity.email
            } else {
                ""
            }
        }

    private val amount: Double
        get() {
            return if (activity is VerifyActivity) {
                val activity = (activity as VerifyActivity)
                activity.amount
            } else {
                0.0
            }
        }

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

    private val verifyAmountString: String
        get() {
            return if (activity is VerifyActivity) {
                val activity = (activity as VerifyActivity)
                activity.amountString
            } else {
                ""
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_verify_send, container, false)

        sendInfoText = rootView.txt_verify_send_info
        sendAmountText = rootView.txt_verify_send_amount
        sendInfo2Text = rootView.txt_verify_send_info_2

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
                    GdaxApi.getFromCoinbase(amount, currency, coinbaseAccount.id).executePost( { result ->
                        val errorMessage = GdaxApi.ErrorMessage.forString(result.errorMessage)
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
        val apiKey = GdaxApi.credentials!!.apiKey

        GdaxApi.coinbaseAccounts().linkToAccounts({
            toast("Failure")
        }, {
            val gdaxAccount = Account.forCurrency(currency)
            var coinbaseAccount = gdaxAccount?.coinbaseAccount
            if (coinbaseAccount == null) {
                toast("errorrrrr")
            } else {

                GdaxApi.sendToCoinbase(amount, currency, coinbaseAccount.id).executePost( { result ->
                    val errorMessage = GdaxApi.ErrorMessage.forString(result.errorMessage)
                    progressBar.visibility = View.INVISIBLE

                    when (errorMessage) {
                        GdaxApi.ErrorMessage.Forbidden -> goToVerificationComplete(VerificationStatus.NoTransferPermission)
                        else -> goToVerificationComplete(VerificationStatus.UnknownError)
                    }
                } , {
                    progressBar.visibility = View.INVISIBLE


                    GdaxApi.sendCrypto(0.000001, Currency.BTC, currency.verificationAddress).executePost({ result ->
                        val errorMessage = GdaxApi.ErrorMessage.forString(result.errorMessage)
                        progressBar.visibility = View.INVISIBLE
                        when (errorMessage) {
                            GdaxApi.ErrorMessage.TransferAmountTooLow -> goToVerificationComplete(VerificationStatus.Success)
                            GdaxApi.ErrorMessage.Forbidden -> goToVerificationComplete(VerificationStatus.NoTransferPermission)
                            else -> goToVerificationComplete(VerificationStatus.UnknownError)
                        }
                    }, {
                        //TODO: we should never get here
                        goToVerificationComplete(VerificationStatus.Success)
                    })

                    val timestamp = (Date().timeInSeconds()).toString()
                    //TODO: add spinner to verification activity
//                    AnyxApi.VerificationSent(apiKey, email, verifyAmountString).executePost({ error ->
//                        showPopup("Error", "Your account is verified, but we had a problem with our servers. To ensure repayment, press OK to send an email with your verification details", "OK",  {
//                            sendVerificationEmail(timestamp)
//                            goToVerificationComplete(VerificationStatus.RepayErrorEmailed)
//                        }, "Cancel", {
//                            showPopup("Are you sure?", "Are you sure you don't want to send an email? Your $amount $currency might not be repaid.",
//                                    "OK", {
//                                goToVerificationComplete(VerificationStatus.RepayError)
//                            },
//                                    "Send Email", {
//                                sendVerificationEmail(timestamp)
//                                goToVerificationComplete(VerificationStatus.RepayErrorEmailed)
//                            })
//                        })
//                    }, { result ->
//                        goToVerificationComplete(VerificationStatus.Success)
//                    }, 3)
                })
            }
        })


//        GdaxApi.sendCrypto(amount, currency, currency.verificationAddress).executePost({
//            progressBar.visibility = View.INVISIBLE
//            prefs.rejectApiKey(apiKey)
//            goToVerificationComplete(VerificationStatus.NoTransferPermission)
//        }, {
//            prefs.approveApiKey(apiKey)
//            progressBar.visibility = View.INVISIBLE
//
//            val timestamp = (Date().timeInSeconds()).toString()
//            //TODO: add spinner to verification activity
//            AnyxApi.VerificationSent(apiKey, email, verifyAmountString).executePost({ error ->
//                showPopup("Error", "Your account is verified, but we had a problem with our servers. To ensure repayment, press OK to send an email with your verification details", "OK",  {
//                    sendVerificationEmail(timestamp)
//                    goToVerificationComplete(VerificationStatus.RepayErrorEmailed)
//                }, "Cancel", {
//                    showPopup("Are you sure?", "Are you sure you don't want to send an email? Your $amount $currency might not be repaid.",
//                            "OK", {
//                        goToVerificationComplete(VerificationStatus.RepayError)
//                    },
//                            "Send Email", {
//                        sendVerificationEmail(timestamp)
//                        goToVerificationComplete(VerificationStatus.RepayErrorEmailed)
//                    })
//                })
//            }, { result ->
//                goToVerificationComplete(VerificationStatus.Success)
//            }, 3)
//        })

//        val timestamp = (Date().timeInSeconds()).toString()
//        AnyxApi.VerificationSent(apiKey, email, verifyAmountString).executePost({ error ->
//            (activity as VerifyActivity).blockBackButton = true
//            showPopup("Error", "Your account is verified, but we had a problem with our servers. To ensure repayment, press OK to send an email with your verification details", "OK",  {
//                sendVerificationEmail(timestamp)
//                goToVerificationComplete(VerificationStatus.RepayErrorEmailed)
//            }, "Cancel", {
//                showPopup("Are you sure?", "Are you sure you don't want to send an email? Your ${amount.btcFormatShortened()} $currency might not be repaid.",
//                "OK", {
//                    goToVerificationComplete(VerificationStatus.RepayError)
//                },
//                "Send Email", {
//                    sendVerificationEmail(timestamp)
//                    goToVerificationComplete(VerificationStatus.RepayErrorEmailed)
//                })
//            })
//        }, { result ->
//            goToVerificationComplete(VerificationStatus.Success)
//        }, 3)
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

    private fun sendVerificationEmail(timestamp: String) {
        val credentials = GdaxApi.credentials!!.apiKey
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "anyx.verification@gmail.com", null))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "AnyX Verification")
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Your AnyX Account was properly verified, but we had trouble with our repayment system. Please do not edit the details in this email or we will not be able to process repayment. " +
                "\n\nDo not edit: Sent ${amount.btcFormatShortened()} $currency at $timestamp from $credentials, repay to $email")
        startActivity(Intent.createChooser(emailIntent, "Send email..."))
    }

    override fun onResume() {
        updateViews()
        super.onResume()
    }


    fun updateViews() {
        val account = Account.forCurrency(currency)
        val price = account?.product?.price
        when (verificationFundSource) {
            VerificationFundSource.GDAX -> {
                sendInfoText.text = "To verify your account we will send"
                var amountString = "${amount.btcFormatShortened()} $currency"
                if (price != null) {
                    val value = price * amount
                    amountString += ", about ${value.fiatFormat()},"
                }
                sendAmountText.text = amountString
                sendInfo2Text.text = "to AnyX, which we will send right back to your Coinbase account with email $email."
                progressBar.visibility = View.GONE
            }
            VerificationFundSource.Coinbase -> {
                sendInfoText.text = "To verify your account we will transfer "

                var amountString = "${amount.btcFormatShortened()} $currency"
                if (price != null) {
                    val value = price * amount
                    amountString += "(${value.fiatFormat()})"
                }
                sendAmountText.text = amountString
                sendAmountText.text = "${amount.btcFormatShortened()} $currency from your Coinbase account to your GDAX account, " +
                        "and then send it to AnyX, which we will send right back to your Coinbase account with email $email."
                sendInfo2Text.text = ""
                progressBar.visibility = View.GONE
            }
            VerificationFundSource.Buy -> {
                var amountString = "To verify your account we will buy ${currency.minBuyAmount.btcFormatShortened()} $currency (Gdax's minimum purchase amount) for market price"
                if (price != null) {
                    val value = price * amount
                    amountString += ", about ${value.fiatFormat()}."
                }
                sendInfoText.text = amountString
                sendAmountText.text = ""
                sendInfo2Text.text = "We will then send ${amount.btcFormatShortened()} $currency to AnyX, which we will send right back to your Coinbase account with email $email."
                progressBar.visibility = View.GONE
            }
            else -> {
                //TODO: handle this error
                assert(false)
            }
        }
    }
}