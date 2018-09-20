package com.anyexchange.anyx.classes

enum class Exchange {
    CBPro,
    Binance;

    override fun toString(): String {
        return when (this) {
            CBPro -> "Coinbase Pro"
            Binance -> "Binance"
        }
    }
}