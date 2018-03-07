package com.jmnbehar.anyx.Classes

import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.widget.ListView
import android.view.ViewGroup
import android.view.View.MeasureSpec
import android.view.View
import android.view.ViewManager
import android.widget.LinearLayout
import org.jetbrains.anko.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

/**
 * Created by josephbehar on 12/28/17.
 *

 */

fun MutableSet<Alert>.removeAlert(alert: Alert) {
    val removeItem = this.find { a -> alert == a }
    if (removeItem != null) {
        this.remove(removeItem)
    }
}

fun ViewManager.horizontalLayout(string1: String, string2: String) : LinearLayout {
    return linearLayout {
        textView(string1).lparams(weight = 1f)
        space().lparams(weight = 20f)
        textView(string2).lparams(weight = 1f)
        padding = dip(3)
    }
}

fun ListView.setHeightBasedOnChildren(): Int {
    val listAdapter = adapter ?: return 0
    val bottomPadding = 66
    val desiredWidth = MeasureSpec.makeMeasureSpec(width, MeasureSpec.UNSPECIFIED)
    var totalHeight = 0
    var view: View? = null
    for (i in 0 until listAdapter.count) {
        view = listAdapter.getView(i, view, this)
        if (i == 0) {
            view?.layoutParams = (ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        view!!.measure(desiredWidth, MeasureSpec.UNSPECIFIED)
        totalHeight += view.measuredHeight
    }
    val params = layoutParams
    params.height = totalHeight + dividerHeight * (listAdapter.count - 1) + bottomPadding
    layoutParams = params
    return params.height
}

fun Double.btcFormat(): String = "%.8f".format(this)
fun Double.fiatFormat(): String {
    var numberFormat = NumberFormat.getNumberInstance(Locale.US)
    numberFormat.currency = java.util.Currency.getInstance(Locale.US)
    numberFormat.minimumFractionDigits = 2
    numberFormat.maximumFractionDigits = 2
    val sign = if (this >= 0) { "" } else { "-" }
    val output = "$sign\$${numberFormat.format(this.absoluteValue)}"
    return output
}

fun Double.percentFormat(): String = "%.2f".format(this) + "%"

fun String.toDoubleOrZero() = this.toDoubleOrNull() ?: 0.0

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}

fun Double.toStringWithTimespan(timespan: Timespan) : String {
    val locale = Locale.getDefault()
    val formatter = when (timespan) {
        Timespan.HOUR  -> SimpleDateFormat("h:mma", locale)
        Timespan.DAY   -> SimpleDateFormat("h:mma M/d", locale)
        Timespan.WEEK  -> SimpleDateFormat("h:mma M/d", locale)
        Timespan.MONTH -> SimpleDateFormat("h:mma M/d", locale)
        Timespan.YEAR  -> SimpleDateFormat("M/d/YYYY", locale)
        Timespan.ALL   -> SimpleDateFormat("M/d/YYYY", locale)
    }
    val itemLong = (this * 1000).toLong()
    val itemDate = Date(itemLong)
    return formatter.format(itemDate)
}

fun Calendar.timeInSeconds() : Long {
    val timeInMillis = this.timeInMillis
    return (timeInMillis / 1000)
}

fun Date.timeInSeconds() : Long {
    val floatMillis = this.time
    val floatSeconds = floatMillis / 1000
    return floatSeconds
}

fun List<Candle>.updateWith(newList: List<Candle>, timespan: Int) {

    val now = Calendar.getInstance()
    val nowInSeconds = now.timeInSeconds()
    val timespanStart = nowInSeconds - timespan

    val firstInTimespan = this.indexOfFirst { candle -> candle.time >= timespanStart }
    val trimmedList = this.subList(firstInTimespan, this.lastIndex).toMutableList()
    trimmedList.addAll(newList)

}

fun String.isValidAddress(currency: Currency): Boolean {
    //TODO: use this to help if someone puts in invalid info here
    return if ((this.length < 25)  || (this.length > 36)) {
        false
    } else if (this.contains('0', true)) {
        false
    } else if (this.contains('O', true)) {
        false
//    } else if (this.contains('1', true)) {
//        false
    } else if (this.contains('I', true)) {
        false
    } else {
        return when (currency) {
            Currency.BTC -> (this[0] == '1')
            Currency.ETH -> (this[0] == '0' && this[0] == '0')
            else -> true
        }
    }
}