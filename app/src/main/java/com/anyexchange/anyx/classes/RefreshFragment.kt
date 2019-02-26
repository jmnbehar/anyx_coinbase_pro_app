package com.anyexchange.anyx.classes

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.anyexchange.anyx.adapters.spinnerAdapters.NavigationSpinnerAdapter
import com.anyexchange.anyx.R
import com.anyexchange.anyx.activities.MainActivity
import com.anyexchange.anyx.api.ApiInitData
import com.anyexchange.anyx.views.searchableSpinner.OnItemSelectedListener
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

    var shouldHideSpinner = true

    val apiInitData: ApiInitData?
        get() {
            val activity = activity
            return when (activity) {
                is MainActivity -> activity.apiInitData
                null -> null
                else -> ApiInitData(activity) { /* do nothing */ }
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

        skipNextRefresh = false
        showDarkMode()

        if (shouldHideSpinner) {
            (activity as? MainActivity)?.let { mainActivity ->
//                mainActivity.navSpinner.background.setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                mainActivity.navSpinner.visibility = View.GONE
                mainActivity.toolbar.title = resources.getString(R.string.app_name)
            }
        }
    }


     fun setOptionsMenuTextColor(menu: Menu?) {
         menu?.let {
             for (i in 0..(menu.size() - 1)) {
                 val item = menu.getItem(i)
                 val spanString = SpannableString(item.title.toString())
                 spanString.setSpan(ForegroundColorSpan(Color.BLACK),0, spanString.length, 0)
                 item.title = spanString
             }
         }
    }

    fun showNavSpinner(defaultSelection: Currency?, currencyList: List<Currency>, onItemSelected: (currency: Currency) -> Unit) {
        shouldHideSpinner = false
        (activity as? MainActivity)?.let { mainActivity ->
            val sortedList = currencyList.sortCurrenciesAlphabetical()
            val spinnerNavAdapter = NavigationSpinnerAdapter(mainActivity, R.layout.list_row_spinner_nav, R.id.txt_currency, sortedList)

            mainActivity.navSpinner.setAdapter(spinnerNavAdapter)

            mainActivity.toolbar.title = ""
//            mainActivity.navSpinner.background.colorFilter = mainActivity.defaultSpinnerColorFilter
            mainActivity.navSpinner.isEnabled = true
            mainActivity.navSpinner.visibility = View.VISIBLE

            val spinnerNavItemSelectedListener: OnItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(view: View, position: Int, id: Long) {
                    (mainActivity.navSpinner.selectedItem as? Currency)?.let {
                        onItemSelected(it)
                    }
//                    if (parent?.getItemAtPosition(position) is Currency) {
//                        val selectedItem = parent.getItemAtPosition(position) as Currency
//                        onItemSelected(selectedItem)
//                    }
                }

                override fun onNothingSelected() { }
            }
            mainActivity.navSpinner.setOnItemSelectedListener(spinnerNavItemSelectedListener)


            if (defaultSelection != null) {
                mainActivity.navSpinner.selectedItem = defaultSelection
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


    fun showPopup(titleTextResource: Int, messageTextResource: Int?, positiveAction: () -> Unit = {}, negativeText: String? = null, negativeAction: () -> Unit = {}) {
        val titleStr = resources.getString(titleTextResource)

        val messageStr = if (messageTextResource != null) {
            resources.getString(messageTextResource)
        } else { null }

        showPopup(titleStr, messageStr, positiveAction, negativeText, negativeAction)
    }
    fun showPopup(titleStr: String, messageStr: String?, positiveAction: () -> Unit = {}, negativeText: String? = null, negativeAction: () -> Unit = {}) {
        alert {
            title = titleStr
            if (messageStr != null) {
                message = messageStr
            }
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

    fun endRefresh() {
        swipeRefreshLayout?.isRefreshing = false
    }
}