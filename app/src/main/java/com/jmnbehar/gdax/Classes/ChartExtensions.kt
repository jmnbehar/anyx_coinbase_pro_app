package com.jmnbehar.gdax.Classes

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import android.view.VelocityTracker
import android.view.View
import kotlin.math.absoluteValue


/**
 * Created by jmnbehar on 1/17/2018.
 */

class PriceChart : LineChart {
    constructor(ctx: Context) : super(ctx)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { }

    var ignoreVerticalDrag = true
    var velocityTracker: VelocityTracker? = null
    private var isVerticalDrag: Boolean? = null
    var underlyingView: View? = null
    var onSideDrag: () -> Unit = { }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (velocityTracker == null) {
                    // Retrieve a new VelocityTracker object to watch the
                    // velocity of a motion.
                    velocityTracker = VelocityTracker.obtain()
                } else {
                    // Reset the velocity tracker back to its initial state.
                    velocityTracker?.clear()
                }
                // Add a user's movement to the tracker.
                velocityTracker?.addMovement(event)
                isVerticalDrag = null
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                velocityTracker?.computeCurrentVelocity(1000)

                val xVelocity = velocityTracker?.xVelocity?.absoluteValue ?: 0.0.toFloat()
                val yVelocity = velocityTracker?.yVelocity?.absoluteValue ?: 0.0.toFloat()
                if (isVerticalDrag == null && (xVelocity > 1 || yVelocity > 1)) {
                    if (yVelocity > (xVelocity * 1.5)) {
                        isVerticalDrag = true
                    } else if (yVelocity < (xVelocity * 1.5)) {
                        onSideDrag()
                        isVerticalDrag = false
                    }
                }
                println("is vertical drag? $isVerticalDrag")
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.recycle()
                velocityTracker = null
                isVerticalDrag = null
            }
        }

        return if (ignoreVerticalDrag && (isVerticalDrag == true)) {
           // underlyingView?.onTouchEvent(event) ?: false
            false
        } else {
            super.onTouchEvent(event)
        }
    }

fun configure(candles: List<Candle>, currency: Currency, touchEnabled: Boolean, timeRange: Int, timeChangable: Boolean) {
    setDrawGridBackground(false)
    setDrawBorders(false)
    var noDescription = Description()
    noDescription.text = ""
    description = noDescription
    legend.isEnabled = false
    xAxis.setDrawGridLines(false)
    xAxis.position = XAxis.XAxisPosition.BOTTOM

    axisLeft.showOnlyMinMaxValues = true
    axisLeft.setDrawGridLines(false)
    axisLeft.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)

    axisRight.showOnlyMinMaxValues = true
    axisRight.setDrawGridLines(false)
    axisRight.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)

    setTouchEnabled(touchEnabled)

    setScaleEnabled(false)
    isDoubleTapToZoomEnabled = false


    addCandles(candles, currency, timeRange)
}

fun addCandles(candles: List<Candle>, currency: Currency, timeRange: Int) {
    val entries = candles.withIndex().map { Entry(it.index.toFloat(), it.value.close.toFloat()) }
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

}


class XAxisDateFormatter(private val values: DoubleArray, var timeRange: Int) : IAxisValueFormatter {
    override fun getFormattedValue(value: Float, axis: AxisBase): String {
        val dateDouble = values[value.toInt()]
        return dateDouble.toStringWithTimeRange(timeRange)
    }
}