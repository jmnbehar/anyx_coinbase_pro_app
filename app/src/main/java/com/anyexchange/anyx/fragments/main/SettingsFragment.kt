package com.anyexchange.anyx.fragments.main

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.activities.VerifyActivity
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.activities.MainActivity
import com.anyexchange.anyx.adapters.spinnerAdapters.FloatSpinnerAdapter
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.android.synthetic.main.fragment_settings.view.*
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
    private lateinit var emailDevButton: Button
    private lateinit var darkModeCheckBox: CheckBox
    private lateinit var showTradeConfirmCheckBox: CheckBox
    private lateinit var showSendConfirmCheckBox: CheckBox
    private lateinit var quickChangeThresholdSpinner: Spinner


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_settings, container, false)

        titleText = rootView.txt_setting_title
        logoutButton = rootView.btn_setting_log_out
        verifyButton = rootView.btn_setting_verify_account
        cbproEulaButton = rootView.btn_setting_show_cbpro_eula
        anyxEulaButton = rootView.btn_setting_show_anyx_eula
        emailDevButton = rootView.btn_setting_email_dev
        darkModeCheckBox = rootView.cb_setting_dark_mode
        showTradeConfirmCheckBox = rootView.cb_setting_show_trade_confirm
        showSendConfirmCheckBox = rootView.cb_setting_show_send_confirm
        quickChangeThresholdSpinner = rootView.spinner_settings_quick_change_threshold

        showDarkMode(rootView)

        val prefs = Prefs(activity!!)

        logoutButton.setOnClickListener  {

            prefs.isLoggedIn = false
            CBProApi.credentials = null
            prefs.stashOrders(null)
            prefs.nukeStashedFills()
            (activity as? MainActivity)?.goToFragment(FragmentType.LOGIN)
        }

        verifyButton.setOnClickListener  {
            val intent = Intent(activity, VerifyActivity::class.java)
            startActivity(intent)
        }

        cbproEulaButton.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coinbase.com/legal/user_agreement"))
            startActivity(browserIntent)
        }

        emailDevButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:anyx.app@gmail.com")
            intent.putExtra(Intent.EXTRA_SUBJECT, "AnyX Feedback")

            context?.packageManager?.let { packageManager ->
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
            }
        }

        anyxEulaButton.visibility = View.GONE
        (activity as? MainActivity)?.let { activity ->
            anyxEulaButton.visibility = View.VISIBLE
            anyxEulaButton.setOnClickListener {
                activity.goToFragment(FragmentType.EULA)
            }
        }

        context?.let {
            val tempThreshold = prefs.quickChangeThreshold
            quickChangeThresholdSpinner.visibility = View.VISIBLE
            val floatList = listOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
            val arrayAdapter = FloatSpinnerAdapter(it, floatList)
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            quickChangeThresholdSpinner.adapter = arrayAdapter
            quickChangeThresholdSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                    prefs.quickChangeThreshold = floatList[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
            val index = floatList.indexOf(tempThreshold)
            if (index != -1) {
                quickChangeThresholdSpinner.setSelection(index)
            }
            quickChangeThresholdSpinner.visibility = View.VISIBLE
        } ?: run {
            quickChangeThresholdSpinner.visibility = View.GONE
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
        dismissProgressSpinner()
        val prefs = Prefs(activity!!)
        val apiKey = CBProApi.credentials?.apiKey

        when {
            apiKey == null -> verifyButton.visibility = View.GONE
            prefs.isApiKeyValid(apiKey) == true -> {
                verifyButton.visibility = View.GONE
                txt_setting_verify_account.visibility = View.GONE
            }
            else -> {
                verifyButton.visibility = View.VISIBLE
                txt_setting_verify_account.visibility = View.VISIBLE
            }
        }

        if (prefs.isLoggedIn) {
            logoutButton.text = resources.getString(R.string.settings_log_out_btn)
        } else {
            logoutButton.text = resources.getString(R.string.settings_log_in_btn)
        }
    }
}