package com.jmnbehar.gdax.Classes

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.support.v4.content.ContextCompat
import com.jmnbehar.gdax.Fragments.TradeFragment
import com.jmnbehar.gdax.R
import org.jetbrains.anko.backgroundColor
import java.util.*

/**
 * Created by jmnbehar on 1/19/2018.
 */

enum class Currency {
    BTC,
    BCH,
    ETH,
    LTC,
    USD;

    override fun toString() : String {
        return when (this) {
            BTC -> "BTC"
            BCH -> "BCH"
            ETH -> "ETH"
            LTC -> "LTC"
            USD -> "USD"
        }
    }

    val productId : String
        get() = when (this) {
            BTC -> "BTC-USD"
            BCH -> "BCH-USD"
            ETH -> "ETH-USD"
            LTC -> "LTC-USD"
            USD -> "USD"
        }

    val fullName : String
        get() = when (this) {
            BTC -> "Bitcoin"
            BCH -> "Bitcoin Cash"
            ETH -> "Ethereum"
            LTC -> "Litecoin"
            USD -> "USD"
        }

    val iconId
        get() = when(this) {
            BTC -> R.drawable.icon_btc
            ETH ->  R.drawable.icon_eth
            LTC -> R.drawable.icon_ltc
            BCH -> R.drawable.icon_bch
            USD -> R.drawable.icon_usd
        }

    val feePercentage : Double
        get() = when(this) {
            Currency.BTC -> 0.0025
            Currency.BCH -> 0.0025
            Currency.ETH -> 0.003
            Currency.LTC -> 0.003
            Currency.USD -> 0.0
        }


    val minSendAmount : Double
        get() = when (this) {
            Currency.BTC -> .0001
            Currency.ETH -> .001
            Currency.BCH -> .001
            Currency.LTC -> .1
            else -> 100.0
        }

    private val startDate : Date
        get() = when (this) {
            Currency.BTC -> GregorianCalendar(2013, Calendar.JANUARY, 1).time
            Currency.ETH -> GregorianCalendar(2015, Calendar.AUGUST, 6).time
            Currency.BCH -> GregorianCalendar(2017, Calendar.JULY, 1).time
            Currency.LTC -> GregorianCalendar(2013, Calendar.JANUARY, 1).time
            else         -> GregorianCalendar(2013, Calendar.JANUARY, 1).time
        }

    fun colorPrimary(context: Context) : Int {
        val prefs = Prefs(context)
        return if (prefs.isDarkModeOn) {
            when (this) {
                Currency.BTC -> ContextCompat.getColor(context, R.color.btc_dk)
                Currency.BCH -> ContextCompat.getColor(context, R.color.bch_dk)
                Currency.ETH -> ContextCompat.getColor(context, R.color.eth_dk)
                Currency.LTC -> ContextCompat.getColor(context, R.color.ltc_dk)
                Currency.USD -> ContextCompat.getColor(context, R.color.white)
            }
        } else {
            when (this) {
                Currency.BTC -> ContextCompat.getColor(context, R.color.btc_light)
                Currency.BCH -> ContextCompat.getColor(context, R.color.bch_light)
                Currency.ETH -> ContextCompat.getColor(context, R.color.eth_light)
                Currency.LTC -> ContextCompat.getColor(context, R.color.ltc_light)
                Currency.USD -> ContextCompat.getColor(context, R.color.black)
            }
        }
    }

    fun colorAccent(context: Context) : Int {
        val prefs = Prefs(context)
        return if (prefs.isDarkModeOn) {
            when (this) {
                Currency.BTC -> ContextCompat.getColor(context, R.color.btc_accent)
                Currency.BCH -> ContextCompat.getColor(context, R.color.bch_accent)
                Currency.ETH -> ContextCompat.getColor(context, R.color.eth_accent)
                Currency.LTC -> ContextCompat.getColor(context, R.color.ltc_accent)
                Currency.USD -> ContextCompat.getColor(context, R.color.white)
            }
        } else {
            when (this) {
                Currency.BTC -> ContextCompat.getColor(context, R.color.btc_accent)
                Currency.BCH -> ContextCompat.getColor(context, R.color.bch_accent)
                Currency.ETH -> ContextCompat.getColor(context, R.color.eth_accent)
                Currency.LTC -> ContextCompat.getColor(context, R.color.ltc_accent)
                Currency.USD -> ContextCompat.getColor(context, R.color.black)
            }
        }
    }

    fun colorStateList(context: Context) : ColorStateList {
        val prefs = Prefs(context)
        return if (prefs.isDarkModeOn) {
            when (this) {
                Currency.BTC -> context.resources.getColorStateList(R.color.btc_color_state_list_dark, context.resources.newTheme())
                Currency.ETH -> context.resources.getColorStateList(R.color.eth_color_state_list_dark, context.resources.newTheme())
                Currency.BCH -> context.resources.getColorStateList(R.color.bch_color_state_list_dark, context.resources.newTheme())
                Currency.LTC -> context.resources.getColorStateList(R.color.ltc_color_state_list_dark, context.resources.newTheme())
                Currency.USD -> context.resources.getColorStateList(R.color.ltc_color_state_list_dark, context.resources.newTheme())
            }
        } else {
            when (this) {
                Currency.BTC -> context.resources.getColorStateList(R.color.btc_color_state_list_light, context.resources.newTheme())
                Currency.ETH -> context.resources.getColorStateList(R.color.eth_color_state_list_light, context.resources.newTheme())
                Currency.BCH -> context.resources.getColorStateList(R.color.bch_color_state_list_light, context.resources.newTheme())
                Currency.LTC -> context.resources.getColorStateList(R.color.ltc_color_state_list_light, context.resources.newTheme())
                Currency.USD -> context.resources.getColorStateList(R.color.ltc_color_state_list_light, context.resources.newTheme())
            }
        }
    }
    fun buttonTextColor(context: Context) : Int {
        val prefs = Prefs(context)
        return if (prefs.isDarkModeOn) {
            when (this) {
                Currency.BTC -> Color.WHITE
                Currency.ETH -> Color.WHITE
                Currency.BCH -> Color.WHITE
                Currency.LTC -> Color.BLACK
                Currency.USD -> Color.BLACK
            }
        } else {
            when (this) {
                Currency.BTC -> Color.WHITE
                Currency.ETH -> Color.WHITE
                Currency.BCH -> Color.WHITE
                Currency.LTC -> Color.WHITE
                Currency.USD -> Color.WHITE
            }
        }
    }
    val lifetimeInSeconds : Long
        get() {
            val utcTimeZone = TimeZone.getTimeZone("UTC")
            val now = Calendar.getInstance(utcTimeZone)
            val nowTime = now.timeInSeconds()
            val startTime = startDate.time / 1000
            return now.timeInSeconds() - startTime
        }

    companion object {
        fun fromString(string: String) : Currency {
            return when (string) {
                "BTC-USD" -> BTC
                "BTC" -> BTC
                "BCH-USD" -> BCH
                "BCH" -> BCH
                "ETH-USD" -> ETH
                "ETH" -> ETH
                "LTC-USD" -> LTC
                "LTC" -> LTC
                "USD" -> USD
                else -> USD
            }
        }
    }
}