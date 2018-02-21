package com.jmnbehar.gdax.Classes

import android.graphics.Color
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.fragment_chart.view.*
import org.jetbrains.anko.backgroundColor
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
        showDarkMode()
    }

    fun showDarkMode(newView: View? = null) {
        val backgroundView = newView ?: view
        val prefs = Prefs(context)
        if (prefs.isDarkModeOn) {
            backgroundView?.backgroundColor = Color.TRANSPARENT
            activity.setTheme(R.style.AppThemeDark)
        } else {
            backgroundView?.backgroundColor = Color.WHITE
            activity.setTheme(R.style.AppThemeLight)
        }
    }

    fun setupSwipeRefresh(rootView: View) {
        swipeRefreshLayout = rootView.swipe_refresh_layout
        swipeRefreshLayout?.onRefresh {
            refresh { endRefresh() }
        }
    }

    open fun refresh(onComplete: () -> Unit) {
        onComplete()
    }

    fun endRefresh() {
        swipeRefreshLayout?.isRefreshing = false
    }
}