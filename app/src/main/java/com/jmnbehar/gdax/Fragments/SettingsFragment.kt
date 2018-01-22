package com.jmnbehar.gdax.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import com.jmnbehar.gdax.Activities.LoginActivity
import com.jmnbehar.gdax.Classes.Constants
import com.jmnbehar.gdax.Classes.Prefs
import com.jmnbehar.gdax.Classes.RefreshFragment
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_settings.view.*

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

    private lateinit var logoutButton: Button
    private lateinit var disclaimerButton: Button
    private lateinit var darkModeCheckBox: CheckBox
    private lateinit var showConfirmCheckBox: CheckBox

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_settings, container, false)

        logoutButton = rootView.btn_setting_log_out
        disclaimerButton = rootView.btn_setting_show_disclaimer
        darkModeCheckBox = rootView.cb_setting_dark_mode
        showConfirmCheckBox = rootView.cb_setting_show_confirm

        val prefs = Prefs(activity)

        logoutButton.setOnClickListener  {
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(Constants.logout, true)
            prefs.shouldAutologin = false
            startActivity(intent)
            activity.finish()
        }

        showConfirmCheckBox.setOnCheckedChangeListener {  _, isChecked ->
            prefs.shouldShowConfirmModal = isChecked
        }

        return rootView
    }
}