package com.jmnbehar.anyx.Fragments.Verify

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.jmnbehar.anyx.Activities.VerifyActivity
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_verify_intro.view.*
import org.jetbrains.anko.sdk25.coroutines.onClick

/**
 * Created by josephbehar on 1/20/18.
 */

class VerifyIntroFragment : Fragment() {
    companion object {
        fun newInstance(): VerifyIntroFragment
        {
            return VerifyIntroFragment()
        }
    }

    private lateinit var acceptCheckBox: CheckBox
    private lateinit var gdaxEulaBtn: Button
    private lateinit var anyXEulaText: TextView


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_verify_intro, container, false)

        acceptCheckBox = rootView.cb_accept_eula
        anyXEulaText = rootView.txt_verify_intro_eula
        gdaxEulaBtn = rootView.btn_verify_intro_gdax_eula


        anyXEulaText.text = "Before we can verify your account, please read and accept our simple user agreement." +
                "\n\nAnyX will never save your Api Secret or Passphrase."

        gdaxEulaBtn.onClick {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coinbase.com/legal/user_agreement"))
            startActivity(browserIntent)
        }
        acceptCheckBox.setOnCheckedChangeListener { _, isChecked ->
            (activity as VerifyActivity).acceptEula(isChecked)
        }

        return rootView
    }
}