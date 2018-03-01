package com.jmnbehar.anyx.Fragments.Login

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jmnbehar.anyx.Adapters.LoginHelpPagerAdapter
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_login_help_container.view.*

/**
 * Created by jmnbehar on 11/5/2017.
 */
class LoginHelpContainerFragment() : Fragment() {
    private lateinit var helpPager: ViewPager

    companion object {
        fun newInstance() : LoginHelpContainerFragment {
            return LoginHelpContainerFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView =  inflater!!.inflate(R.layout.fragment_login_help_container, container, false)

        helpPager = rootView.login_help_view_pager
        helpPager.adapter = LoginHelpPagerAdapter(childFragmentManager)
        //helpPager.setOnTouchListener(this)

        return rootView
    }
}
