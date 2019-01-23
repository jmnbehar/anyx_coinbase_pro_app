package com.anyexchange.anyx.classes

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import com.anyexchange.anyx.R

/**
 * Created by anyexchange on 1/19/2018.
 */

class Currency(val id: String) {
    constructor(knownCurrency: KnownCurrency): this(knownCurrency.toString())
    private val knownCurrency = KnownCurrency.forString(id)

    override fun toString() : String {
        return id
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Currency) {
            (other.id == this.id)
        } else {
            false
        }
    }
    val symbol
        get() = knownCurrency?.symbol ?: id

    val fullName
        get() = knownCurrency?.fullName ?: id

    val iconId
        get() = knownCurrency?.iconId

    val feePercentage : Double
        get() {
            return if (type == Type.CRYPTO) {
                0.003
            } else {
                0.0
            }
        }

    val type: Type
        get() { return knownCurrency?.type ?: Type.CRYPTO }
    val isFiat: Boolean
        get() { return type == Type.FIAT }

    val minSendAmount: Double = knownCurrency?.minSendAmount ?: 0.0

    fun addToList() {
        if (type == Type.CRYPTO && !cryptoList.any { it.id == this.id }) {
            cryptoList.add(this)
        }
    }

    fun colorPrimary(context: Context) : Int {
        return knownCurrency?.colorPrimary(context) ?: run {
            if (Prefs(context).isDarkModeOn) {
                Color.WHITE
            } else {
                Color.BLACK
            }
        }
    }

    fun colorStateList(context: Context) : ColorStateList {
        return knownCurrency?.colorStateList(context) ?: run {
            context.resources.getColorStateList(R.color.usd_color_state_list_light, context.resources.newTheme())
        }
    }

    fun buttonTextColor(context: Context) : Int {
        return knownCurrency?.buttonTextColor(context) ?: run {
            if (Prefs(context).isDarkModeOn) {
                Color.BLACK
            } else {
                Color.WHITE
            }
        }
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    val developerAddress = knownCurrency?.developerAddress

    //Order Value sucks and should be removed whenever possible
    val orderValue: Int
        get() {
            return knownCurrency?.orderValue ?: 99999
        }

    val relevantStableCoin: Currency?
        get() = knownCurrency?.relevantStableCoin

    val relevantFiat : Currency?
        get() = knownCurrency?.relevantFiat


    companion object {
        val cryptoList = KnownCurrency.cryptoList().map { Currency(it) }.toMutableList()
        val fiatList   = KnownCurrency.fiatList().map { Currency(it) }.toMutableList()
        val stableCoinList = listOf(Currency(CurrencyUSDC()))

        val USD = Currency(CurrencyUSD())
        val USDC = Currency(CurrencyUSDC())
        val EUR = Currency(CurrencyEUR())
        val GBP = Currency(CurrencyGBP())

        val BTC = Currency(CurrencyBTC())
        val ETH = Currency(CurrencyETH())
        val BCH = Currency(CurrencyBCH())
        val LTC = Currency(CurrencyLTC())


        val OTHER = Currency("Other")

        val validDefaultQuotes: List<Currency> = listOf(USD, EUR, GBP, BTC)
    }


    enum class Type {
        FIAT,
        STABLECOIN,
        CRYPTO
    }
}