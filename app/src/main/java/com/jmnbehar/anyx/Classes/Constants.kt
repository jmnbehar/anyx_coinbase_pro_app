package com.jmnbehar.anyx.Classes

/**
 * Created by jmnbehar on 1/19/2018.
 */

object Constants {
    val exit = "EXIT"
    val logout = "LOGOUT"
    val alertChannelId = "com.jmnbehar.gdax.alerts"
    val salt = "GdaxApp"
    val isMobileLoginHelp = "LOGIN_HELP_TYPE"
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
            HOUR -> "buy"
            DAY -> "buy"
            WEEK -> "buy"
            MONTH -> "buy"
            YEAR -> "buy"
            ALL -> "buy"
        }
    }

    fun value() : Long {
        return when (this) {
            HOUR -> TimeInSeconds.oneHour
            DAY -> TimeInSeconds.oneDay
            WEEK -> TimeInSeconds.oneWeek
            MONTH -> TimeInSeconds.oneMonth
            YEAR -> TimeInSeconds.oneYear
            ALL -> -1
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
