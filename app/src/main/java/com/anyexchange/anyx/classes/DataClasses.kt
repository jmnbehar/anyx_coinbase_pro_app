package com.anyexchange.anyx.classes

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


/**
 * Created by anyexchange on 12/19/2017.
 */

enum class TradeSide {
    BUY,
    SELL;

    override fun toString() : String {
        return when (this) {
            BUY -> "buy"
            SELL -> "sell"
        }
    }
    companion object {
        fun forString(string: String) : TradeSide {
            return when (string) {
                "buy"  -> BUY
                "sell" -> SELL
                else   -> BUY
            }
        }
    }
}

enum class TradeType {
    MARKET,
    LIMIT,
    STOP;

    override fun toString() : String {
        return when (this) {
            MARKET -> "market"
            LIMIT -> "limit"
            STOP -> "stop"
        }
    }
    companion object {
        fun forString(string: String) : TradeType {
            return when (string) {
                "market"  ->  MARKET
                "limit"  -> LIMIT
                "stop" -> STOP
                else -> MARKET
            }
        }
    }
}

