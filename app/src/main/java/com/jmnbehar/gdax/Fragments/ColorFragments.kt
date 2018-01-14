package com.jmnbehar.gdax.Fragments

import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jmnbehar.gdax.Classes.RefreshFragment
import com.jmnbehar.gdax.R

/**
 * Created by josephbehar on 12/26/17.
 */



class RedFragment : RefreshFragment() {
    companion object {
        fun newInstance(): RedFragment
        {
            return RedFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_red, container, false)
        rootView.setBackgroundColor(Color.RED)
        return rootView
    }
}
