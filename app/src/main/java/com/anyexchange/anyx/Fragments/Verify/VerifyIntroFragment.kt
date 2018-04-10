package com.anyexchange.anyx.Fragments.Verify

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import com.anyexchange.anyx.R
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

//        eulaScrollView.onScrollChange { v, scrollX, scrollY, oldScrollX, oldScrollY ->
//            println("bottom: " + v?.bottom)
//            println("height: " + v?.height)
//            println("scroll2:" + scrollY)
//            println("lin layout height: " + eulaLinearLayout)
//            if (v == null) {
//                (activity as VerifyActivity).acceptEula()
//            } else if (v.bottom == (v.height + scrollY)) {
//                (activity as VerifyActivity).acceptEula()
//            } else {
//                println("scrollin")
//            }
//        }

        return rootView
    }
}