package com.anyexchange.cryptox.classes

import android.content.Context
import com.anyexchange.cryptox.R
import com.anyexchange.cryptox.api.BinanceApi
import com.anyexchange.cryptox.api.CBProApi

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
            Binance -> R.drawable.binance
            CBPro -> R.drawable.cbpro
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

        fun forString(string: String?) : Exchange? {
            return Exchange.values().find { it.toString() == string || it.name == string }
        }
    }
}