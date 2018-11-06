package com.anyexchange.anyx.classes

import java.util.*

enum class TimeInForce {
    GoodTilCancelled,
    GoodTilTime,
    ImmediateOrCancel,
    FirstOrKill;

    override fun toString(): String {
        return when (this) {
            GoodTilCancelled -> "GTC"
            GoodTilTime -> "GTT"
            ImmediateOrCancel -> "IOC"
            FirstOrKill -> "FOK"
        }
    }
    fun userFriendlyString(endDate: Date? = null): String {
        return when (this) {
            GoodTilCancelled -> "Good until cancelled"
            GoodTilTime -> if (endDate == null) {
                "Good until time"
            } else {
                val formattedDate = endDate.format(Fill.dateFormat)
                "Good until time:\n$formattedDate"
            }
            ImmediateOrCancel -> "Immediate or cancel"
            FirstOrKill -> "First or kill"
        }
    }

    companion object {
        fun forString(string: String?) : TimeInForce? {
            if (string == null) {
                return null
            }
            for (value in TimeInForce.values()) {
                if (value.toString() == string) {
                    return value
                }
            }
            return null
        }
    }
    fun label(): String {
        return when (this) {
            GoodTilCancelled -> "Good Til Cancelled"
            GoodTilTime -> "Good Til Time"
            ImmediateOrCancel -> "Immediate Or Cancel"
            FirstOrKill -> "First Or Kill"
        }
    }

    enum class CancelAfter {
        Minute,
        Hour,
        Day;

        override fun toString(): String {
            return when (this) {
                Minute -> "min"
                Hour -> "hour"
                Day -> "day"
            }
        }
    }
}