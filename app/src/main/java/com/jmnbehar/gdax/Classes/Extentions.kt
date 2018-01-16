package com.jmnbehar.gdax.Classes

import android.graphics.Color
import android.support.v4.app.Fragment
import android.widget.ListView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import android.view.ViewGroup
import android.view.View.MeasureSpec
import android.view.View
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis


/**
 * Created by josephbehar on 12/28/17.
 */


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


fun LineChart.configure(candles: List<Candle>, currency: Currency, touchEnabled: Boolean, timeChangable: Boolean) {
    setDrawGridBackground(false)
    setDrawBorders(false)
    var noDescription = Description()
    noDescription.text = ""
    description = noDescription
    legend.isEnabled = false
    xAxis.setDrawGridLines(false)
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    axisLeft.setDrawGridLines(false)
    axisRight.setDrawGridLines(false)
    axisLeft.setLabelCount(3, false)
    axisRight.setLabelCount(3, false)

    setTouchEnabled(touchEnabled)

    setScaleEnabled(false)
    isDoubleTapToZoomEnabled = false


    addCandles(candles, currency)
}

fun LineChart.addCandles(candles: List<Candle>, currency: Currency) {
    val entries = candles.reversed().withIndex().map { Entry(it.index.toFloat(), it.value.close.toFloat()) }
    println("first time: " + candles.first().time)
    println("last time: " + candles.last().time)
    val dataSet = LineDataSet(entries, "Chart")

    val color = when (currency) {
        Currency.BTC -> Color.YELLOW
        Currency.BCH -> Color.GREEN
        Currency.ETH -> Color.BLUE
        Currency.LTC -> Color.GRAY
        Currency.USD -> Color.BLACK
    }
    dataSet.color = color
    dataSet.lineWidth = 2.toFloat()
    dataSet.setDrawFilled(true)
    dataSet.fillColor = color

    dataSet.setDrawCircles(false)
    val lineData = LineData(dataSet)
    this.data = lineData
    this.invalidate()
}