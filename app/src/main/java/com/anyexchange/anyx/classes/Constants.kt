package com.anyexchange.anyx.classes

/**
 * Created by anyexchange on 1/19/2018.
 */

object Constants {
    const val salt = "GdaxApp"  //DO NOT RENAME
    const val isMobileLoginHelp = "LOGIN_HELP_TYPE"

    const val CHART_CURRENCY = "CHART_CURRENCY"
    const val CHART_TRADING_PAIR = "CHART_TRADING_PAIR"
    const val CHART_STYLE = "CHART_STYLE"
    const val CHART_TIMESPAN = "CHART_TIMESPAN"

    const val GO_TO_CURRENCY = "GO_TO_CURRENCY"

    const val dataFragmentTag = "data"
}


val DEV_FEE_PERCENTAGE : Double
    get() {
        return 0.001
    }

val defaultVerificationCurrency = Currency.ETH

object TimeInSeconds {
    const val halfMinute: Long = 30
    const val oneMinute: Long = 60
    const val fiveMinutes: Long = 300
    const val fifteenMinutes: Long = 900
    const val twentyMinutes: Long = 1200
    const val thirtyMinutes: Long = 1800
    const val halfHour: Long = 1800
    const val oneHour: Long = 3600
    const val sixHours: Long = 21600
    const val oneDay: Long = 86400
    const val oneWeek: Long = 604800
    const val twoWeeks: Long = 1209600
    const val oneMonth: Long = 2592000
    const val oneYear: Long = 31536000
    const val fiveYears: Long = 158112000
}
object TimeInMillis {
    const val threeSeconds: Long = 3 * 1000
    const val tenSeconds: Long = 10 * 1000
    const val halfMinute: Long = 30 * 1000
    const val oneMinute: Long = 60 * 1000
    const val fiveMinutes: Long = 300 * 1000
    const val eightMinutes: Long = 60 * 8 * 1000
    const val tenMinutes: Long = 600 * 1000
    const val fifteenMinutes: Long = 900 * 1000
    const val twentyMinutes: Long = 1200 * 1000
    const val thirtyMinutes: Long = 1800 * 1000
    const val halfHour: Long = 1800 * 1000
    const val oneHour: Long = 3600 * 1000
    const val sixHours: Long = 21600 * 1000
    const val oneDay: Long = 86400 * 1000
    const val oneWeek: Long = 604800 * 1000
    const val twoWeeks: Long = 1209600 * 1000
}

enum class Timespan {
    HOUR,
    DAY,
    WEEK,
    MONTH,
    YEAR;

    override fun toString() : String {
        return when (this) {
            HOUR -> "HOUR"
            DAY -> "DAY"
            WEEK -> "WEEK"
            MONTH -> "MONTH"
            YEAR -> "YEAR"
//            ALL -> "ALL"
        }
    }

    fun value() : Long {
        return when (this) {
            HOUR -> TimeInSeconds.oneHour
            DAY -> TimeInSeconds.oneDay
            WEEK -> TimeInSeconds.oneWeek
            MONTH -> TimeInSeconds.oneMonth
            YEAR -> TimeInSeconds.oneYear
        }
    }
}

object Granularity {
    const val oneMinute: Long = 60
    const val fiveMinutes: Long = 300
    const val fifteenMinutes: Long = 900
    const val oneHour: Long = 3600
    const val sixHours: Long = 21600
    const val oneDay: Long = 86400
}
