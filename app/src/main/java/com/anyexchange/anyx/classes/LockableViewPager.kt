package com.anyexchange.anyx.classes

/**
 * Created by anyexchange on 2/18/2018.
 */

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

class LockableViewPager : ViewPager {
    var isLocked = false

    constructor(context: Context) : super(context) {
        setMyScroller()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setMyScroller()
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        // Never allow swiping to switch between pages
        return if (isLocked) {
            false
        } else {
            super.onInterceptTouchEvent(event)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Never allow swiping to switch between pages
        return if (isLocked) {
            false
        } else {
            super.onTouchEvent(event)
        }
    }

    //down one is added for smooth scrolling

    private fun setMyScroller() {
//        try {
//            val viewpager = ViewPager::class.java
//            val scroller = viewpager.getDeclaredField("mScroller")
//            scroller.isAccessible = true
//            scroller.set(this, MyScroller(context))
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
    }

//    inner class MyScroller(context: Context) : Scroller(context, DecelerateInterpolator()) {
//        override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
//            super.startScroll(startX, startY, dx, dy, 350 /*1 secs*/)
//        }
//    }
}