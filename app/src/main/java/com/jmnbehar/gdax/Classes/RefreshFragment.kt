package com.jmnbehar.gdax.Classes

import android.os.Handler
import android.support.v4.app.Fragment

/**
 * Created by jmnbehar on 1/15/2018.
 */

open class RefreshFragment: Fragment() {
    var refreshLocked = false
    val handler = Handler()
    var autoRefresh: Runnable? = null
    open fun refresh(onComplete: () -> Unit) {
        onComplete()
    }
}