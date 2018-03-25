package com.jmnbehar.anyx.Fragments.Verify

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.jmnbehar.anyx.Activities.VerifyActivity
import com.jmnbehar.anyx.Classes.Currency
import com.jmnbehar.anyx.Classes.VerificationStatus
import com.jmnbehar.anyx.Classes.btcFormatShortened
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_verify_complete.view.*

/**
 * Created by josephbehar on 1/20/18.
 */

class VerifyCompleteFragment : Fragment() {
    companion object {
        fun newInstance(): VerifyCompleteFragment
        {
            return VerifyCompleteFragment()
        }
    }

    private lateinit var statusText: TextView
    private lateinit var infoText: TextView
    private lateinit var statusImageView: ImageView

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_verify_complete, container, false)

        statusText = rootView.txt_verify_complete_status
        infoText = rootView.txt_verify_complete_info
        statusImageView = rootView.img_verify_complete_status

        return rootView
    }

    override fun onResume() {
        super.onResume()
        if (activity is VerifyActivity) {
            val verifyStatus = (activity as VerifyActivity).verifyStatus
            if (verifyStatus != null) {
                updateText(verifyStatus)
            }
        }
    }

    fun updateText(verifyStatus: VerificationStatus) {
        if (verifyStatus.isVerified) {
            statusText.text = "Success!"
            statusImageView.setImageResource(R.drawable.success_icon)
        } else {
            statusText.text = "Bummer"
            statusImageView.setImageResource(R.drawable.fail_icon)
        }
        infoText.text = when (verifyStatus) {
            VerificationStatus.Success -> "\n\nYour account is verified, and your ${amount.btcFormatShortened()} $currency will be returned to your Coinbase Account with email $email within two days."
            VerificationStatus.RepayErrorEmailed -> "\n\nYour account is verified, we will review the email you sent us and your ${amount.btcFormatShortened()} $currency will be returned to your Coinbase Account with email $email."
            VerificationStatus.RepayError -> "Your account is verified, but there was a problem with our servers so you may not be automatically repaid. " +
                    "\n\nIf you don't receive your ${amount.btcFormatShortened()} $currency in your Coinbase Account within two days, please reach out to our verification support at anyx.verify@gmail.com"
            VerificationStatus.NoTransferPermission -> missingPermissionString("Transfer")
            VerificationStatus.NoTradePermission -> missingPermissionString("Trade")
            VerificationStatus.NoTwoFactorPermission -> missingPermissionString("Bypass Two-Factor Auth")
            VerificationStatus.NoViewPermission -> missingPermissionString("View")
            VerificationStatus.NoPaymentMethods -> "Could not verify at this time because your account is unfunded and has no payment methods. \n\nPlease add funds or a payment method to verify your account."
            VerificationStatus.GdaxError -> missingPermissionString("Transfer")
        }
    }

    fun missingPermissionString(permission: String) : String {
        return "Your account could not be verified because your API Key does not have the \"$permission\" permission. " +
                "\n\nPlease create a new API Key with View, Transfer, Bypass Two-Factor Auth, and Trade permissions."
    }


}