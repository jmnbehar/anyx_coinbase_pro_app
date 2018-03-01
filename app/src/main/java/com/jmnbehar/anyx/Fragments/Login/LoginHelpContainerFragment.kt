package com.jmnbehar.anyx.Fragments.Login

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.jmnbehar.anyx.Activities.LoginHelpActivity
import com.jmnbehar.anyx.Classes.Constants
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_login_help_container.view.*
import org.jetbrains.anko.sdk25.coroutines.onClick

/**
 * Created by jmnbehar on 11/5/2017.
 */
class LoginHelpContainerFragment() : Fragment() {
    private lateinit var mobileBtn: Button
    private lateinit var computerBtn: Button

    companion object {
        fun newInstance() : LoginHelpContainerFragment {
            return LoginHelpContainerFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView =  inflater!!.inflate(R.layout.fragment_login_help_container, container, false)

        mobileBtn = rootView.btn_login_help_mobile
        computerBtn = rootView.btn_login_help_desktop

        val intent = Intent(activity, LoginHelpActivity::class.java)
        val thisFragment = this
        mobileBtn.onClick {
            activity.supportFragmentManager.beginTransaction().remove(thisFragment).commit()
//            intent.putExtra(Constants.isMobileLoginHelp, true)
//            startActivity(intent)
        }
        computerBtn.onClick {
            //            activity.supportFragmentManager.beginTransaction().remove(thisFragment).commit()
            activity.onBackPressed()
//            intent.putExtra(Constants.isMobileLoginHelp, false)
//            startActivity(intent)
        }


        return rootView
    }

}
