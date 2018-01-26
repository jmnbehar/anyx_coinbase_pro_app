package com.jmnbehar.gdax.Classes

import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout

/**
 * Created by jmnbehar on 1/15/2018.
 */

open class RefreshFragment: Fragment() {
    val handler = Handler()
    var autoRefresh: Runnable? = null
    var swipeRefreshLayout: SwipeRefreshLayout? = null


    open fun refresh(onComplete: () -> Unit) {
        onComplete()
    }

    fun endRefresh() {
        swipeRefreshLayout?.isRefreshing = false
    }
}