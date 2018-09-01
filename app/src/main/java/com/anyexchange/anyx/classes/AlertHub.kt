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
        postAlert("Dummy", "AnyX Ran Alerts", "Tested Alerts at $date", "Dummy_${date.time}", null, context)
    }

    fun triggerQuickChangeAlert(alert: QuickChangeAlert, context: Context) {
        val channelId = "Change_Alerts"
        val currency = if (alert.currencies.size == 1) {
            alert.currencies.first()
        }  else { null }
        postAlert(channelId, alert.title, alert.text, alert.tag, currency, context)
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

        postAlert(channelId, notificationTitle, notificationText, notificationTag, tradingPair.baseCurrency, context)
    }

    fun triggerPriceAlert(alert: PriceAlert, context: Context) {
        val channelId = "Price_Alerts"

        postAlert(channelId, alert.title, alert.text, alert.tag, alert.currency, context)
        val prefs = Prefs(context)
        prefs.removeAlert(alert)
    }

    private fun postAlert(channelId: String, title: String, text: String, tag: String, goToCurrency: Currency?, context: Context) {
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
        intent.putExtra(Constants.GO_TO_CURRENCY, goToCurrency?.toString())

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