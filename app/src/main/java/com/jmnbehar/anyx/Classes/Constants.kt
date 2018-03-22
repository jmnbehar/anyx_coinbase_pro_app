package com.jmnbehar.anyx.Classes

/**
 * Created by jmnbehar on 1/19/2018.
 */

object Constants {
    const val exit = "EXIT"
    const val logout = "LOGOUT"
    const val alertChannelId = "com.jmnbehar.gdax.alerts"
    const val salt = "GdaxApp"
    const val isMobileLoginHelp = "LOGIN_HELP_TYPE"
    const val verifyAmount = "VERIFY_AMOUNT"
    const val verifyCurrency = "VERIFY_CURRENCY"
}


object TimeInSeconds {
    const val halfMinute: Long = 30
    const val oneMinute: Long = 60
    const val fiveMinutes: Long = 300
    const val fifteenMinutes: Long = 900
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

enum class Timespan {
    HOUR,
    DAY,
    WEEK,
    MONTH,
    YEAR,
    ALL;

    override fun toString() : String {
        return when (this) {
            HOUR -> "HOUR"
            DAY -> "DAY"
            WEEK -> "WEEK"
            MONTH -> "MONTH"
            YEAR -> "YEAR"
            ALL -> "ALL"
        }
    }

    fun value(currency: Currency = Currency.USD) : Long {
        return when (this) {
            HOUR -> TimeInSeconds.oneHour
            DAY -> TimeInSeconds.oneDay
            WEEK -> TimeInSeconds.oneWeek
            MONTH -> TimeInSeconds.oneMonth
            YEAR -> TimeInSeconds.oneYear
            ALL -> currency.lifetimeInSeconds
        }
    }

    companion object {
        fun fromLong(value: Long) : Timespan {
            return when (value) {
                TimeInSeconds.oneHour -> HOUR
                TimeInSeconds.oneDay -> DAY
                TimeInSeconds.oneWeek -> WEEK
                TimeInSeconds.oneMonth -> MONTH
                TimeInSeconds.oneYear -> YEAR
                (-1).toLong() -> ALL
                else -> DAY
            }
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
