package com.jmnbehar.anyx.Fragments.Main

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_settings.view.*

/**
 * Created by josephbehar on 1/20/18.
 */

class VerififyCompleteFragment : Fragment() {
    companion object {
        fun newInstance(): VerififyCompleteFragment
        {
            return VerififyCompleteFragment()
        }
    }

    private lateinit var titleText: TextView
    private lateinit var logoutButton: Button
    private lateinit var disclaimerButton: Button
    private lateinit var darkModeCheckBox: CheckBox
    private lateinit var showTradeConfirmCheckBox: CheckBox
    private lateinit var showSendConfirmCheckBox: CheckBox

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_settings, container, false)


        return rootView
    }
}