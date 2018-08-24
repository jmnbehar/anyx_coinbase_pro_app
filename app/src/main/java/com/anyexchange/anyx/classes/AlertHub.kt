package com.anyexchange.anyx.classes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.anyexchange.anyx.R
import com.anyexchange.anyx.activities.MainActivity
import java.util.*

object AlertHub {
    private var notificationManager: NotificationManager? = null


    fun triggerDummyAlert(context: Context) {
        val date = Date()
        triggerAlert("Dummy", "AnyX Ran Alerts", "Tested Alerts at $date", "Dummy_${date.time}", context)
    }

    fun triggerRapidMovementAlert(currency: Currency, percentChange: Double, isChangePositive: Boolean, timespan: String, context: Context) {
        val channelId = "Movement_Alerts"
        val notificationTitle = "${currency.fullName} price alert"

        val upDown = when (isChangePositive) {
            true -> "up"
            false -> "down"
        }
        val notificationText = "$currency is $upDown ${percentChange.percentFormat()} in the past $timespan"

        val notificationTag = "PriceMovementAlert_${currency}_${Date().time}"

        triggerAlert(channelId, notificationTitle, notificationText, notificationTag, context)
    }

    fun triggerFillAlert(fill: ApiFill, context: Context) {
        val channelId = "Fill_Alerts"

        val tradingPair = TradingPair(fill.product_id)
        val side = TradeSide.forString(fill.side)
        val size = fill.size
        val notificationTitle = "${fill.order_id} "
        val price = if (tradingPair.quoteCurrency.isFiat) {
            fill.price.toDoubleOrNull()?.fiatFormat(tradingPair.quoteCurrency)
        } else {
            "${fill.price.toDoubleOrNull()?.btcFormat()} ${tradingPair.quoteCurrency}"
        }
        val notificationText = "$side order of $size ${tradingPair.baseCurrency} filled at $price"
        val notificationTag = "FillAlert_" + fill.trade_id

        triggerAlert(channelId, notificationTitle, notificationText, notificationTag, context)
    }

    fun triggerPriceAlert(alert: Alert, context: Context) {
        val channelId = "Price_Alerts"

        val overUnder = when(alert.triggerIfAbove) {
            true  -> "over"
            false -> "under"
        }
        val notificationTitle = "${alert.currency.fullName} price alert"
        val notificationText = "${alert.currency} is $overUnder ${alert.price.fiatFormat(Account.defaultFiatCurrency)}"
        val notificationTag = "PriceAlert_" + alert.currency.toString() + "_" + alert.price

        triggerAlert(channelId, notificationTitle, notificationText, notificationTag, context)
        val prefs = Prefs(context)
        prefs.removeAlert(alert)
    }

    private fun triggerAlert(channelId: String, title: String, text: String, tag: String, context: Context) {
        if (notificationManager == null) {
            notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create the NotificationChannel, but only on API 26+ because
                // the NotificationChannel class is new and not in the support library
                val name = context.getString(R.string.channel_name)
                val description = context.getString(R.string.channel_description)
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(channelId, name, importance)
                channel.description = description
                // Register the channel with the system
                notificationManager?.createNotificationChannel(channel)
            }
        }


        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
//        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val priceAlertGroupTag = "PriceAlert"

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.anyx_notification_icon)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setGroup(priceAlertGroupTag)
//                .setSound(defaultSoundUri)

        notificationManager?.notify(tag, 0, notificationBuilder.build())
    }
}