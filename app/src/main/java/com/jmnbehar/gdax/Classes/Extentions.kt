package com.jmnbehar.gdax.Classes

import android.content.Context
import android.graphics.Color
import android.support.v4.app.Fragment
import android.widget.Toast
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.text.DecimalFormat

/**
 * Created by josephbehar on 12/28/17.
 */


fun Context.toast(message: CharSequence) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
fun Context.toastLong(message: CharSequence) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()


fun Fragment.toast(message: CharSequence, context: Context) =
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
fun Fragment.toastLong(message: CharSequence, context: Context) =
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()

fun DecimalFormat.getBitcoinFormat(value: Double) = "%.8f".format(value)

fun String.toDoubleOrZero() = this.toDoubleOrNull() ?: 0.0

fun LineChart.addCandles(candles: List<Candle>) {
    val entries = candles.map { Entry(it.time.toFloat(), it.close.toFloat()) }
    val dataSet = LineDataSet(entries, "Chart")
    dataSet.setColor(Color.BLUE)
//    dataSet.setValueTextColors(Color.GRAY)
    val lineData = LineData(dataSet)
    this.data = lineData
    this.invalidate()
}