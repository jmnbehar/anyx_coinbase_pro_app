package com.jmnbehar.gdax.Classes

import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


/**
 * Created by jmnbehar on 1/17/2018.
 */

fun LineChart.configure(candles: List<Candle>, currency: Currency, touchEnabled: Boolean, timeRange: Int, timeChangable: Boolean) {
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


    addCandles(candles, currency, timeRange)
}

fun LineChart.addCandles(candles: List<Candle>, currency: Currency, timeRange: Int) {
    val entries = candles.withIndex().map { Entry(it.index.toFloat(), it.value.close.toFloat()) }
    //TODO: reverse candles at its source
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

    val dates = candles.map { c -> c.time }.toDoubleArray()
    xAxis.valueFormatter = XAxisDateFormatter(dates, timeRange)
    dataSet.setDrawCircles(false)
    val lineData = LineData(dataSet)
    this.data = lineData
    this.invalidate()
}

class XAxisDateFormatter(private val values: DoubleArray, var timeRange: Int) : IAxisValueFormatter {
    override fun getFormattedValue(value: Float, axis: AxisBase): String {
        val formatter = when (timeRange) {
            TimeInSeconds.oneDay -> DateTimeFormatter.ofPattern("h:mma")
            TimeInSeconds.oneWeek -> DateTimeFormatter.ofPattern("EEE")
            TimeInSeconds.oneMonth -> DateTimeFormatter.ofPattern("M/d")
           // TimeInSeconds.oneYear -> DateTimeFormatter.ofPattern("LLL")
           // TimeInSeconds.all -> DateTimeFormatter.ofPattern("M/d")
            else -> DateTimeFormatter.ofPattern("h:mma")
        }
        val dateLong = values[value.toInt()].toLong()
        return Instant.ofEpochSecond(dateLong).atZone(ZoneId.systemDefault()).toLocalDateTime().format(formatter)
    }
}