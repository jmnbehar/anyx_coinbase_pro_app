package com.anyexchange.anyx.fragments.verify

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.anyexchange.anyx.activities.VerifyActivity
import com.anyexchange.anyx.classes.Currency
import com.anyexchange.anyx.classes.VerificationStatus
import com.anyexchange.anyx.R
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

        //TODO: use string resource
        infoText.text = when (verifyStatus) {
            VerificationStatus.Success -> "Woohoo!\nYour account is verified"
            VerificationStatus.RepayError -> "Due to an unknown error, you may have lost a very small quantity of $currency. Please reach out to our verification support at anyx.verify@gmail.com"
            VerificationStatus.NoTransferPermission -> missingPermissionString("Transfer")
            VerificationStatus.NoTradePermission -> missingPermissionString("Trade")
            VerificationStatus.NoViewPermission -> missingPermissionString("View")
            VerificationStatus.NoPaymentMethods -> "Could not verify at this time because your account is unfunded. \n\nPlease add funds and try again."
            VerificationStatus.CBProError -> "An unknown error occurred. Try again later."
            VerificationStatus.UnknownError -> "An unknown error occurred. Try again later."
        }
    }

    fun missingPermissionString(permission: String) : String {
        //TODO: use string resource
        return "Your account could not be verified because your API Key does not have the \"$permission\" permission. " +
                "\n\nPlease create a new API Key with View, Transfer, Bypass Two-Factor Auth, and Trade permissions."
    }


}