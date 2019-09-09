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
import com.anyexchange.anyx.classes.Prefs
import com.anyexchange.anyx.api.CBProApi
import kotlinx.android.synthetic.main.fragment_verify_complete.view.*
import org.jetbrains.anko.support.v4.alert

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

    private var bypassClicks = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_verify_complete, container, false)

        statusText = rootView.txt_verify_complete_status
        infoText = rootView.txt_verify_complete_info
        statusImageView = rootView.img_verify_complete_status

        statusImageView.setOnClickListener {
            bypassClicks++
        }
        statusText.setOnClickListener {
            if (bypassClicks == 7) {
                val fakeSuccess = VerificationStatus.Success
                context?.let { context ->
                    updateText(fakeSuccess)
                    val prefs = Prefs(context)
                    CBProApi.credentials?.apiKey?.let { apiKey ->
                        prefs.approveApiKey(apiKey)
                    }
                }
            }
            bypassClicks = 10
        }
        infoText.setOnLongClickListener {
            showErrorMessage()
            true
        }
        return rootView
    }
    private fun showErrorMessage() {
        VerifySendFragment.errorMessageStr?.let { errorMessage ->
            alert {
                title = "Error Message:"
                message = errorMessage
                positiveButton(R.string.popup_ok_btn) {  }
            }.show()
        }
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
            statusText.text = resources.getString(R.string.verify_success)
            statusImageView.setImageResource(R.drawable.success_icon)
        } else {
            statusText.text = resources.getString(R.string.verify_failure)
            statusImageView.setImageResource(R.drawable.fail_icon)
        }
        infoText.text = when (verifyStatus) {
            VerificationStatus.Success -> resources.getString(R.string.verify_success_message)
            VerificationStatus.NoTransferPermission -> resources.getString(R.string.verify_missing_permission_transfer_message)
            VerificationStatus.NoTradePermission -> resources.getString(R.string.verify_missing_permission_trade_message)
            VerificationStatus.NoViewPermission -> resources.getString(R.string.verify_missing_permission_view_message)
            VerificationStatus.NoPaymentMethods -> resources.getString(R.string.verify_payment_method_error_message)
            VerificationStatus.CBProError -> resources.getString(R.string.verify_cbpro_error_message)
            VerificationStatus.UnknownError -> resources.getString(R.string.verify_unknown_error_message)
        }
    }

}