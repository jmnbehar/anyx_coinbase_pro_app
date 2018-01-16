package com.jmnbehar.gdax.Classes

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView
import android.view.MotionEvent



/**
 * Created by jmnbehar on 1/15/2018.
 */

class LockableScrollView: ScrollView {
    constructor(ctx: Context) : super(ctx)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { }

    companion object {
        var scrollLocked = false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                if (scrollLocked) false else super.onTouchEvent(ev)
                // only continue to handle the touch event if scrolling enabled
            }
            else -> super.onTouchEvent(ev)
            //else -> return super.onTouchEvent(ev)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Don't do anything with intercepted touch events if not scrollable
        return if (scrollLocked) {
            false
        } else {
            super.onInterceptTouchEvent(ev)
        }
    }

}