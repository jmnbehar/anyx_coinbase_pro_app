package com.anyexchange.anyx.classes

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.support.v4.content.ContextCompat
import com.anyexchange.anyx.R

/**
 * Created by anyexchange on 1/19/2018.
 */

class Currency(val id: String) {
    constructor(knownCurrency: KnownCurrency): this(knownCurrency.symbol)
    val knownCurrency = KnownCurrency.forString(id)

    override fun toString() : String {
        return id
    }

    val symbol = knownCurrency?.symbol ?: id

    val fullName = knownCurrency?.fullName ?: id

    val iconId = knownCurrency?.iconId ?: R.drawable.fail_icon

    val feePercentage : Double
        get() {
            return if (!isFiat) {
                0.003
            } else {
                0.0
            }
        }

    val isFiat = knownCurrency?.isFiat ?: false

    val minSendAmount: Double = knownCurrency?.minSendAmount ?: 0.0

    fun addToList() {
        if (!isFiat && !cryptoList.any { it.id == this.id }) {
            cryptoList.add(this)
        }
    }

    fun colorPrimary(context: Context) : Int {
        return knownCurrency?.colorPrimary(context) ?: run {
            //TODO: generate colors based on name and darkmode
            val isDarkModeOn = Prefs(context).isDarkModeOn

            ContextCompat.getColor(context, R.color.white)
        }
    }

    fun colorFade(context: Context): Int {
        return knownCurrency?.colorFade(context) ?: run {
            //TODO: generate colors based on name and darkmode
            val isDarkModeOn = Prefs(context).isDarkModeOn
            ContextCompat.getColor(context, R.color.black_fade)
        }
    }

    fun colorAccent(context: Context) : Int {
        return knownCurrency?.colorAccent(context) ?: run {
            //TODO: generate colors based on name and darkmode
            val isDarkModeOn = Prefs(context).isDarkModeOn
            ContextCompat.getColor(context, R.color.black)
        }
    }

    fun colorStateList(context: Context) : ColorStateList {
        return knownCurrency?.colorStateList(context) ?: run {
            //TODO: generate colors based on name and darkmode
            val isDarkModeOn = Prefs(context).isDarkModeOn
            context.resources.getColorStateList(R.color.usd_color_state_list_light, context.resources.newTheme())
        }
    }

    fun buttonTextColor(context: Context) : Int {
        return knownCurrency?.colorAccent(context) ?: run {
            //TODO: generate colors based on name and darkmode
            val isDarkModeOn = Prefs(context).isDarkModeOn
            Color.WHITE
        }
    }

    val developerAddress = knownCurrency?.developerAddress

    //TODO: improve
    val orderValue = knownCurrency?.orderValue ?: 999



    companion object {
        val cryptoList: MutableList<Currency> = KnownCurrency.values().filter { !it.isFiat }.asSequence().map { Currency(it) }.toMutableList()
        val fiatList       = KnownCurrency.values().filter { it.isFiat  }

        val USD = Currency(KnownCurrency.USD)
        val BTC = Currency(KnownCurrency.BTC)
        val ETH = Currency(KnownCurrency.ETH)
        val BCH = Currency(KnownCurrency.BCH)
        val LTC = Currency(KnownCurrency.LTC)

        val OTHER = Currency("Other")
    }
}