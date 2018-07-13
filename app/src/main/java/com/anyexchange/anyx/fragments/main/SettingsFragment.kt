package com.anyexchange.anyx.fragments.main

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.anyexchange.anyx.activities.VerifyActivity
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.activities.MainActivity
import kotlinx.android.synthetic.main.fragment_settings.view.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.textColor


/**
 * Created by josephbehar on 1/20/18.
 */

class SettingsFragment : RefreshFragment() {
    companion object {
        fun newInstance(): SettingsFragment
        {
            return SettingsFragment()
        }
    }

    private lateinit var titleText: TextView
    private lateinit var logoutButton: Button
    private lateinit var verifyButton: Button
    private lateinit var cbproEulaButton: Button
    private lateinit var anyxEulaButton: Button
    private lateinit var darkModeCheckBox: CheckBox
    private lateinit var showTradeConfirmCheckBox: CheckBox
    private lateinit var showSendConfirmCheckBox: CheckBox

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_settings, container, false)

        titleText = rootView.txt_setting_title
        logoutButton = rootView.btn_setting_log_out
        verifyButton = rootView.btn_setting_verify_account
        cbproEulaButton = rootView.btn_setting_show_cbpro_eula
        anyxEulaButton = rootView.btn_setting_show_anyx_eula
        darkModeCheckBox = rootView.cb_setting_dark_mode
        showTradeConfirmCheckBox = rootView.cb_setting_show_trade_confirm
        showSendConfirmCheckBox = rootView.cb_setting_show_send_confirm

        showDarkMode(rootView)

        val prefs = Prefs(activity!!)

        logoutButton.setOnClickListener  {
            val intent = Intent(activity, com.anyexchange.anyx.activities.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(Constants.logout, true)
            prefs.shouldAutologin = false
            prefs.isLoggedIn = false
            CBProApi.credentials = null
            prefs.stashOrders(null)
            prefs.stashFills(null)
            startActivity(intent)
            activity!!.finishAffinity()
        }

        verifyButton.setOnClickListener  {
            val intent = Intent(activity, VerifyActivity::class.java)
            startActivity(intent)
        }

        cbproEulaButton.onClick {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coinbase.com/legal/user_agreement"))
            startActivity(browserIntent)
        }

        anyxEulaButton.visibility = View.GONE
        activity?.let { activity ->
            anyxEulaButton.visibility = View.VISIBLE
            anyxEulaButton.onClick {
                (activity as? MainActivity)?.goToFragment(EulaFragment(), "EULA")
            }
        }

        showTradeConfirmCheckBox.isChecked = prefs.shouldShowTradeConfirmModal
        showTradeConfirmCheckBox.setOnCheckedChangeListener {  _, isChecked ->
            prefs.shouldShowTradeConfirmModal = isChecked
        }

        showSendConfirmCheckBox.isChecked = prefs.shouldShowSendConfirmModal
        showSendConfirmCheckBox.setOnCheckedChangeListener {  _, isChecked ->
            prefs.shouldShowSendConfirmModal = isChecked
        }

        darkModeCheckBox.isChecked = prefs.isDarkModeOn
        darkModeCheckBox.setOnCheckedChangeListener {  _, isChecked ->
            prefs.isDarkModeOn = isChecked
            showDarkMode()

            if (isChecked) {
                darkModeCheckBox.textColor = Color.WHITE
                showTradeConfirmCheckBox.textColor = Color.WHITE
                showSendConfirmCheckBox.textColor = Color.WHITE
                titleText.textColor = Color.WHITE
            } else {
                darkModeCheckBox.textColor = Color.BLACK
                showTradeConfirmCheckBox.textColor = Color.BLACK
                showSendConfirmCheckBox.textColor = Color.BLACK
                titleText.textColor = Color.BLACK
            }
        }
        showDarkMode()

        return rootView
    }

    override fun onResume() {
        super.onResume()
        val prefs = Prefs(activity!!)
        val apiKey = CBProApi.credentials?.apiKey
        if (apiKey == null) {
            verifyButton.visibility = View.GONE
        } else if (prefs.isApiKeyValid(apiKey) == true) {
            verifyButton.visibility = View.GONE
        } else {
            verifyButton.visibility = View.VISIBLE
        }

        if (prefs.isLoggedIn) {
            logoutButton.text = "Log Out"
        } else {
            logoutButton.text = "Log In"
        }

    }

}