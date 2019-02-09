package com.anyexchange.anyx.classes

import android.content.Context
import com.anyexchange.anyx.R
import com.anyexchange.anyx.api.BinanceApi
import com.anyexchange.anyx.api.CBProApi

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

    fun isEnabled(context: Context?) : Boolean {
        return when (this) {
            CBPro -> true
            else -> {
                //TODO: make this changable per exchange
                return context?.let {
                    val prefs = Prefs(it)
                    prefs.isAnyXProActive
                } ?: run {
                    false
                }
            }
        }
    }

    fun isLoggedIn() : Boolean {
        return when (this) {
            CBPro -> CBProApi.credentials != null
            Binance -> BinanceApi.credentials != null
        }
    }

    companion object {
        fun isAnyLoggedIn() : Boolean {
            return CBProApi.credentials != null || BinanceApi.credentials != null
        }
    }
}