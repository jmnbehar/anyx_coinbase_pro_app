package com.jmnbehar.gdax.Classes

import android.graphics.Color
import android.support.v4.app.Fragment
import android.widget.ListView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.text.DecimalFormat
import android.view.ViewGroup
import android.view.View.MeasureSpec
import android.view.View


/**
 * Created by josephbehar on 12/28/17.
 */


open class RefreshFragment: Fragment() {
    open fun refresh(onComplete: () -> Unit) {
        onComplete()
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

fun DecimalFormat.btcFormat(value: Double) = "%.8f".format(value)

fun String.toDoubleOrZero() = this.toDoubleOrNull() ?: 0.0

fun LineChart.addCandles(candles: List<Candle>) {
    val entries = candles.reversed().withIndex().map { Entry(it.index.toFloat(), it.value.close.toFloat()) }
    println("first time: " + candles.first().time)
    println("last time: " + candles.last().time)

    val dataSet = LineDataSet(entries, "Chart")
    dataSet.setColor(Color.BLUE)
//    dataSet.setValueTextColors(Color.GRAY)
    val lineData = LineData(dataSet)
    this.data = lineData
    this.invalidate()
}