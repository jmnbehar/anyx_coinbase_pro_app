package com.jmnbehar.gdax.Classes

import android.widget.ListView
import android.view.ViewGroup
import android.view.View.MeasureSpec
import android.view.View
import android.view.ViewManager
import android.widget.LinearLayout
import org.jetbrains.anko.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


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

fun ViewManager.horizontalLayout(string1: String, string2: String) : LinearLayout {
    return linearLayout {
        textView(string1)
        textView(string2).lparams(width = matchParent) { textAlignment = right }
        padding = dip(3)
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


fun Double.toStringWithTimeRange(timeRange: Int) : String {
    val formatter = when (timeRange) {
        TimeInSeconds.oneDay -> DateTimeFormatter.ofPattern("h:mma")
        TimeInSeconds.oneWeek -> DateTimeFormatter.ofPattern("EEE")
        TimeInSeconds.oneMonth -> DateTimeFormatter.ofPattern("M/d")
    // TimeInSeconds.oneYear -> DateTimeFormatter.ofPattern("LLL")
    // TimeInSeconds.all -> DateTimeFormatter.ofPattern("M/d")
        else -> DateTimeFormatter.ofPattern("h:mma")
    }
    val dateLong = this.toLong()
    return Instant.ofEpochSecond(dateLong).atZone(ZoneId.systemDefault()).toLocalDateTime().format(formatter)

}