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
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Patterns
import com.jmnbehar.anyx.Activities.VerifyActivity


/**
 * Created by josephbehar on 1/20/18.
 */

class VerifyEmailFragment : Fragment() {
    companion object {
        fun newInstance(): VerifyEmailFragment
        {
            return VerifyEmailFragment()
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
                    val activity = (activity as VerifyActivity)
                    val email = emailEditText.text.toString()
                    val emailConfirm = emailConfirmEditText.text.toString()

                    val isValidEmail = (!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches())
                    if (!isValidEmail) {
                        activity.blankEmail("Email is not valid")
                    } else if (email != emailConfirm) {
                        activity.blankEmail("Emails don't match")
                    } else {
                        activity.confirmEmail(email)
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