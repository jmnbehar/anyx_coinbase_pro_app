package com.jmnbehar.gdax.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jmnbehar.gdax.Activities.LoginActivity
import com.jmnbehar.gdax.Classes.Constants
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

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_settings, container, false)

        rootView.btn_setting_log_out.setOnClickListener  {
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(Constants.exit, true)
            startActivity(intent)
        }

        return rootView
    }
}