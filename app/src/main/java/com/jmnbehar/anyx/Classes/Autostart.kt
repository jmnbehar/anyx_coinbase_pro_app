package com.jmnbehar.anyx.Classes

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
import com.jmnbehar.anyx.R
import android.app.job.JobScheduler
import android.app.job.JobInfo
import android.content.ComponentName
import android.app.job.JobParameters
import android.app.job.JobService


class AutoStart : BroadcastReceiver() {
    override fun onReceive(context: Context, arg1: Intent) {
        AlertUtil.scheduleJob(context)
    }
}


object AlertUtil {

    // schedule the start of the service every 10 - 30 seconds
    fun scheduleJob(context: Context) {
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

}

class AlertJobService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        if (Account.list.isEmpty()) {
            GdaxApi.accounts().getAllAccountInfo(this, { /* do nothing*/ }, {
                loopThroughAlerts()
            })
        } else {
            updatePrices({ /* do nothing*/ }, {
                loopThroughAlerts()
            })
        }
        AlertUtil.scheduleJob(applicationContext) // reschedule the job
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return true
    }

    companion object {
        private val TAG = "SyncService"
    }


    fun updatePrices(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        var tickersUpdated = 0
        val accountListSize = Account.list.size
        for (account in Account.list) {
            GdaxApi.ticker(account.product.id).executeRequest(onFailure) { result ->
                val ticker: ApiTicker = Gson().fromJson(result.value, object : TypeToken<ApiTicker>() {}.type)
                val price = ticker.price.toDoubleOrNull()
                if (price != null) {
                    account.product.price = price
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
                var currentPrice = Account.forCurrency(alert.currency)?.product?.price
                if (alert.triggerIfAbove && (currentPrice != null) && (currentPrice >= alert.price)) {
                    triggerAlert(alert)
                } else if (!alert.triggerIfAbove && (currentPrice != null) && (currentPrice <= alert.price)) {
                    triggerAlert(alert)
                }
            }
        }
    }

    private var notificationManager: NotificationManager? = null

    private fun triggerAlert(alert: Alert) {
        val CHANNEL_ID = "Price_Alerts"
        if (notificationManager == null) {
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create the NotificationChannel, but only on API 26+ because
                // the NotificationChannel class is new and not in the support library
                val name = getString(R.string.channel_name)
                val description = getString(R.string.channel_description)
                val importance = NotificationManagerCompat.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, name, importance)
                channel.description = description
                // Register the channel with the system
                notificationManager?.createNotificationChannel(channel)
            }
        }


        val overUnder = when(alert.triggerIfAbove) {
            true  -> "over"
            false -> "under"
        }
        val intent = Intent(this, this.javaClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationTitle = "${alert.currency.fullName} price alert"
        val notificationText = "${alert.currency} is $overUnder ${alert.price.fiatFormat()}"
        val priceAlertGroupTag = "PriceAlert"

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.anyx_notification_icon)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setGroup(priceAlertGroupTag)
//                .setSound(defaultSoundUri)

        val notificationTag = "PriceAlert_" + alert.currency.toString() + "_" + alert.price
        notificationManager?.notify(notificationTag, 0, notificationBuilder.build())
        val prefs = Prefs(this)
        prefs.removeAlert(alert)
    }
}