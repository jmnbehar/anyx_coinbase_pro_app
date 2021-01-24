package com.anyexchange.cryptox.classes

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.app.job.JobScheduler
import android.app.job.JobInfo
import android.content.ComponentName
import android.app.job.JobParameters
import android.app.job.JobService
import com.anyexchange.cryptox.api.AnyApi
import com.anyexchange.cryptox.api.ApiInitData
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


class AutoStart : BroadcastReceiver() {
    override fun onReceive(context: Context, arg1: Intent) {
        //TODO: test action string to ensure it is safe
//        arg1.action
        scheduleCustomAlertJob(context)
    }
    companion object {

        var hasStarted = false

        // schedule the start of the service every 1 - 10 minutes
        fun scheduleCustomAlertJob(context: Context) {
            hasStarted = true
            val serviceComponent = ComponentName(context, AlertJobService::class.java)
            val builder = JobInfo.Builder(0, serviceComponent)
            builder.setMinimumLatency(TimeInMillis.threeSeconds) // wait at least
            builder.setOverrideDeadline(TimeInMillis.halfMinute) // maximum delay
            builder.setRequiresDeviceIdle(true) // device should be idle
            builder.setRequiresCharging(false) // we don't care if the device is charging or not
            val jobScheduler = context.getSystemService(JobScheduler::class.java)
            jobScheduler?.schedule(builder.build())
        }

    }
}


class AlertJobService : JobService() {
    val apiInitData = ApiInitData(this) { }

    override fun onStartJob(params: JobParameters): Boolean {
        if (Product.map.isEmpty()) {
            Product.map = Prefs(this).stashedProducts.associateBy { it.currency.id }.toMutableMap()
        }
//        AlertHub.triggerDummyAlert(this)
        if (Product.map.isEmpty()) {
            AnyApi(apiInitData).getAllProducts({ /* do nothing*/ }, {
                //TODO: get all accounts here?
                Product.updateImportantCandles(apiInitData, { /* do nothing*/ }, {
                    loopThroughAlerts()
                })
                checkFillAlerts()
            })
        } else {
            Product.updateImportantCandles(apiInitData, { /* do nothing*/ }, {
                loopThroughAlerts()
            })
            checkFillAlerts()
        }
        AutoStart.scheduleCustomAlertJob(applicationContext) // reschedule the job
        return true
    }

    private fun checkFillAlerts() {
        val prefs = Prefs(this)
        val anyApi = AnyApi(apiInitData)
        if (prefs.areFillAlertsActive && Exchange.isAnyLoggedIn()) {
            val orderTradingPairs = mutableListOf<TradingPair>()
            for (product in Product.map.values) {
                //TODO: fix for multiple exchanges
                val stashedOrders = prefs.getStashedOrders(product.currency)
                val partialTradingPairs = stashedOrders.map { it.tradingPair }
                orderTradingPairs.addAll(partialTradingPairs)
            }

            if (orderTradingPairs.isNotEmpty()) {
                val orderExchangeList = orderTradingPairs.map { it.exchange }.distinct()
                for (exchange in orderExchangeList) {
                    anyApi.getAndStashOrderList(exchange, null, { }, { })
                }
                for (tradingPair in orderTradingPairs) {
                    anyApi.getAndStashFillList(tradingPair.exchange, tradingPair, { }, { } )
                }
            }
        }
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return true
    }

    private val timespan = Timespan.DAY

    private var quickChangeTimestampBackingLong: Long? = null
    private var lastQuickChangeAlertTimestamp: Long
        get() {
            return quickChangeTimestampBackingLong ?: Prefs(this).lastQuickChangeAlertTimestamp
        }
        set(value) {
            quickChangeTimestampBackingLong = value
            Prefs(this).lastQuickChangeAlertTimestamp = value
        }


    private fun loopThroughAlerts() {
        val prefs = Prefs(this)
        val alerts = prefs.alerts
        for (alert in alerts) {
            if (!alert.hasTriggered) {
                val currentPrice = Product.forCurrency(alert.currency)?.defaultPrice
                if (alert.triggerIfAbove && (currentPrice != null) && (currentPrice >= alert.price)) {
                    AlertHub.triggerPriceAlert(alert, this)
                } else if (!alert.triggerIfAbove && (currentPrice != null) && (currentPrice <= alert.price)) {
                    AlertHub.triggerPriceAlert(alert, this)
                }
            }
        }

        val enabledCurrencies = prefs.quickChangeAlertCurrencies
        var changeAlert: QuickChangeAlert? = null
        val setTimespan: QuickChangeAlert.AlertTimespan? = null
        for (currencyId in enabledCurrencies) {
            val currency = Currency(currencyId)
            val product = Product.forCurrency(currency)
            if (product != null) {
                val candles = product.candlesForTimespan(timespan, null)
                if (candles.size > 12) {
                    val minAlertPercentage = prefs.quickChangeThreshold.toDouble()
                    var alertPercentage = 0.0
                    var alertTimespan: QuickChangeAlert.AlertTimespan? = null
                    var changeIsPositive = false
                    val timestamp = Date().timeInSeconds()

                    val mostRecentPrice = candles.last().close
                    val lastIndex = candles.size - 1
                    val tenMinutePrice = candles[lastIndex - 2].close
                    val halfHourPrice = candles[lastIndex - 6].close
                    val hourPrice = candles[lastIndex - 12].close

                    val tenMinuteChange = percentChange(mostRecentPrice, tenMinutePrice)
                    if (tenMinuteChange > minAlertPercentage
                            && (setTimespan == null || setTimespan == QuickChangeAlert.AlertTimespan.TEN_MINUTES)) {
                        alertPercentage = tenMinuteChange
                        alertTimespan = QuickChangeAlert.AlertTimespan.TEN_MINUTES
                        changeIsPositive = mostRecentPrice > tenMinutePrice
                    }
                    val halfHourChange = percentChange(mostRecentPrice, halfHourPrice)
                    if ((halfHourChange > minAlertPercentage) && (halfHourChange > alertPercentage + 0.5)
                            && (setTimespan == null || setTimespan == QuickChangeAlert.AlertTimespan.HALF_HOUR)) {
                        alertPercentage = halfHourChange
                        alertTimespan = QuickChangeAlert.AlertTimespan.HALF_HOUR
                        changeIsPositive = mostRecentPrice > halfHourPrice
                    }
                    val hourChange = percentChange(mostRecentPrice, hourPrice)
                    if ((hourChange > minAlertPercentage) && (hourChange > alertPercentage + 0.5)
                            && (setTimespan == null || setTimespan == QuickChangeAlert.AlertTimespan.HOUR)) {
                        alertPercentage = hourChange
                        alertTimespan = QuickChangeAlert.AlertTimespan.HOUR
                        changeIsPositive = mostRecentPrice > hourPrice
                    }
                    //Make sure alert Percent is high enough to be interesting but low enough to not be an error
                    if (alertPercentage > minAlertPercentage && alertPercentage < 80 && alertTimespan != null
                            && (lastQuickChangeAlertTimestamp + TimeInSeconds.oneHour < timestamp)) {
                        lastQuickChangeAlertTimestamp = timestamp

                        if (changeAlert == null) {
                            changeAlert = QuickChangeAlert(mutableListOf(currency), alertPercentage, changeIsPositive, alertTimespan)
                        } else if (changeIsPositive == changeAlert.isChangePositive) {
                            changeAlert.currencies.add(currency)
                            //Make sure alert Percent is high enough to be interesting but
                            if (alertPercentage < changeAlert.percentChange) {
                                val doublePercent: Int = (alertPercentage * 2.0).roundToInt()
                                val roundedPercent: Double = doublePercent / 2.0
                                changeAlert.percentChange = roundedPercent
                            }
                        }
                    }
                }
            }
        }
        if (changeAlert != null) {
            AlertHub.triggerQuickChangeAlert(changeAlert, this)
        }
    }


    private fun percentChange(start: Double, end: Double) : Double {
        val change = end - start
        val percentChange = (change / start) * 100.0
        return percentChange.absoluteValue
    }

}