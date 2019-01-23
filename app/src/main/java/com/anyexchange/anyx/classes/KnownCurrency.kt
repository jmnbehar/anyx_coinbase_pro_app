package com.anyexchange.anyx.classes

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.support.v4.content.ContextCompat
import com.anyexchange.anyx.R

/**
 * Created by anyexchange on 1/22/2019.
 */

abstract class KnownCurrency {
    abstract val symbol: String
    abstract val name: String
    abstract val fullName: String
    abstract val minSendAmount: Double
    abstract val iconId: Int
    abstract val type: Currency.Type
    abstract val relevantStableCoin: Currency?
    abstract val relevantFiat: Currency?
    abstract val colorPrimaryResource : Int
    abstract val colorStateListResource : Int
    abstract val buttonTextColorResource : Int
    open val colorPrimaryResourceLight : Int? = null
    open val colorStateListResourceLight : Int? = null
    open val buttonTextColorResourceLight : Int? = null
    abstract val orderValue: Int
    abstract val developerAddress: String

    companion object {
        fun forString(string: String?) : KnownCurrency? {
            return when (string) {
                "BTC" -> CurrencyBTC()
                "BCH" -> CurrencyBCH()
                "ETH" -> CurrencyETH()
                "ETC" -> CurrencyETC()
                "LTC" -> CurrencyLTC()
                "ZRX" -> CurrencyZRX()
                "BAT" -> CurrencyBAT()

                "USDC" -> CurrencyUSDC()
                "USD" -> CurrencyUSD()
                "EUR" -> CurrencyEUR()
                "GBP" -> CurrencyGBP()
                else -> null
            }
        }


        fun cryptoList() : List<DummyCryptoCurrency> {
            return listOf(CurrencyBTC(), CurrencyBCH(), CurrencyETH(),
                    CurrencyETC(), CurrencyLTC(), CurrencyZRX(), CurrencyBAT())
        }

        fun fiatList() : List<DummyFiatCurrency> {
            return listOf(CurrencyUSD(), CurrencyGBP(), CurrencyEUR())
        }
    }

    fun colorPrimary(context: Context) : Int {
        val colorPrimaryResourceLight = colorPrimaryResourceLight
        return if (Prefs(context).isDarkModeOn || colorPrimaryResourceLight == null) {
            ContextCompat.getColor(context, colorPrimaryResource)
        } else {
            ContextCompat.getColor(context, colorPrimaryResourceLight)
        }
    }

    fun colorStateList(context: Context) : ColorStateList {
        val colorStateListResourceLight = colorStateListResourceLight
        return if (Prefs(context).isDarkModeOn || colorStateListResourceLight == null) {
            context.resources.getColorStateList(colorStateListResource, context.resources.newTheme())
        } else {
            context.resources.getColorStateList(colorStateListResourceLight, context.resources.newTheme())
        }
    }

    fun buttonTextColor(context: Context) : Int {
        val buttonTextColorResourceLight = buttonTextColorResourceLight
        return if (Prefs(context).isDarkModeOn || buttonTextColorResourceLight == null) {
            buttonTextColorResource
        } else {
            buttonTextColorResourceLight
        }
    }
}

abstract class DummyCryptoCurrency : KnownCurrency() {
    override val type = Currency.Type.CRYPTO
    override val name: String
        get() = symbol
    override val relevantStableCoin: Currency? = null
    override val relevantFiat: Currency? = null
}

abstract class DummyFiatCurrency : KnownCurrency() {
    override val minSendAmount = 0.0
    override val type = Currency.Type.FIAT
    override val relevantFiat: Currency?  = null
    override val colorPrimaryResource = R.color.white
    override val colorPrimaryResourceLight = R.color.black
    override val colorStateListResource = R.color.usd_color_state_list_dark
    override val colorStateListResourceLight = R.color.usd_color_state_list_light
    override val buttonTextColorResource = Color.WHITE
    override val buttonTextColorResourceLight = Color.BLACK
    override val developerAddress: String = ""
}

class CurrencyBTC : DummyCryptoCurrency() {
    override val symbol = "BTC"
    override val fullName = "Bitcoin"
    override val minSendAmount = 0.001
    override val iconId = R.drawable.icon_btc
    override val colorPrimaryResource = R.color.btc_dk
    override val colorPrimaryResourceLight = R.color.btc_light
    override val colorStateListResource = R.color.btc_color_state_list_dark
    override val colorStateListResourceLight = R.color.btc_color_state_list_light
    override val buttonTextColorResource = Color.BLACK
    override val buttonTextColorResourceLight = Color.WHITE
    override val orderValue = -500
    override val developerAddress = "3K63fgura9ctK3Wh6ofwyrTgCb4RrwWci6"
}
class CurrencyBCH : DummyCryptoCurrency() {
    override val symbol = "BCH"
    override val fullName = "Bitcoin Cash"
    override val minSendAmount = 0.001
    override val iconId = R.drawable.icon_bch
    override val colorPrimaryResource = R.color.bch_dk
    override val colorPrimaryResourceLight = R.color.bch_light
    override val colorStateListResource = R.color.bch_color_state_list_dark
    override val colorStateListResourceLight = R.color.bch_color_state_list_light
    override val buttonTextColorResource = Color.WHITE
    override val orderValue = -200
    override val developerAddress = "qztzaeg4axteayx7qngcdt2h72n2lw3asq50s50av8"
}
class CurrencyETH : DummyCryptoCurrency() {
    override val symbol = "ETH"
    override val fullName = "Ethereum"
    override val minSendAmount = 0.001
    override val iconId = R.drawable.icon_eth
    override val colorPrimaryResource = R.color.eth_dk
    override val colorPrimaryResourceLight = R.color.eth_light
    override val colorStateListResource = R.color.eth_color_state_list_dark
    override val colorStateListResourceLight = R.color.eth_color_state_list_light
    override val buttonTextColorResource = Color.WHITE
    override val orderValue = -400
    override val developerAddress = "0x6CDD817fdDAb3Ee5324e0Bb51b0f49f9d0Fd1247"
}
class CurrencyETC : DummyCryptoCurrency() {
    override val symbol = "ETC"
    override val fullName = "Ether Classic"
    override val minSendAmount = 0.001
    override val iconId = R.drawable.icon_etc
    override val colorPrimaryResource = R.color.etc_dk
    override val colorPrimaryResourceLight = R.color.etc_light
    override val colorStateListResource = R.color.etc_color_state_list_dark
    override val colorStateListResourceLight = R.color.etc_color_state_list_light
    override val buttonTextColorResource = Color.WHITE
    override val orderValue = -100
    override val developerAddress = "0x6e459139E65B4589e3F91c86D11143dBBA4570cf"
}
class CurrencyLTC : DummyCryptoCurrency() {
    override val symbol = "LTC"
    override val fullName = "Litecoin"
    override val minSendAmount = 0.1
    override val iconId = R.drawable.icon_ltc
    override val colorPrimaryResource = R.color.ltc_dk
    override val colorPrimaryResourceLight = R.color.ltc_light
    override val colorStateListResource = R.color.ltc_color_state_list_dark
    override val colorStateListResourceLight = R.color.ltc_color_state_list_light
    override val buttonTextColorResource = Color.BLACK
    override val buttonTextColorResourceLight = Color.WHITE
    override val orderValue = -300
    override val developerAddress = "MGnywyDCyBxGo58xnAeSS8RPLhpbenpuSD"
}
class CurrencyZRX : DummyCryptoCurrency() {
    override val symbol = "ZRX"
    override val fullName = "0x"
    override val minSendAmount = 0.1
    override val iconId = R.drawable.icon_zrx
    override val colorPrimaryResource = R.color.zrx_color
    override val colorStateListResource = R.color.zrx_color_state_list
    override val buttonTextColorResource = Color.WHITE
    override val orderValue = -90
    override val developerAddress = "0x43e781a556DD3DECF64670740EE661b8d766d86c"
}

class CurrencyBAT : DummyCryptoCurrency() {
    override val symbol = "BAT"
    override val fullName = "Basic Attention Token"
    override val minSendAmount = 0.1
    override val iconId = R.drawable.icon_bat
    override val colorPrimaryResource = R.color.bat_color
    override val colorStateListResource = R.color.bat_color_state_list
    override val buttonTextColorResource = Color.WHITE
    override val buttonTextColorResourceLight = Color.BLACK
    override val orderValue = -80
    override val developerAddress = "0xF6D0aaB48BECf69f0cfF1c4693CE67a20295B02B"
}

class CurrencyUSDC : KnownCurrency() {
    override val symbol = "USDC"
    override val name = "USDC"
    override val fullName = "USD Coin"
    override val minSendAmount = 0.1
    override val iconId = R.drawable.icon_usdc
    override val type = Currency.Type.STABLECOIN
    override val relevantFiat: Currency? = Currency.USD
    override val relevantStableCoin: Currency? = null
    override val colorPrimaryResource = R.color.white
    override val colorPrimaryResourceLight = R.color.black
    override val colorStateListResource = R.color.usd_color_state_list_dark
    override val colorStateListResourceLight = R.color.usd_color_state_list_light
    override val buttonTextColorResource = Color.WHITE
    override val buttonTextColorResourceLight = Color.BLACK
    override val orderValue = -1000
    override val developerAddress = "0x1aCfECe40ccbac06A183d67A1CDC7fb3aF1ad906"
}

class CurrencyUSD : DummyFiatCurrency() {
    override val symbol = "$"
    override val name = "USD"
    override val fullName = "US Dollar"
    override val iconId = R.drawable.icon_usd
    override val relevantStableCoin: Currency? = Currency.USDC
    override val orderValue = -5000

}
class CurrencyEUR : DummyFiatCurrency() {
    override val symbol = "€"
    override val name = "EUR"
    override val fullName = "Euro"
    override val iconId = R.drawable.icon_eur
    override val relevantStableCoin: Currency? = null
    override val orderValue = -3000
}
class CurrencyGBP : DummyFiatCurrency() {
    override val symbol = "£"
    override val name = "GBP"
    override val fullName = "Pound Sterling"
    override val iconId = R.drawable.icon_gbp
    override val relevantStableCoin: Currency? = null
    override val orderValue = -2000
}

