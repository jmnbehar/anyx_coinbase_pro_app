package com.anyexchange.anyx.classes

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.app.job.JobScheduler
import android.app.job.JobInfo
import android.content.ComponentName
import android.app.job.JobParameters
import android.app.job.JobService
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


class AutoStart : BroadcastReceiver() {
    override fun onReceive(context: Context, arg1: Intent) {
        scheduleCustomAlertJob(context)
    }
    companion object {

        var hasStarted = false

        // schedule the start of the service every 1 - 5 minutes
        fun scheduleCustomAlertJob(context: Context) {
            hasStarted = true
            val serviceComponent = ComponentName(context, AlertJobService::class.java)
            val builder = JobInfo.Builder(0, serviceComponent)
            builder.setMinimumLatency((10 * 1000)) // wait at least
            builder.setOverrideDeadline((20 * 1000)) // maximum delay
            builder.setRequiresDeviceIdle(true) // device should be idle
            builder.setRequiresCharging(false) // we don't care if the device is charging or not
            val jobScheduler = context.getSystemService(JobScheduler::class.java)
            jobScheduler!!.schedule(builder.build())
        }

    }
}


class AlertJobService : JobService() {
    val apiInitData = CBProApi.CBProApiInitData(this) { }

    override fun onStartJob(params: JobParameters): Boolean {
//        AlertHub.triggerDummyAlert(this)
        if (Account.cryptoAccounts.isEmpty()) {
            CBProApi.accounts(apiInitData).getAllAccountInfo({ /* do nothing*/ }, {
                loopThroughAlerts()
                checkFillAlerts()
            })
        } else {
            Account.updateAllAccountsCandles(apiInitData, { /* do nothing*/ }, {
                loopThroughAlerts()
            })
            checkFillAlerts()
        }
        AutoStart.scheduleCustomAlertJob(applicationContext) // reschedule the job
        return true
    }
    private fun checkFillAlerts() {
        val prefs = Prefs(this)
        if (prefs.areAlertFillsActive && prefs.isLoggedIn) {
            for (account in Account.cryptoAccounts) {
                val stashedOrders = prefs.getStashedOrders(account.product.id)
                if (stashedOrders.isNotEmpty()) {
                    CBProApi.listOrders(apiInitData, productId = account.product.id).getAndStash({ }) { }
                    CBProApi.fills(apiInitData, productId = account.product.id).getAndStash({ }) { }
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
                val currentPrice = Account.forCurrency(alert.currency)?.product?.defaultPrice
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
        for (currency in enabledCurrencies) {
            val account = Account.forCurrency(currency)
            if (account != null) {
                val candles = account.product.candlesForTimespan(timespan, null)
                if (candles.size > 12) {
                    val minAlertPercentage = prefs.quickChangeThreshold.toDouble()
                    var alertPercentage = minAlertPercentage
                    var alertTimespan: QuickChangeAlert.AlertTimespan? = null
                    var changeIsPositive = false
                    val timestamp = Date().timeInSeconds()

                    val mostRecentPrice = candles.last().close
                    val tenMinutePrice = candles[candles.size - 2].close
                    val halfHourPrice = candles[candles.size - 6].close
                    val hourPrice = candles[candles.size - 12].close

                    val tenMinuteChange = percentChange(mostRecentPrice, tenMinutePrice)
                    if (tenMinuteChange > minAlertPercentage
                            && (lastQuickChangeAlertTimestamp < timestamp - TimeInSeconds.oneHour)
                            && (setTimespan == null || setTimespan == QuickChangeAlert.AlertTimespan.TEN_MINUTES)) {
                        alertPercentage = tenMinuteChange
                        alertTimespan = QuickChangeAlert.AlertTimespan.TEN_MINUTES
                        changeIsPositive = mostRecentPrice > tenMinutePrice
                    }
                    val halfHourChange = percentChange(mostRecentPrice, halfHourPrice)
                    if ((halfHourChange > minAlertPercentage) && (halfHourChange > alertPercentage + 0.5)
                            && (lastQuickChangeAlertTimestamp < timestamp - TimeInSeconds.oneHour)
                            && (setTimespan == null || setTimespan == QuickChangeAlert.AlertTimespan.HALF_HOUR)) {
                        alertPercentage = halfHourChange
                        alertTimespan = QuickChangeAlert.AlertTimespan.HALF_HOUR
                        changeIsPositive = mostRecentPrice > halfHourPrice
                    }
                    val hourChange = percentChange(mostRecentPrice, hourPrice)
                    if ((hourChange > minAlertPercentage) && (hourChange > alertPercentage + 0.5)
                            && (lastQuickChangeAlertTimestamp < timestamp - TimeInSeconds.oneHour)
                            && (setTimespan == null || setTimespan == QuickChangeAlert.AlertTimespan.HOUR)) {
                        alertPercentage = hourChange
                        alertTimespan = QuickChangeAlert.AlertTimespan.HOUR
                        changeIsPositive = mostRecentPrice > hourPrice
                    }
                    //Make sure alert Percent is high enough to be interesting but low enough to not be an error
                    if (alertPercentage > minAlertPercentage && alertPercentage < 80 && alertTimespan != null) {
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