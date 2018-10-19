package com.anyexchange.anyx.classes

import com.anyexchange.anyx.R

enum class Exchange {
    CBPro,
    Binance;

    override fun toString(): String {
        return when (this) {
            CBPro -> "Coinbase Pro"
            Binance -> "Binance"
        }
    }

    val iconId
        get() = when(this) {
            //TODO: switch to binance logo
            Binance -> R.drawable.icon_btc
            CBPro -> R.drawable.gdax
        }
}