package com.jmnbehar.anyx.Classes

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import com.jmnbehar.anyx.Activities.MainActivity
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_chart.view.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.onRefresh

/**
 * Created by jmnbehar on 1/15/2018.
 */

open class RefreshFragment: Fragment() {
    val handler = Handler()
    var autoRefresh: Runnable? = null
    var swipeRefreshLayout: SwipeRefreshLayout? = null

    companion object {
        val ARG_OBJECT = "object"
    }

    override fun onResume() {
        super.onResume()
        swipeRefreshLayout?.isEnabled = false
        refresh {
            swipeRefreshLayout?.isEnabled = true
        }
        showDarkMode()
        if (activity is MainActivity) {
            (activity as MainActivity).spinnerNav.background.setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            (activity as MainActivity).spinnerNav.visibility = View.GONE
            (activity as MainActivity).toolbar.title = "AnyX"
        }
    }

    fun doneLoading() {
        if (activity is MainActivity) {
            (activity as MainActivity).dismissProgressBar()
        }
    }


    fun showPopup(string: String, positiveAction: () -> Unit, negativeText: String? = null, negativeAction: () -> Unit = {}) {
        alert {
            title = string
            positiveButton("OK") { positiveAction() }
            if (negativeText != null) {
                negativeButton(negativeText) { negativeAction }
            }
        }.show()
    }

    fun showDarkMode(newView: View? = null) {
        val backgroundView = newView ?: view
        val activity = activity
        if (activity != null) {
            val prefs = Prefs(activity)
            if (prefs.isDarkModeOn) {
                backgroundView?.backgroundColor = Color.TRANSPARENT
                activity.setTheme(R.style.AppThemeDark)
            } else {
                backgroundView?.backgroundColor = Color.WHITE
                activity.setTheme(R.style.AppThemeLight)
            }
        }
    }


    //TODO: use the other one
    fun setupSwipeRefresh(rootView: View) {
        swipeRefreshLayout = rootView.swipe_refresh_layout
        swipeRefreshLayout?.onRefresh {
            refresh { endRefresh() }
        }
    }

//    fun setupSwipeRefresh(swipeRefreshLayout: SwipeRefreshLayout) {
//        this.swipeRefreshLayout = swipeRefreshLayout
//        this.swipeRefreshLayout?.onRefresh {
//            refresh { endRefresh() }
//        }
//    }


    open fun refresh(onComplete: () -> Unit) {
        onComplete()
    }

    fun endRefresh() {
        swipeRefreshLayout?.isRefreshing = false
    }
}