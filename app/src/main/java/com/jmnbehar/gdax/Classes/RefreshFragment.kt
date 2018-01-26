package com.jmnbehar.gdax.Classes

import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import kotlinx.android.synthetic.main.fragment_chart.view.*
import org.jetbrains.anko.support.v4.onRefresh

/**
 * Created by jmnbehar on 1/15/2018.
 */

open class RefreshFragment: Fragment() {
    val handler = Handler()
    var autoRefresh: Runnable? = null
    var swipeRefreshLayout: SwipeRefreshLayout? = null


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