package com.anyexchange.anyx.classes

import android.arch.lifecycle.Lifecycle
import android.support.annotation.LayoutRes
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.text.style.QuoteSpan
import android.view.LayoutInflater
import android.widget.ListView
import android.view.ViewGroup
import android.view.View.MeasureSpec
import android.view.View
import android.view.ViewManager
import android.widget.LinearLayout
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import org.jetbrains.anko.*
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

/**
 * Created by josephbehar on 12/28/17.
 *

 */

fun MutableSet<PriceAlert>.removeAlert(alert: PriceAlert) {
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

fun ViewManager.horizontalLayout(string1Id: Int, string2: String) : LinearLayout {
    return linearLayout {
        val string1 = resources.getString(string1Id)
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

fun RecyclerView.setHeightBasedOnChildren(): Int {
    val listAdapter = adapter ?: return 0
    val bottomPadding = 66
    val desiredWidth = MeasureSpec.makeMeasureSpec(width, MeasureSpec.UNSPECIFIED)
    var totalHeight = 0
    var view: View?
    for (i in 0 until listAdapter.itemCount) {
        view = layoutManager.findViewByPosition(i)

        if (i == 0) {
            view?.layoutParams = (ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        view?.measure(desiredWidth, MeasureSpec.UNSPECIFIED)
        totalHeight += view?.measuredHeight ?: 0
    }
    val params = layoutParams

    //TODO: figure out dividerHeight for recyclerView
    val dividerHeight = 0
    params.height = totalHeight + dividerHeight * (listAdapter.itemCount - 1) + bottomPadding
    layoutParams = params
    return params.height
}

fun Double.btcFormat(): String = "%.8f".format(this)
fun Double.btcFormatShortened(): String {
    var string = "%.8f".format(this)
    while (string.last() == '0') {
        string = string.substring(0, string.lastIndex)
    }
    if (string.last() == '.') {
        string += '0'
    }
    return string
}
fun Double.fiatFormat(currency: Currency): String {
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    numberFormat.currency = java.util.Currency.getInstance(Locale.US)
    numberFormat.minimumFractionDigits = 2
    numberFormat.maximumFractionDigits = 2
    val sign = if (this >= 0) { "" } else { "-" }
    val currencySymbol = currency.symbol
    return "$sign$currencySymbol${numberFormat.format(this.absoluteValue)}"
}
fun Double.format(currency: Currency): String {
    return if (currency.isFiat) {
        this.fiatFormat(currency)
    } else {
        this.btcFormatShortened() + " " + currency.toString()
    }
}
fun Float.volumeFormat() = "%.2f".format(this)

fun Double.percentFormat(): String = "%.2f".format(this) + "%"
fun Double.intPercentFormat(): String = "%.0f".format(this) + "%"

fun String?.toDoubleOrZero() = this?.toDoubleOrNull() ?: 0.0

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}

val Result.Failure<Any, FuelError>.errorMessage : String
    get() {
        return try {
            val errorData = JSONObject(String(error.response.data))
            (errorData["message"] as? String) ?: error.response.responseMessage
        } catch (e: Exception) {
            if (error.response.statusCode == ErrorCode.NoInternet.code) {
                "Can't access Coinbase Pro"
            } else {
                ""
            }
        }
    }

//val Result.Failure<String, FuelError>.errorMessage : String
//    get() {
//        val errorData = JSONObject(String(error.response.data))
//        return (errorData["message"] as? String) ?: error.response.responseMessage
//    }

fun Long.toStringWithTimespan(timespan: Timespan) : String {
    val locale = Locale.getDefault()
    val formatter = when (timespan) {
        Timespan.HOUR  -> SimpleDateFormat("h:mma", locale)
        Timespan.DAY   -> SimpleDateFormat("h:mma M/d", locale)
        Timespan.WEEK  -> SimpleDateFormat("h:mma M/d", locale)
        Timespan.MONTH -> SimpleDateFormat("h:mma M/d", locale)
        Timespan.YEAR  -> SimpleDateFormat("M/d/YYYY", locale)
//        Timespan.ALL   -> SimpleDateFormat("M/d/YYYY", locale)
    }
    //TODO: investigate when this is needed: only for cbpro?
    val itemLong = (this * 1000)
    val itemDate = Date(itemLong)
    return formatter.format(itemDate)
}

fun Calendar.timeInSeconds() : Long {
    val timeInMillis = this.timeInMillis
    return (timeInMillis / 1000)
}

fun Date.timeInSeconds() : Long {
    val floatMillis = this.time
    return floatMillis / 1000
}


val Lifecycle.isCreatedOrResumed : Boolean
    get() = when (currentState) {
        Lifecycle.State.STARTED, Lifecycle.State.RESUMED -> true
        else -> false
    }

fun TabLayout.setupCryptoTabs(onSelected: (Currency) -> Unit) {
    removeAllTabs()
    for (currency in Currency.cryptoList) {
        val newTab = newTab()
        newTab.text = currency.toString()
        addTab(newTab)
    }
    addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
            val selectedCurrency = Currency.cryptoList[tab.position]
            onSelected(selectedCurrency)
        }
        override fun onTabUnselected(tab: TabLayout.Tab) {}
        override fun onTabReselected(tab: TabLayout.Tab) {}
    })
}

fun List<TradingPair>.sortTradingPairs() : List<TradingPair> {
    return this.sortedWith(compareBy({ it.quoteCurrency == Account.defaultFiatCurrency }, { it.quoteCurrency.orderValue })).reversed()
}

fun List<Currency>.sortCurrencies() : List<Currency> {
    return this.sortedWith(compareBy({ Product.map[it.id]?.totalDefaultValueOfRelevantAccounts() ?: 0.0 }, { it.orderValue })).reversed()
}


fun String.dateFromCBProApiDateString(): Date? {
    return try {
        val locale = Locale.getDefault()
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'", locale)
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.parse(this)
    } catch (e: Exception) {
        null
    }
}
fun Date.format(formatString: String): String {
    val outputFormat = SimpleDateFormat(formatString, Locale.getDefault())
    outputFormat.timeZone = TimeZone.getDefault()
    return outputFormat.format(this)
}

fun List<TradingPair>.withQuoteCurrency(quoteCurrency: Currency) : TradingPair?  {
    return this.find { it.quoteCurrency == quoteCurrency }
}


fun Fragment.toast(textResource: Int) = activity?.toast(textResource)
fun Fragment.toast(text: CharSequence) = activity?.toast(text)
