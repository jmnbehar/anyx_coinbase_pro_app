package com.jmnbehar.anyx.Fragments.Verify

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_verify_email.view.*
import android.text.Editable
import android.text.TextWatcher
import com.jmnbehar.anyx.Activities.VerifyActivity


/**
 * Created by josephbehar on 1/20/18.
 */

class VerififyEmailFragment : Fragment() {
    companion object {
        fun newInstance(): VerififyEmailFragment
        {
            return VerififyEmailFragment()
        }
    }

    private lateinit var emailEditText: EditText
    private lateinit var emailConfirmEditText: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_verify_email, container, false)

        emailEditText = rootView.etxt_verify_email
        emailConfirmEditText = rootView.etxt_verify_email_confirm

        val emailTextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (activity is VerifyActivity){
                    val email = emailEditText.text.toString()
                    val emailConfirm = emailConfirmEditText.text.toString()
                    if (email == emailConfirm) {
                        (activity as VerifyActivity).confirmEmail(email)
                    } else {
                        (activity as VerifyActivity).blankEmail()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        }
        emailEditText.addTextChangedListener(emailTextWatcher)
        emailConfirmEditText.addTextChangedListener(emailTextWatcher)
        return rootView
    }
}