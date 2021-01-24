package com.anyexchange.cryptox.fragments.verify

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import com.anyexchange.cryptox.R
import kotlinx.android.synthetic.main.fragment_verify_intro.view.*

typealias LinearLayout = Any

/**
 * Created by josephbehar on 1/20/18.
 */

class VerifyIntroFragment : Fragment() {
    companion object {
        fun newInstance(): VerifyIntroFragment
        {
            return VerifyIntroFragment()
        }
    }

    private lateinit var eulaScrollView: ScrollView
    private lateinit var eulaLinearLayout: LinearLayout


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_verify_intro, container, false)

        eulaScrollView = rootView.scrollview_eula
        eulaLinearLayout = rootView.layout_verify_intro_eula

        return rootView
    }
}