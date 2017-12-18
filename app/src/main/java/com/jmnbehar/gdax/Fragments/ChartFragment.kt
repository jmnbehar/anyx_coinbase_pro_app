package com.jmnbehar.gdax.Fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jmnbehar.gdax.R

/**
 * Created by jmnbehar on 11/5/2017.
 */
class ChartFragment : Fragment() {


    companion object {
        fun newInstance(): Fragment {
            return ChartFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_chart, container, false)
    }
}
