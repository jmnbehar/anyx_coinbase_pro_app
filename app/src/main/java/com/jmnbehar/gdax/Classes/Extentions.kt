package com.jmnbehar.gdax.Classes

import android.content.Context
import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.widget.ListView
import android.view.ViewGroup
import android.view.View.MeasureSpec
import android.view.View
import android.view.ViewManager
import android.widget.LinearLayout
import org.jetbrains.anko.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by josephbehar on 12/28/17.
 *
 *
import org.bitcoinj.core.Address
import org.bitcoinj.core.AddressFormatException
import org.bitcoinj.core.NetworkParameters

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
}

fun Double.btcFormat(): String = "%.8f".format(this)
fun Double.fiatFormat(): String = "%.2f".format(this)

fun String.toDoubleOrZero() = this.toDoubleOrNull() ?: 0.0

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}

fun Double.toStringWithTimeRange(timeRange: Long) : String {
    val locale = Locale.getDefault()
    val formatter = when (timeRange) {
        TimeInSeconds.oneHour -> SimpleDateFormat("h:mma", locale)
        TimeInSeconds.oneDay -> SimpleDateFormat("h:mma M/d", locale)
        TimeInSeconds.oneWeek -> SimpleDateFormat("h:mma M/d", locale)
        TimeInSeconds.oneMonth -> SimpleDateFormat("h:mma M/d", locale)
        TimeInSeconds.oneYear -> SimpleDateFormat("M/d/YYYY", locale)
        else -> SimpleDateFormat("M/d/YYYY", locale) //This is most likely all
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

/**
 * Created by josephbehar on 1/30/18.
 */

fun String.isValidAddress(currency: Currency): Boolean {
    //TODO: this
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