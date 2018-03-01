package com.jmnbehar.anyx.Fragments.Login

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.jmnbehar.anyx.Adapters.HistoryListViewAdapter
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.list_view.view.*

/**
 * Created by jmnbehar on 11/5/2017.
 */
class LoginHelpFragment() : Fragment() {
    var isMobileHelpPage = true

    fun newInstance(isMobileHelpPage: Boolean) : LoginHelpFragment {
        this.isMobileHelpPage = isMobileHelpPage
        return this
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return if (isMobileHelpPage) {
            inflater!!.inflate(R.layout.fragment_login_help_mobile, container, false)
        } else {
            inflater!!.inflate(R.layout.fragment_login_help_desktop, container, false)
        }
    }
}
