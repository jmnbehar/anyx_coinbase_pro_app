package com.anyexchange.anyx.classes

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.support.v4.content.res.ResourcesCompat
import android.util.AttributeSet
import android.view.MotionEvent
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import android.view.VelocityTracker
import com.anyexchange.anyx.R
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.data.*
import java.util.*
import kotlin.math.absoluteValue


/**
 * Created by anyexchange on 1/17/2018.
 */

class PriceCandleChart : CandleStickChart {
    constructor(ctx: Context) : super(ctx)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { }

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

    fun configure(candles: List<Candle>, granularity: Long, currency: Currency, touchEnabled: Boolean, defaultDragDirection: DefaultDragDirection, onDefaultDrag: () -> Unit) {
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

        isScaleYEnabled = false
        isScaleXEnabled = false
        isDragEnabled = false
        isAutoScaleMinMaxEnabled = true
//        isKeepPositionOnRotation = false
        isDoubleTapToZoomEnabled = false

        addCandles(candles, granularity, currency)
    }

    fun addCandles(candles: List<Candle>, granularity: Long, currency: Currency) {
        val entries = if (candles.isEmpty()) {
            val now = Date().time.toDouble()
            val blankEntry = CandleEntry(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, now)
            listOf(blankEntry, blankEntry)
        } else {
            //Combine Candles to prevent v small candles:
            if (candles.size > 70) { //70 is safely above 60 - hour candles should never get cut down
                val compositeCandles = candles.compositeCandles(40)
                compositeCandles.withIndex().map { CandleEntry(it.index.toFloat(), it.value.high.toFloat(), it.value.low.toFloat(), it.value.open.toFloat(), it.value.close.toFloat(), it.value.time) }
            } else {
                candles.withIndex().map  { CandleEntry(it.index.toFloat(), it.value.high.toFloat(), it.value.low.toFloat(), it.value.open.toFloat(), it.value.close.toFloat(), it.value.time) }
            }
        }

        val currencyColor = currency.colorPrimary(context)
        val dataSet = CandleDataSet(entries, "Chart")
        dataSet.setDrawIcons(false)
        dataSet.setDrawValues(false)
        dataSet.shadowColor = currencyColor
        dataSet.shadowWidth = 0.7f
        dataSet.decreasingColor = ResourcesCompat.getColor(resources, R.color.anyx_red, null)
        dataSet.decreasingPaintStyle = Paint.Style.FILL
        dataSet.increasingColor = ResourcesCompat.getColor(resources, R.color.anyx_green, null)
        dataSet.increasingPaintStyle = Paint.Style.STROKE
        dataSet.neutralColor = currencyColor


        xAxis.axisLineColor = currencyColor
        axisLeft.axisLineColor = currencyColor

        val strokeWidth = 2.toFloat()
        xAxis.axisLineWidth = strokeWidth

        val data = CandleData(dataSet)

        setData(data)
        invalidate()
    }

}