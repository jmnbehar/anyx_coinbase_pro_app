package com.anyexchange.anyx.classes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.anyexchange.anyx.R
import android.app.job.JobScheduler
import android.app.job.JobInfo
import android.content.ComponentName
import android.app.job.JobParameters
import android.app.job.JobService
import java.util.*
import kotlin.math.absoluteValue


class AutoStart : BroadcastReceiver() {
    override fun onReceive(context: Context, arg1: Intent) {
        AlertUtil.scheduleCustomAlertJob(context)
    }
}


object AlertUtil {

    // schedule the start of the service every 10 - 30 seconds
    fun scheduleCustomAlertJob(context: Context) {
        val serviceComponent = ComponentName(context, AlertJobService::class.java)
        val builder = JobInfo.Builder(0, serviceComponent)
        builder.setMinimumLatency((30 * 1000).toLong()) // wait at least
        builder.setOverrideDeadline((60 * 1000).toLong()) // maximum delay
        //builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED); // require unmetered network
        //builder.setRequiresDeviceIdle(true); // device should be idle
        //builder.setRequiresCharging(false); // we don't care if the device is charging or not
        val jobScheduler = context.getSystemService(JobScheduler::class.java)
        jobScheduler!!.schedule(builder.build())
    }

    fun scheduleRapidMovementJob(context: Context) {
        val serviceComponent = ComponentName(context, RapidMovementJobService::class.java)
        val builder = JobInfo.Builder(0, serviceComponent)
        builder.setMinimumLatency((300 * 1000).toLong()) // wait at least
        builder.setOverrideDeadline((600 * 1000).toLong()) // maximum delay
        //builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED); // require unmetered network
        //builder.setRequiresDeviceIdle(true); // device should be idle
        //builder.setRequiresCharging(false); // we don't care if the device is charging or not
        val jobScheduler = context.getSystemService(JobScheduler::class.java)
        jobScheduler!!.schedule(builder.build())
    }

    private var notificationManager: NotificationManager? = null

    fun postNotification(title: String, message: String, groupTag: String, notificationTag: String, context: Context) {
        val CHANNEL_ID = "Price_Alerts"
        if (notificationManager == null) {
            notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create the NotificationChannel, but only on API 26+ because
                // the NotificationChannel class is new and not in the support library
                val name = context.getString(R.string.channel_name)
                val description = context.getString(R.string.channel_description)
                val importance = NotificationManagerCompat.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, name, importance)
                channel.description = description
                // Register the channel with the system
                notificationManager?.createNotificationChannel(channel)
            }
        }

        val intent = Intent(context, this.javaClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.anyx_notification_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setGroup(groupTag)
//                .setSound(defaultSoundUri)

        notificationManager?.notify(notificationTag, 0, notificationBuilder.build())
    }
}

class RapidMovementJobService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        if (Account.list.isEmpty()) {
            CBProApi.accounts().getAllAccountInfo(this, { /* do nothing*/ }, {
                checkPriceChanges()
            })
        } else {
            checkPriceChanges()
        }

        AlertUtil.scheduleRapidMovementJob(applicationContext) // reschedule the job
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return true
    }

    companion object {
        private val TAG = "SyncService"
    }

    var lastAlertTimestamp: Long = 0
    fun checkPriceChanges() {
        val prefs = Prefs(this)
        val enabledCurrencies = prefs.rapidMovementAlerts
        for (account in Account.list) {
            if (enabledCurrencies.contains(account.currency)) {
                //TODO: consider switching to hour timespan
                val timeSpan = Timespan.DAY
                account.product.updateCandles(timeSpan, { }, { didUpdate ->
                    val candles = account.product.candlesForTimespan(timeSpan)
                    if (didUpdate && candles.size > 12) {
                        var alertPercentage = 1.0
                        var alertTime = ""
                        var changeIsPositive = false
                        val timestamp = Date().timeInSeconds()

                        val mostRecentPrice = candles.last().close
                        val twentyMinutePrice = candles[candles.size - 4].close
                        val halfHourPrice = candles[candles.size - 6].close
                        val hourPrice = candles[candles.size - 12].close

                        val twentyMinuteChange = percentChange(mostRecentPrice, twentyMinutePrice)
                        if (twentyMinuteChange > 2.0 && (lastAlertTimestamp < timestamp - (20 * 60))) {
                            alertPercentage = twentyMinuteChange
                            alertTime = "twenty minutes"
                            changeIsPositive = mostRecentPrice > twentyMinutePrice
                        }
                        val halfHourChange = percentChange(mostRecentPrice, halfHourPrice)
                        if (halfHourChange > alertPercentage + 1.0 && (lastAlertTimestamp < timestamp - (30 * 60))) {
                            alertPercentage = halfHourChange
                            alertTime = "half hour"
                            changeIsPositive = mostRecentPrice > halfHourPrice
                        }
                        val hourChange = percentChange(mostRecentPrice, hourPrice)
                        if (hourChange > alertPercentage + 1.0 && (lastAlertTimestamp < timestamp - (60 * 60))) {
                            alertPercentage = hourChange
                            alertTime = "hour"
                            changeIsPositive = mostRecentPrice > hourPrice
                        }
                        if (alertPercentage > 2.0) {
                            val currency = account.currency
                            val notificationTitle = "${currency.fullName} price alert"

                            val upDown = when(changeIsPositive) {
                                true  -> "up"
                                false -> "down"
                            }
                            val notificationText = "$currency is $upDown ${alertPercentage.percentFormat()} in the past $alertTime"
                            val priceAlertGroupTag = "PriceMovementAlert"

                            lastAlertTimestamp = timestamp

                            val notificationTag = "PriceMovementAlert_" + currency.toString() + "_" + timestamp.toString()

                            AlertUtil.postNotification(notificationTitle, notificationText, priceAlertGroupTag, notificationTag, this)
                        }
                    }
                })
            }
        }
    }

    fun percentChange(start: Double, end: Double) : Double {
        val change = end - start
        val percentChange = (change / start) * 100.0
        return percentChange.absoluteValue
    }
}

class AlertJobService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        if (Account.list.isEmpty()) {
            CBProApi.accounts().getAllAccountInfo(this, { /* do nothing*/ }, {
                loopThroughAlerts()
            })
        } else {
            updatePrices({ /* do nothing*/ }, {
                loopThroughAlerts()
            })
        }
        AlertUtil.scheduleCustomAlertJob(applicationContext) // reschedule the job
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return true
    }

    companion object {
        private val TAG = "SyncService"
    }


    private fun updatePrices(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        var tickersUpdated = 0
        val accountListSize = Account.list.size
        for (account in Account.list) {
            CBProApi.ticker(account.product.id).executeRequest(onFailure) { result ->
                if (result.value.isNotBlank()) {
                    val ticker: ApiTicker = Gson().fromJson(result.value, object : TypeToken<ApiTicker>() {}.type)
                    val price = ticker.price.toDoubleOrNull()
                    if (price != null) {
                        account.product.price = price
                    }
                }
                tickersUpdated++
                if (tickersUpdated == accountListSize) {
                    onComplete()
                }
            }
        }
    }

    private fun loopThroughAlerts() {
        val prefs = Prefs(this)
        val alerts = prefs.alerts
        for (alert in alerts) {
            if (!alert.hasTriggered) {
                val currentPrice = Account.forCurrency(alert.currency)?.product?.price
                if (alert.triggerIfAbove && (currentPrice != null) && (currentPrice >= alert.price)) {
                    triggerAlert(alert)
                } else if (!alert.triggerIfAbove && (currentPrice != null) && (currentPrice <= alert.price)) {
                    triggerAlert(alert)
                }
            }
        }
    }


    private fun triggerAlert(alert: Alert) {
        val overUnder = when(alert.triggerIfAbove) {
            true  -> "over"
            false -> "under"
        }
        val notificationTitle = "${alert.currency.fullName} price alert"
        val notificationText = "${alert.currency} is $overUnder ${alert.price.fiatFormat()}"
        val priceAlertGroupTag = "PriceAlert"

        val notificationTag = "PriceAlert_" + alert.currency.toString() + "_" + alert.price

        AlertUtil.postNotification(notificationTitle, notificationText, priceAlertGroupTag, notificationTag, this)
        val prefs = Prefs(this)
        prefs.removeAlert(alert)
    }
}