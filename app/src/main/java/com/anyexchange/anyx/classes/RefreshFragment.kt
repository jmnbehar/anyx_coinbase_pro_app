package com.anyexchange.anyx.classes

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import android.widget.AdapterView
import com.anyexchange.anyx.adapters.NavigationSpinnerAdapter
import com.anyexchange.anyx.R
import com.anyexchange.anyx.activities.MainActivity
import kotlinx.android.synthetic.main.app_bar_main.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.onRefresh

/**
 * Created by anyexchange on 1/15/2018.
 */

open class RefreshFragment: Fragment() {
    val handler = Handler()
    var autoRefresh: Runnable? = null
    var swipeRefreshLayout: SwipeRefreshLayout? = null
    var skipNextRefresh: Boolean = false
    var lockPortrait = true

    val apiInitData: CBProApi.CBProApiInitData?
        get() {
            val context = context
            return if (activity is MainActivity) {
                (activity as MainActivity).apiInitData
            } else if (context != null){
                CBProApi.CBProApiInitData(context) { /* do nothing */ }
            } else {
                null
            }
        }

    companion object {
        const val ARG_OBJECT = "object"
    }

    override fun onResume() {
        super.onResume()
        activity?.requestedOrientation = if (lockPortrait) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }
        if (!skipNextRefresh) {
            refresh {
                endRefresh()
            }
        }
        skipNextRefresh = false
        showDarkMode()

        System.out.println("Removing spinner: ")
        if (activity is com.anyexchange.anyx.activities.MainActivity) {
            (activity as com.anyexchange.anyx.activities.MainActivity).spinnerNav.background.setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            (activity as com.anyexchange.anyx.activities.MainActivity).spinnerNav.visibility = View.GONE
            (activity as com.anyexchange.anyx.activities.MainActivity).toolbar.title = resources.getString(R.string.app_name)
        }
    }

    fun showNavSpinner(defaultSelection: Currency?, currencyList: List<Currency>, onItemSelected: (currency: Currency) -> Unit) {
        (activity as? MainActivity)?.let { mainActivity ->
            val spinnerNavAdapter = NavigationSpinnerAdapter(mainActivity, R.layout.list_row_coinbase_account, currencyList)
            spinnerNavAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mainActivity.spinnerNav.adapter = spinnerNavAdapter

            mainActivity.toolbar.title = ""
            mainActivity.spinnerNav.background.colorFilter = mainActivity.defaultSpinnerColorFilter
            mainActivity.spinnerNav.isEnabled = true
            mainActivity.spinnerNav.visibility = View.VISIBLE
            mainActivity.spinnerNav.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (parent?.getItemAtPosition(position) is Currency) {
                        val selectedItem = parent.getItemAtPosition(position) as Currency
                        onItemSelected(selectedItem)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
            if (defaultSelection != null) {
                val spinnerList = (mainActivity.spinnerNav.adapter as NavigationSpinnerAdapter).currencyList
                val currentIndex = spinnerList.indexOf(defaultSelection)
                mainActivity.spinnerNav.setSelection(currentIndex)
            }
        }
    }

    fun dismissProgressSpinner() {
        if (activity is com.anyexchange.anyx.activities.MainActivity) {
            (activity as com.anyexchange.anyx.activities.MainActivity).dismissProgressBar()
        }
    }

    fun showProgressSpinner() {
        if (activity is com.anyexchange.anyx.activities.MainActivity) {
            (activity as com.anyexchange.anyx.activities.MainActivity).showProgressBar()
        }
    }


    fun showPopup(stringTextResource: Int, positiveAction: () -> Unit = {}, negativeText: String? = null, negativeAction: () -> Unit = {}) {
        val string = resources.getString(stringTextResource)
        showPopup(string, positiveAction, negativeText, negativeAction)
    }
    fun showPopup(string: String, positiveAction: () -> Unit = {}, negativeText: String? = null, negativeAction: () -> Unit = {}) {
        alert {
            title = string
            positiveButton(R.string.popup_ok_btn) { positiveAction() }
            if (negativeText != null) {
                negativeButton(negativeText) { negativeAction() }
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

    fun setupSwipeRefresh(swipeRefreshLayout: SwipeRefreshLayout) {
        this.swipeRefreshLayout = swipeRefreshLayout
        this.swipeRefreshLayout?.onRefresh {
            refresh { endRefresh() }
        }
    }


    open fun refresh(onComplete: (Boolean) -> Unit) {   //The boolean indicates whether or not refresh was successful
        skipNextRefresh = false
        onComplete(true)
    }

    private fun endRefresh() {
        swipeRefreshLayout?.isRefreshing = false
    }
}