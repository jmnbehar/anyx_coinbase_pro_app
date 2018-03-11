package com.jmnbehar.anyx.Fragments.Verify

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jmnbehar.anyx.R

/**
 * Created by josephbehar on 1/20/18.
 */

class VerififyIntroFragment : Fragment() {
    companion object {
        fun newInstance(): VerififyIntroFragment
        {
            return VerififyIntroFragment()
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_verify_intro, container, false)

        return rootView
    }
}