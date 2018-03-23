package com.jmnbehar.anyx.Fragments.Verify

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.jmnbehar.anyx.Activities.VerifyActivity
import com.jmnbehar.anyx.Classes.VerificationStatus
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
        infoText.text = verifyStatus.toString()

    }
}