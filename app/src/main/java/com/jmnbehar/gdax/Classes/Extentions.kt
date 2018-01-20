package com.jmnbehar.gdax.Classes

import android.widget.ListView
import android.view.ViewGroup
import android.view.View.MeasureSpec
import android.view.View


/**
 * Created by josephbehar on 12/28/17.
 */



class ApiCredentials(val passPhrase: String, val apiKey: String, val secret: String)

fun MutableSet<Alert>.removeAlert(alert: Alert) {
    val removeItem = this.find { a -> alert == a }
    if (removeItem != null) {
        this.remove(removeItem)
    }
}

fun ListView.setHeightBasedOnChildren() {
    val listAdapter = adapter ?: return
    val desiredWidth = MeasureSpec.makeMeasureSpec(width, MeasureSpec.UNSPECIFIED)
    var totalHeight = 0
    var view: View? = null
    for (i in 0 until listAdapter.count) {
        view = listAdapter.getView(i, view, this)
        if (i == 0) {
            view?.layoutParams = (ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        view!!.measure(desiredWidth, MeasureSpec.UNSPECIFIED)
        totalHeight += view!!.measuredHeight
    }
    val params = layoutParams
    params.height = totalHeight + dividerHeight * (listAdapter.count - 1)
    layoutParams = params
}

fun Double.btcFormat(): String = "%.8f".format(this)
fun Double.fiatFormat(): String = "%.2f".format(this)

fun String.toDoubleOrZero() = this.toDoubleOrNull() ?: 0.0
