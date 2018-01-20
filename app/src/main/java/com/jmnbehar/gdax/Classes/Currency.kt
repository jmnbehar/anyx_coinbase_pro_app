package com.jmnbehar.gdax.Classes

import com.jmnbehar.gdax.R

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

    fun productId() : String {
        return when (this) {
            BTC -> "BTC-USD"
            BCH -> "BCH-USD"
            ETH -> "ETH-USD"
            LTC -> "LTC-USD"
            USD -> "USD"
        }
    }

    var fullName : String = ""
        get() = when (this) {
            BTC -> "Bitcoin"
            BCH -> "Bitcoin Cash"
            ETH -> "Ethereum"
            LTC -> "Litecoin"
            USD -> "USD"
        }

    var iconId = 0
        get() = when(this) {
            BTC -> R.drawable.icon_btc
            ETH ->  R.drawable.icon_eth
            LTC -> R.drawable.icon_ltc
            BCH -> R.drawable.icon_bch
            USD -> R.drawable.icon_usd
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