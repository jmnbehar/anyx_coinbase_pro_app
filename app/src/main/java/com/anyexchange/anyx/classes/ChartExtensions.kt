package com.anyexchange.anyx.classes

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.YAxis
import android.view.VelocityTracker
import kotlin.math.absoluteValue


/**
 * Created by anyexchange on 1/17/2018.
 */

class PriceChart : LineChart {
    constructor(ctx: Context) : super(ctx)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { }

    enum class DefaultDragDirection {
        Horizontal,
        Vertical;
    }

    private var velocityTracker: VelocityTracker? = null
    private var isVerticalDrag: Boolean? = null
    private var onSideDrag: () -> Unit = { }
    private var onVerticalDrag: () -> Unit = { }
    private var defaultDragDirection: DefaultDragDirection = DefaultDragDirection.Horizontal

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain()
                } else {
                    // Reset the velocity tracker back to its initial state.
                    velocityTracker?.clear()
                }
                velocityTracker?.addMovement(event)
                isVerticalDrag = null
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                velocityTracker?.computeCurrentVelocity(1000)

                val xVelocity = velocityTracker?.xVelocity?.absoluteValue ?: 0.0.toFloat()
                val yVelocity = velocityTracker?.yVelocity?.absoluteValue ?: 0.0.toFloat()

                val xCoefficient = if (defaultDragDirection == DefaultDragDirection.Horizontal) { 5.toFloat() } else { 1.25.toFloat() }
                val yCoefficient = if (defaultDragDirection == DefaultDragDirection.Vertical)   { 5.toFloat() } else { 1.25.toFloat() }

                if (isVerticalDrag == null && (xVelocity > 5 || yVelocity > 5)) {
                    if (yVelocity > (xVelocity * xCoefficient)) {
                        onVerticalDrag()
                        isVerticalDrag = true
                    } else if (xVelocity > (yVelocity * yCoefficient)) {
                        onSideDrag()
                        isVerticalDrag = false
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.recycle()
                velocityTracker = null
                isVerticalDrag = null
            }
        }

        return if (defaultDragDirection == DefaultDragDirection.Horizontal && (isVerticalDrag == true)) {
            false
        } else if (defaultDragDirection == DefaultDragDirection.Vertical && (isVerticalDrag == false)) {
            false
        } else {
            super.onTouchEvent(event)
        }
    }

    fun configure(candles: List<Candle>, currency: Currency, touchEnabled: Boolean, defaultDragDirection: DefaultDragDirection, onDefaultDrag: () -> Unit) {
        setDrawGridBackground(false)
        setDrawBorders(false)
        val newDescription = Description()
        if (touchEnabled) {
            newDescription.text = ""
            description = newDescription
            description.isEnabled = false
        } else {
            newDescription.text = "24h"
            newDescription.textSize = 18f
            newDescription.yOffset = 5f
            newDescription.xOffset = 5f
            description = newDescription
            description.isEnabled = true
        }

        this.defaultDragDirection = defaultDragDirection
        when (defaultDragDirection) {
            DefaultDragDirection.Horizontal -> onSideDrag   = onDefaultDrag
            DefaultDragDirection.Vertical -> onVerticalDrag = onDefaultDrag
        }

        legend.isEnabled = false
        xAxis.setDrawGridLines(false)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawLabels(false)

        axisLeft.showOnlyMinMaxValues = true
        axisLeft.setDrawGridLines(false)
        axisLeft.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)

        //axisRight.showOnlyMinMaxValues = true
        axisRight.setDrawLabels(false)
        axisRight.setDrawGridLines(false)
        axisRight.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)

        setTouchEnabled(touchEnabled)

        setScaleEnabled(false)
        isDoubleTapToZoomEnabled = false

        addCandles(candles, currency)
    }

    fun addCandles(candles: List<Candle>, currency: Currency) {
        val entries = if (candles.isEmpty()) {
            val blankEntry = Entry(0.0f, 0.0f)
            listOf(blankEntry, blankEntry)
        } else {
            candles.withIndex().map { Entry(it.index.toFloat(), it.value.close.toFloat()) }
        }
        val dataSet = LineDataSet(entries, "Chart")

        val color = currency.colorPrimary(context)

        val strokeWidth = 2.toFloat()
        dataSet.color = color
        dataSet.lineWidth = strokeWidth

        xAxis.axisLineColor = color
//        axisLeft.axisLineColor = color
//        axisRight.axisLineColor = color

        xAxis.axisLineWidth = strokeWidth
//        axisLeft.axisLineWidth = strokeWidth
//        axisRight.axisLineWidth = strokeWidth

        dataSet.setDrawFilled(true)
        dataSet.fillColor = color
        dataSet.setDrawValues(false)

        val open = candles.firstOrNull()?.close?.toFloat()
        if (open != null) {
//            axisLeft.showSpecificLabels(floatArrayOf(open), false)
            axisLeft.setDrawPartialAxis(open)
        }
        val close = candles.lastOrNull()?.close?.toFloat()
        if (close != null) {
            //axisRight.showSpecificLabels(floatArrayOf(close), false)
            axisRight.setDrawPartialAxis(close)
        }

        //TODO: fill in missing entries
//        val dates = candles.map { c -> c.time }.toDoubleArray()
//        xAxis.valueFormatter = XAxisDateFormatter(dates, timespan)
        dataSet.setDrawCircles(false)
        val lineData = LineData(dataSet)
        this.data = lineData
        this.invalidate()
//        this.animateY(500)
    }

}


//class XAxisDateFormatter(private val values: DoubleArray, var timeRange: Int) : IAxisValueFormatter {
//    override fun getFormattedValue(value: Float, axis: AxisBase): String {
//        val dateDouble = values[value.toInt()]
//        return dateDouble.toStringWithTimespan(timeRange)
//    }
//}