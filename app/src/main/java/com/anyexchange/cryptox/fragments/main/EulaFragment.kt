package com.anyexchange.cryptox.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import com.anyexchange.cryptox.R
import com.anyexchange.cryptox.classes.RefreshFragment
import kotlinx.android.synthetic.main.fragment_verify_intro.view.*


class EulaFragment : RefreshFragment() {
    private lateinit var eulaScrollView: ScrollView
    private lateinit var eulaLinearLayout: LinearLayout


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_verify_intro, container, false)

        eulaScrollView = rootView.scrollview_eula
        eulaLinearLayout = rootView.layout_verify_intro_eula

        rootView.txt_verify_intro_eula_header.visibility = View.GONE

        return rootView
    }
}