package com.anyexchange.anyx.classes

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import android.app.job.JobScheduler
import android.app.job.JobInfo
import android.content.ComponentName
import android.app.job.JobParameters
import android.app.job.JobService
import java.sql.Time
import java.util.*
import kotlin.math.absoluteValue


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

    private var movementTimestampBackingLong: Long? = null
    private var lastMovementAlertTimestamp: Long
        get() {
            val tempTimestamp = movementTimestampBackingLong
            return if (tempTimestamp == null) {
                Prefs(this).lastMovementAlertTimestamp
            } else {
                tempTimestamp
            }
        }
        set(value) {
            Prefs(this).lastMovementAlertTimestamp = value
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

        //TODO: revert this
//        val enabledCurrencies = prefs.rapidMovementAlertCurrencies
        val enabledCurrencies = listOf(Currency.ETH)
        for (currency in enabledCurrencies) {
            val account = Account.forCurrency(currency)
            if (account != null) {
                val candles = account.product.candlesForTimespan(timespan, null)
                if (candles.size > 12) {
                    val minAlertPercentage = 0.7
                    var alertPercentage = minAlertPercentage
                    var alertTime = ""
                    var changeIsPositive = false
                    val timestamp = Date().timeInSeconds()

                    val mostRecentPrice = candles.last().close
                    val twentyMinutePrice = candles[candles.size - 4].close
                    val halfHourPrice = candles[candles.size - 6].close
                    val hourPrice = candles[candles.size - 12].close

                    val twentyMinuteChange = percentChange(mostRecentPrice, twentyMinutePrice)
                    if (twentyMinuteChange > alertPercentage && (lastMovementAlertTimestamp < timestamp - TimeInSeconds.twentyMinutes)) {
                        alertPercentage = twentyMinuteChange
                        alertTime = "twenty minutes"
                        changeIsPositive = mostRecentPrice > twentyMinutePrice
                    }
                    val halfHourChange = percentChange(mostRecentPrice, halfHourPrice)
                    if (halfHourChange > alertPercentage + 0.5 && (lastMovementAlertTimestamp < timestamp - TimeInSeconds.halfHour)) {
                        alertPercentage = halfHourChange
                        alertTime = "half hour"
                        changeIsPositive = mostRecentPrice > halfHourPrice
                    }
                    val hourChange = percentChange(mostRecentPrice, hourPrice)
                    if (hourChange > alertPercentage + 0.5 && (lastMovementAlertTimestamp < timestamp - TimeInSeconds.oneHour)) {
                        alertPercentage = hourChange
                        alertTime = "hour"
                        changeIsPositive = mostRecentPrice > hourPrice
                    }
                    if (alertPercentage > minAlertPercentage) {
                        lastMovementAlertTimestamp = timestamp

                        AlertHub.triggerRapidMovementAlert(currency, alertPercentage, changeIsPositive, alertTime, this)
                    }
                }
            }
        }
    }


    private fun percentChange(start: Double, end: Double) : Double {
        val change = end - start
        val percentChange = (change / start) * 100.0
        return percentChange.absoluteValue
    }

}