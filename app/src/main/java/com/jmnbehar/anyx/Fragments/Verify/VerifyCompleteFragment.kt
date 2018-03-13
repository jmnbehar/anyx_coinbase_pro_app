package com.jmnbehar.anyx.Fragments.Verify

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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

    fun updateText(isVerified: Boolean) {
        if (isVerified) {
            statusText.text = "Success!"
            statusImageView.setImageResource(R.drawable.success_icon)
            infoText.text = "Now that your account is verified, you will be able to buy, sell, and send cryptocurrencies right in this app!"
        } else {
            statusText.text = "Failure"
            statusImageView.setImageResource(R.drawable.fail_icon)
            infoText.text = "Your API Key is missing at least one required permission. " +
                    "\nPlease create a new GDAX API Key with View, Transfer, Bypass Two-Factor Auth, and Trade permissions and enter it into AnyX."

        }
    }
}