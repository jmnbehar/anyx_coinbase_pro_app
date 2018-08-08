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
//    //ETC,
    LTC,
    USD,
    EUR,
    GBP;

    override fun toString() : String {
        return when (this) {
            BTC -> "BTC"
            BCH -> "BCH"
            ETH -> "ETH"
//            //ETC -> "ETC"
            LTC -> "LTC"
            USD -> "USD"
            EUR -> "EUR"
            GBP -> "GBP"
        }
    }

    val symbol : String
        get() {
            return when (this) {
                USD -> "$"
                EUR -> "€"
                GBP -> "£"
                else -> ""
            }
        }

    val fullName : String
        get() = when (this) {
            BTC -> "Bitcoin"
            BCH -> "Bitcoin Cash"
            ETH -> "Ethereum"
            //ETC -> "Ethereum Classic"
            LTC -> "Litecoin"
            USD -> "US Dollar"
            EUR -> "Euro"
            GBP -> "Pound sterling"
        }

    val iconId
        get() = when(this) {
            BTC -> R.drawable.icon_btc
            ETH -> R.drawable.icon_eth
            //TODO: replace this:
            //ETC -> R.drawable.icon_eth
            LTC -> R.drawable.icon_ltc
            BCH -> R.drawable.icon_bch
            USD -> R.drawable.icon_usd
            EUR -> R.drawable.icon_eur
            GBP -> R.drawable.icon_gbp
        }

    val feePercentage : Double
        get() {
            return if (this.isFiat) {
                0.003
            } else {
                0.0
            }
        }

    val isFiat : Boolean
        get() = when(this) {
            USD, EUR, GBP -> true
            else -> false
        }

    val minBuyAmount : Double
        get() = when (this) {
            BTC -> .001
            ETH -> .01
            //ETC -> .01
            BCH -> .01
            LTC -> .1
            else -> 1.0
        }

    val minSendAmount : Double
        get() = when (this) {
            BTC -> .0001
            ETH -> .001
            //ETC -> .001
            BCH -> .001
            LTC -> .1
            else -> 100.0
        }

//    private val startDate : Date
//        get() = when (this) {
//            BTC -> GregorianCalendar(2013, Calendar.JANUARY, 1).time
//            ETH -> GregorianCalendar(2015, Calendar.AUGUST, 6).time
//            BCH -> GregorianCalendar(2017, Calendar.JULY, 1).time
//            LTC -> GregorianCalendar(2013, Calendar.JANUARY, 1).time
//            else -> GregorianCalendar(2013, Calendar.JANUARY, 1).time
//        }
//
//    val lifetimeInSeconds : Long
//        get() {
//            val utcTimeZone = TimeZone.getTimeZone("UTC")
//            val now = Calendar.getInstance(utcTimeZone)
//            val nowTime = now.timeInSeconds()
//            val startTime = startDate.time / 1000
//            return (nowTime - startTime)
//        }

    fun colorPrimary(context: Context) : Int {
        val prefs = Prefs(context)
        return if (prefs.isDarkModeOn) {
            when (this) {
                BTC -> ContextCompat.getColor(context, R.color.btc_dk)
                BCH -> ContextCompat.getColor(context, R.color.bch_dk)
                ETH -> ContextCompat.getColor(context, R.color.eth_dk)
                //TODO: change this color
                //ETC -> ContextCompat.getColor(context, R.color.eth_dk)
                LTC -> ContextCompat.getColor(context, R.color.ltc_dk)
                USD,
                EUR,
                GBP -> ContextCompat.getColor(context, R.color.white)
            }
        } else {
            when (this) {
                BTC -> ContextCompat.getColor(context, R.color.btc_light)
                BCH -> ContextCompat.getColor(context, R.color.bch_light)
                ETH -> ContextCompat.getColor(context, R.color.eth_light)
                //TODO: change this color
                //ETC -> ContextCompat.getColor(context, R.color.eth_dk)
                LTC -> ContextCompat.getColor(context, R.color.ltc_light)
                USD,
                EUR,
                GBP -> ContextCompat.getColor(context, R.color.black)
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
                //TODO: change this color
                //ETC -> ContextCompat.getColor(context, R.color.eth_accent)
                LTC -> ContextCompat.getColor(context, R.color.ltc_accent)
                USD,
                EUR,
                GBP -> ContextCompat.getColor(context, R.color.white)
            }
        } else {
            when (this) {
                BTC -> ContextCompat.getColor(context, R.color.btc_accent)
                BCH -> ContextCompat.getColor(context, R.color.bch_accent)
                ETH -> ContextCompat.getColor(context, R.color.eth_accent)
                //TODO: change this color
                //ETC -> ContextCompat.getColor(context, R.color.eth_accent)
                LTC -> ContextCompat.getColor(context, R.color.ltc_accent)
                USD,
                EUR,
                GBP -> ContextCompat.getColor(context, R.color.black)
            }
        }
    }

    fun colorStateList(context: Context) : ColorStateList {
        val prefs = Prefs(context)
        return if (prefs.isDarkModeOn) {
            when (this) {
                BTC -> context.resources.getColorStateList(R.color.btc_color_state_list_dark, context.resources.newTheme())
                ETH -> context.resources.getColorStateList(R.color.eth_color_state_list_dark, context.resources.newTheme())
                //TODO: change this color
                //ETC -> context.resources.getColorStateList(R.color.eth_color_state_list_dark, context.resources.newTheme())
                BCH -> context.resources.getColorStateList(R.color.bch_color_state_list_dark, context.resources.newTheme())
                LTC -> context.resources.getColorStateList(R.color.ltc_color_state_list_dark, context.resources.newTheme())
                USD,
                EUR,
                GBP -> context.resources.getColorStateList(R.color.usd_color_state_list_dark, context.resources.newTheme())
            }
        } else {
            when (this) {
                BTC -> context.resources.getColorStateList(R.color.btc_color_state_list_light, context.resources.newTheme())
                ETH -> context.resources.getColorStateList(R.color.eth_color_state_list_light, context.resources.newTheme())
                //TODO: change this color
                //ETC -> context.resources.getColorStateList(R.color.eth_color_state_list_dark, context.resources.newTheme())
                BCH -> context.resources.getColorStateList(R.color.bch_color_state_list_light, context.resources.newTheme())
                LTC -> context.resources.getColorStateList(R.color.ltc_color_state_list_light, context.resources.newTheme())
                USD,
                EUR,
                GBP -> context.resources.getColorStateList(R.color.usd_color_state_list_light, context.resources.newTheme())
            }
        }
    }

    fun buttonTextColor(context: Context) : Int {
        val prefs = Prefs(context)
        return if (prefs.isDarkModeOn) {
            when (this) {
                BTC -> Color.BLACK
                ETH -> Color.WHITE
                //ETC -> Color.WHITE
                BCH -> Color.WHITE
                LTC -> Color.BLACK
                USD,
                EUR,
                GBP -> Color.WHITE
            }
        } else {
            when (this) {
                BTC -> Color.WHITE
                ETH -> Color.WHITE
                //ETC -> Color.WHITE
                BCH -> Color.WHITE
                LTC -> Color.WHITE
                USD,
                EUR,
                GBP -> Color.BLACK
            }
        }
    }

    val developerAddress : String
        get() = when (this) {
        //paper wallets: (Messiah)
            BTC -> "1E9yDtPcWMJESXLjQFCZoZfNeTB3oxiq7o"
            ETH -> "0xAA75018336e91f3b621205b8cbdf020304052b5a"
            //ETC -> ""   //TODO: fix this asap
            BCH -> "1E9yDtPcWMJESXLjQFCZoZfNeTB3oxiq7o"
            LTC -> "LgASuiijykWJAM3i3E3Ke2zEfhemkYaVxi"
            USD,
            EUR,
            GBP -> "my irl address?"
        }

    val verificationAddress : String
        get() = when (this) {
        //paper wallets: (Messiah)
            BTC -> "3QYWtcfgu8W8p43rmJAufiGABhYtq7b8F2"
            ETH -> "0x497125cf15da8F397cc33693434125Db50D659bc"
            //ETC -> ""   //TODO: fix this asap
            BCH -> "qzqj4dxr2m20ys99x2jkuyq90d4q9q6jzq985ey2j7"
            LTC -> "LgASuiijykWJAM3i3E3Ke2zEfhemkYaVxi"
            USD,
            EUR,
            GBP -> "my irl address?"
        }


    val orderValue : Int
        get() {
            return when (this) {
                BTC -> 5
                ETH -> 4
                LTC -> 3
                BCH -> 2
                //ETC -> 1
                EUR -> -1
                GBP -> -2
                USD -> -100
            }
        }

    companion object {
        val cryptoList = Currency.values().filter { !it.isFiat }
        val fiatList = Currency.values().filter { it.isFiat }

        fun forString(string: String) : Currency? {
            return when (string) {
                "BTC" -> BTC
                "BCH" -> BCH
                "ETH" -> ETH
               // "ETC" -> ETC
                "LTC" -> LTC
                "USD" -> USD
                "EUR" -> EUR
                "GBP" -> GBP
                else -> null
            }
        }
    }
}