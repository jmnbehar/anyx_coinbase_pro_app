package com.jmnbehar.anyx.Fragments.Main

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.jmnbehar.anyx.Activities.LoginActivity
import com.jmnbehar.anyx.Classes.Constants
import com.jmnbehar.anyx.Classes.Prefs
import com.jmnbehar.anyx.Classes.RefreshFragment
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_settings.view.*
import org.jetbrains.anko.textColor

/**
 * Created by josephbehar on 1/20/18.
 */

class VerificationFragment : RefreshFragment() {
    companion object {
        fun newInstance(): VerificationFragment
        {
            return VerificationFragment()
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

        titleText = rootView.txt_setting_title
        logoutButton = rootView.btn_setting_log_out
        disclaimerButton = rootView.btn_setting_show_disclaimer
        darkModeCheckBox = rootView.cb_setting_dark_mode
        showTradeConfirmCheckBox = rootView.cb_setting_show_trade_confirm
        showSendConfirmCheckBox = rootView.cb_setting_show_send_confirm

        return rootView
    }
}