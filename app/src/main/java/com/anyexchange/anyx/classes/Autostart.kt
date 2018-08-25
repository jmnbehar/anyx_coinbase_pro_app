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

        // schedule the start of the service every 1 - 5 minutes
        fun scheduleCustomAlertJob(context: Context) {
            val serviceComponent = ComponentName(context, AlertJobService::class.java)
            val builder = JobInfo.Builder(0, serviceComponent)
            builder.setMinimumLatency((TimeInSeconds.oneMinute * 1000)) // wait at least
            builder.setOverrideDeadline((TimeInSeconds.fiveMinutes * 1000)) // maximum delay
            //builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED); // require unmetered network
            //builder.setRequiresDeviceIdle(true); // device should be idle
            //builder.setRequiresCharging(false); // we don't care if the device is charging or not
            val jobScheduler = context.getSystemService(JobScheduler::class.java)
            jobScheduler!!.schedule(builder.build())
        }

//        fun scheduleRapidMovementJob(context: Context) {
//            val serviceComponent = ComponentName(context, RapidMovementJobService::class.java)
//            val builder = JobInfo.Builder(0, serviceComponent)
//            builder.setMinimumLatency((300 * 1000).toLong()) // wait at least
//            builder.setOverrideDeadline((600 * 1000).toLong()) // maximum delay
//            //builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED); // require unmetered network
//            //builder.setRequiresDeviceIdle(true); // device should be idle
//            //builder.setRequiresCharging(false); // we don't care if the device is charging or not
//            val jobScheduler = context.getSystemService(JobScheduler::class.java)
//            jobScheduler!!.schedule(builder.build())
//        }
    }
}

//class RapidMovementJobService : JobService() {
//    val apiInitData = CBProApi.CBProApiInitData(this, { })
//
//    override fun onStartJob(params: JobParameters): Boolean {
//        if (Account.cryptoAccounts.isEmpty()) {
//            CBProApi.accounts(apiInitData).getAllAccountInfo({ /* do nothing*/ }, {
//                checkPriceChanges()
//            })
//        } else {
//            checkPriceChanges()
//        }
//
//        AutoStart.scheduleRapidMovementJob(applicationContext) // reschedule the job
//        return true
//    }
//
//    override fun onStopJob(params: JobParameters): Boolean {
//        return true
//    }
//
//    companion object {
//        private val TAG = "SyncService"
//    }
//
//
//    fun percentChange(start: Double, end: Double) : Double {
//        val change = end - start
//        val percentChange = (change / start) * 100.0
//        return percentChange.absoluteValue
//    }
//}

class AlertJobService : JobService() {
    val apiInitData = CBProApi.CBProApiInitData(this) { }

    override fun onStartJob(params: JobParameters): Boolean {
        AlertHub.triggerDummyAlert(this)

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
        if (Prefs(this).areAlertFillsActive) {
            for (account in Account.cryptoAccounts) {
                val stashedOrders = prefs.getStashedOrders(account.product.id)
                if (stashedOrders.isNotEmpty()) {
                    CBProApi.listOrders(apiInitData, productId = account.product.id).getAndStash({ }) { }
                    CBProApi.fills(apiInitData, productId = account.product.id).getAndStash({ }) {
                        //TODO: trigger alerts? or is that already done elsewhere
                    }
                }
            }
        }
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return true
    }

    private fun updatePrices(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        var tickersUpdated = 0
        var candlesUpdated = 0
        val accountListSize = Account.cryptoAccounts.size
        for (account in Account.cryptoAccounts) {
            account.product.updateCandles(timespan, null, apiInitData, onFailure) { _ ->
                candlesUpdated++
                if (candlesUpdated == accountListSize) {
                    onComplete()
                }
            }

//            CBProApi.ticker(apiInitData, account.product.id).get(onFailure) {
//                tickersUpdated++
//                if (tickersUpdated == accountListSize) {
//                    onComplete()
//                }
//            }
        }
    }

    val timespan = Timespan.DAY
    var lastMovementAlertTimestamp: Long = 0

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

        val enabledCurrencies = prefs.rapidMovementAlertCurrencies
        for (currency in enabledCurrencies) {
            val account = Account.forCurrency(currency)
            if (account != null) {
                val candles = account.product.candlesForTimespan(Timespan.DAY, null)
                if (candles.size > 12) {
                    val minAlertPercentage = 2.0
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
                    if (halfHourChange > alertPercentage + 1.0 && (lastMovementAlertTimestamp < timestamp - TimeInSeconds.halfHour)) {
                        alertPercentage = halfHourChange
                        alertTime = "half hour"
                        changeIsPositive = mostRecentPrice > halfHourPrice
                    }
                    val hourChange = percentChange(mostRecentPrice, hourPrice)
                    if (hourChange > alertPercentage + 1.0 && (lastMovementAlertTimestamp < timestamp - TimeInSeconds.oneHour)) {
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