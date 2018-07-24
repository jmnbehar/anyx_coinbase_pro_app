package com.anyexchange.anyx.classes

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.support.v4.content.ContextCompat
import com.anyexchange.anyx.R
import java.util.*

/**
 * Created by anyexchange on 1/19/2018.
 */

enum class Currency {
    BTC,
    BCH,
    ETH,
    LTC,
    USD,
    EUR;

    override fun toString() : String {
        return when (this) {
            BTC -> "BTC"
            BCH -> "BCH"
            ETH -> "ETH"
            LTC -> "LTC"
            USD -> "USD"
            EUR -> "EUR"
        }
    }

    val productId : String
        get() = when (this) {
            BTC -> "BTC-USD"
            BCH -> "BCH-USD"
            ETH -> "ETH-USD"
            LTC -> "LTC-USD"
            USD -> "USD"
            EUR -> "EUR"
        }

    val fullName : String
        get() = when (this) {
            BTC -> "Bitcoin"
            BCH -> "Bitcoin Cash"
            ETH -> "Ethereum"
            LTC -> "Litecoin"
            USD -> "USD"
            EUR -> "Euro"
        }

    val iconId
        get() = when(this) {
            BTC -> R.drawable.icon_btc
            ETH ->  R.drawable.icon_eth
            LTC -> R.drawable.icon_ltc
            BCH -> R.drawable.icon_bch
            USD -> R.drawable.icon_usd
            //TODO: replace this:
            EUR -> R.drawable.icon_usd
        }

    val feePercentage : Double
        get() = when(this) {
            BTC -> 0.0025
            BCH -> 0.0025
            ETH -> 0.003
            LTC -> 0.003
            USD -> 0.0
            EUR -> 0.0
        }

    val isFiat : Boolean
        get() = when(this) {
            USD, EUR -> true
            else -> false
        }

    val minBuyAmount : Double
        get() = when (this) {
            BTC -> .001
            ETH -> .01
            BCH -> .01
            LTC -> .1
            else -> 1.0
        }

    val minSendAmount : Double
        get() = when (this) {
            BTC -> .0001
            ETH -> .001
            BCH -> .001
            LTC -> .1
            else -> 100.0
        }

    private val startDate : Date
        get() = when (this) {
            BTC -> GregorianCalendar(2013, Calendar.JANUARY, 1).time
            ETH -> GregorianCalendar(2015, Calendar.AUGUST, 6).time
            BCH -> GregorianCalendar(2017, Calendar.JULY, 1).time
            LTC -> GregorianCalendar(2013, Calendar.JANUARY, 1).time
            else         -> GregorianCalendar(2013, Calendar.JANUARY, 1).time
        }

    fun colorPrimary(context: Context) : Int {
        val prefs = Prefs(context)
        return if (prefs.isDarkModeOn) {
            when (this) {
                BTC -> ContextCompat.getColor(context, R.color.btc_dk)
                BCH -> ContextCompat.getColor(context, R.color.bch_dk)
                ETH -> ContextCompat.getColor(context, R.color.eth_dk)
                LTC -> ContextCompat.getColor(context, R.color.ltc_dk)
                USD,
                EUR -> ContextCompat.getColor(context, R.color.white)
            }
        } else {
            when (this) {
                BTC -> ContextCompat.getColor(context, R.color.btc_light)
                BCH -> ContextCompat.getColor(context, R.color.bch_light)
                ETH -> ContextCompat.getColor(context, R.color.eth_light)
                LTC -> ContextCompat.getColor(context, R.color.ltc_light)
                USD,
                EUR -> ContextCompat.getColor(context, R.color.black)
            }
        }
    }

    fun colorAccent(context: Context) : Int {
        val prefs = Prefs(context)
        return if (prefs.isDarkModeOn) {
            when (this) {
                BTC -> ContextCompat.getColor(context, R.color.btc_accent)
                BCH -> ContextCompat.getColor(context, R.color.bch_accent)
                ETH -> ContextCompat.getColor(context, R.color.eth_accent)
                LTC -> ContextCompat.getColor(context, R.color.ltc_accent)
                USD,
                EUR -> ContextCompat.getColor(context, R.color.white)
            }
        } else {
            when (this) {
                BTC -> ContextCompat.getColor(context, R.color.btc_accent)
                BCH -> ContextCompat.getColor(context, R.color.bch_accent)
                ETH -> ContextCompat.getColor(context, R.color.eth_accent)
                LTC -> ContextCompat.getColor(context, R.color.ltc_accent)
                USD,
                EUR -> ContextCompat.getColor(context, R.color.black)
            }
        }
    }

    fun colorStateList(context: Context) : ColorStateList {
        val prefs = Prefs(context)
        return if (prefs.isDarkModeOn) {
            when (this) {
                BTC -> context.resources.getColorStateList(R.color.btc_color_state_list_dark, context.resources.newTheme())
                ETH -> context.resources.getColorStateList(R.color.eth_color_state_list_dark, context.resources.newTheme())
                BCH -> context.resources.getColorStateList(R.color.bch_color_state_list_dark, context.resources.newTheme())
                LTC -> context.resources.getColorStateList(R.color.ltc_color_state_list_dark, context.resources.newTheme())
                USD,
                EUR -> context.resources.getColorStateList(R.color.usd_color_state_list_dark, context.resources.newTheme())
            }
        } else {
            when (this) {
                BTC -> context.resources.getColorStateList(R.color.btc_color_state_list_light, context.resources.newTheme())
                ETH -> context.resources.getColorStateList(R.color.eth_color_state_list_light, context.resources.newTheme())
                BCH -> context.resources.getColorStateList(R.color.bch_color_state_list_light, context.resources.newTheme())
                LTC -> context.resources.getColorStateList(R.color.ltc_color_state_list_light, context.resources.newTheme())
                USD,
                EUR -> context.resources.getColorStateList(R.color.usd_color_state_list_light, context.resources.newTheme())
            }
        }
    }

    fun buttonTextColor(context: Context) : Int {
        val prefs = Prefs(context)
        return if (prefs.isDarkModeOn) {
            when (this) {
                BTC -> Color.BLACK
                ETH -> Color.WHITE
                BCH -> Color.WHITE
                LTC -> Color.BLACK
                USD,
                EUR -> Color.WHITE
            }
        } else {
            when (this) {
                BTC -> Color.WHITE
                ETH -> Color.WHITE
                BCH -> Color.WHITE
                LTC -> Color.WHITE
                USD,
                EUR -> Color.BLACK
            }
        }
    }

    val developerAddress : String
        get() = when (this) {
        //paper wallets: (Messiah)
            BTC -> "1E9yDtPcWMJESXLjQFCZoZfNeTB3oxiq7o"
            ETH -> "0xAA75018336e91f3b621205b8cbdf020304052b5a"
            BCH -> "1E9yDtPcWMJESXLjQFCZoZfNeTB3oxiq7o"
            LTC -> "LgASuiijykWJAM3i3E3Ke2zEfhemkYaVxi"
            USD,
            EUR -> "my irl address?"
        }

    val verificationAddress : String
        get() = when (this) {
        //paper wallets: (Messiah)
            BTC -> "3QYWtcfgu8W8p43rmJAufiGABhYtq7b8F2"
            ETH -> "0x497125cf15da8F397cc33693434125Db50D659bc"
            BCH -> "qzqj4dxr2m20ys99x2jkuyq90d4q9q6jzq985ey2j7"
            LTC -> "LgASuiijykWJAM3i3E3Ke2zEfhemkYaVxi"
            USD,
            EUR -> "my irl address?"
        }

    val lifetimeInSeconds : Long
        get() {
            val utcTimeZone = TimeZone.getTimeZone("UTC")
            val now = Calendar.getInstance(utcTimeZone)
            val nowTime = now.timeInSeconds()
            val startTime = startDate.time / 1000
            return (nowTime - startTime)
        }

    val orderValue : Int
        get() {
            return when (this) {
                BTC -> 4
                ETH -> 3
                LTC -> 2
                BCH -> 1
                USD,
                EUR -> 0
            }
        }


    companion object {
        val cryptoList = listOf(BTC, ETH, LTC, BCH)

        fun forString(string: String) : Currency? {
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
                "EUR" -> EUR
                else -> null
            }
        }
    }
}