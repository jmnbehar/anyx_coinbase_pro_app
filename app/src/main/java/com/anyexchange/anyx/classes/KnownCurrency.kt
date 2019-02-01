package com.anyexchange.anyx.classes

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.support.v4.content.ContextCompat
import com.anyexchange.anyx.R

/**
 * Created by anyexchange on 1/19/2018.
 */

enum class KnownCurrency {
    BTC,
    BCH,
    ETH,
    ETC,
    LTC,
    ZRX,
    BAT,

    CVC,
    DAI,
    DNT,
    GNT,
    LOOM,
    MANA,
    MKR,
    ZEC,
    ZIL,

    USDC,

    USD,
    EUR,
    GBP;

    //TODO: this class desperately needs a refactor

    override fun toString() : String {
        if (type == Currency.Type.CRYPTO) {
            return symbol
        } else {
            return when (this) {
                USDC -> "USDC"

                USD -> "USD"
                EUR -> "EUR"
                GBP -> "GBP"
                else -> ""
            }
        }

    }

    val symbol : String
        get() {
            return when (this) {
                BTC -> "BTC"
                BCH -> "BCH"
                ETH -> "ETH"
                ETC -> "ETC"
                LTC -> "LTC"
                ZRX -> "ZRX"
                BAT -> "BAT"

                CVC -> "CVC"
                DAI -> "DAI"
                DNT -> "DNT"
                GNT -> "GNT"
                LOOM -> "LOOM"
                MANA -> "MANA"
                MKR -> "MKR"
                ZEC -> "ZEC"
                ZIL -> "ZIL"

                USDC -> "USDC"

                USD -> "$"
                EUR -> "€"
                GBP -> "£"
            }
        }

    val fullName : String
        get() = when (this) {
            BTC -> "Bitcoin"
            BCH -> "Bitcoin Cash"
            ETH -> "Ethereum"
            ETC -> "Ether Classic"
            LTC -> "Litecoin"
            ZRX -> "0x"
            BAT -> "Basic Attention Token"

            CVC -> "Civic"
            DAI -> "Dai"
            DNT -> "district0x"
            GNT -> "Golem"
            LOOM -> "Loom Network"
            MANA -> "Decentraland"
            MKR -> "Maker"
            ZEC -> "Zcash"
            ZIL -> "Zilliqa"

            USDC -> "USD Coin"
            USD -> "US Dollar"
            EUR -> "Euro"
            GBP -> "Pound sterling"
        }

    val minSendAmount : Double
        get() = when (this) {
            BTC -> .0001
            ETH -> .001
            ETC -> .001
            BCH -> .001
            LTC -> .1
            else -> 0.0
        }

    val iconId: Int?
        get() = when(this) {
            BTC -> R.drawable.icon_btc
            ETH -> R.drawable.icon_eth
            ETC -> R.drawable.icon_etc
            LTC -> R.drawable.icon_ltc
            BCH -> R.drawable.icon_bch
            ZRX -> R.drawable.icon_zrx

            BAT -> R.drawable.icon_bat

            USDC -> R.drawable.icon_usdc

            USD -> R.drawable.icon_usd
            EUR -> R.drawable.icon_eur
            GBP -> R.drawable.icon_gbp

            else -> null
        }

    val type: Currency.Type
        get() = when(this) {
            USD, EUR, GBP -> Currency.Type.FIAT
            USDC -> Currency.Type.STABLECOIN
            else -> Currency.Type.CRYPTO
        }

    val relevantStableCoin : Currency?
        get() = when (this) {
            USD -> Currency(USDC)
            else -> null
        }

    val relevantFiat : Currency?
        get() = when (this) {
            USDC -> Currency(USD)
            else -> null
        }

    fun colorPrimary(context: Context) : Int {
        val prefs = Prefs(context)
        return if (prefs.isDarkModeOn) {
            when (this) {
                BTC -> ContextCompat.getColor(context, R.color.btc_dk)
                BCH -> ContextCompat.getColor(context, R.color.bch_dk)
                ETH -> ContextCompat.getColor(context, R.color.eth_dk)
                LTC -> ContextCompat.getColor(context, R.color.ltc_dk)
                ETC -> ContextCompat.getColor(context, R.color.etc_dk)
                ZRX -> ContextCompat.getColor(context, R.color.zrx_color)
                BAT -> ContextCompat.getColor(context, R.color.bat_color)

                USDC,
                USD,
                EUR,
                GBP -> ContextCompat.getColor(context, R.color.white)

                else -> Color.WHITE
            }
        } else {
            when (this) {
                BTC -> ContextCompat.getColor(context, R.color.btc_light)
                BCH -> ContextCompat.getColor(context, R.color.bch_light)
                ETH -> ContextCompat.getColor(context, R.color.eth_light)
                LTC -> ContextCompat.getColor(context, R.color.ltc_light)
                ETC -> ContextCompat.getColor(context, R.color.etc_light)
                ZRX -> ContextCompat.getColor(context, R.color.zrx_color)
                BAT -> ContextCompat.getColor(context, R.color.bat_color)

                USDC,
                USD,
                EUR,
                GBP -> ContextCompat.getColor(context, R.color.black)
                else -> Color.BLACK
            }
        }
    }

    fun colorStateList(context: Context) : ColorStateList {
        val prefs = Prefs(context)
        return if (prefs.isDarkModeOn) {
            when (this) {
                BTC -> context.resources.getColorStateList(R.color.btc_color_state_list_dark, context.resources.newTheme())
                ETH -> context.resources.getColorStateList(R.color.eth_color_state_list_dark, context.resources.newTheme())
                BCH -> context.resources.getColorStateList(R.color.bch_color_state_list_dark, context.resources.newTheme())
                LTC -> context.resources.getColorStateList(R.color.ltc_color_state_list_dark, context.resources.newTheme())
                ETC -> context.resources.getColorStateList(R.color.etc_color_state_list_dark, context.resources.newTheme())
                ZRX -> context.resources.getColorStateList(R.color.zrx_color_state_list, context.resources.newTheme())
                BAT -> context.resources.getColorStateList(R.color.bat_color_state_list, context.resources.newTheme())

                USDC,
                USD,
                EUR,
                GBP -> context.resources.getColorStateList(R.color.usd_color_state_list_dark, context.resources.newTheme())

                else -> context.resources.getColorStateList(R.color.usd_color_state_list_light, context.resources.newTheme())

            }
        } else {
            when (this) {
                BTC -> context.resources.getColorStateList(R.color.btc_color_state_list_light, context.resources.newTheme())
                ETH -> context.resources.getColorStateList(R.color.eth_color_state_list_light, context.resources.newTheme())
                ETC -> context.resources.getColorStateList(R.color.etc_color_state_list_dark, context.resources.newTheme())
                BCH -> context.resources.getColorStateList(R.color.bch_color_state_list_light, context.resources.newTheme())
                LTC -> context.resources.getColorStateList(R.color.ltc_color_state_list_light, context.resources.newTheme())
                ZRX -> context.resources.getColorStateList(R.color.zrx_color_state_list, context.resources.newTheme())
                BAT -> context.resources.getColorStateList(R.color.bat_color_state_list, context.resources.newTheme())

                USDC,
                USD,
                EUR,
                GBP -> context.resources.getColorStateList(R.color.usd_color_state_list_light, context.resources.newTheme())
                else -> context.resources.getColorStateList(R.color.usd_color_state_list_light, context.resources.newTheme())
            }
        }
    }

    fun buttonTextColor(context: Context) : Int {
        val prefs = Prefs(context)
        return if (prefs.isDarkModeOn) {
            when (this) {
                BTC -> Color.BLACK
                ETH -> Color.WHITE
                ETC -> Color.WHITE
                BCH -> Color.WHITE
                LTC -> Color.BLACK
                ZRX -> Color.WHITE
                BAT -> Color.WHITE

                USDC,
                USD,
                EUR,
                GBP -> Color.WHITE
                else -> Color.WHITE
            }
        } else {
            when (this) {
                BTC -> Color.WHITE
                ETH -> Color.WHITE
                ETC -> Color.WHITE
                BCH -> Color.WHITE
                LTC -> Color.WHITE
                ZRX -> Color.WHITE
                BAT -> Color.BLACK

                USDC,
                USD,
                EUR,
                GBP -> Color.BLACK
                else -> Color.BLACK
            }
        }
    }

    val developerAddress : String
        get() = when (this) {
        //CBPro Wallets (AnyX)
            BTC -> "3K63fgura9ctK3Wh6ofwyrTgCb4RrwWci6"
            ETH -> "0x6CDD817fdDAb3Ee5324e0Bb51b0f49f9d0Fd1247"
            ETC -> "0x6e459139E65B4589e3F91c86D11143dBBA4570cf"
            BCH -> "qztzaeg4axteayx7qngcdt2h72n2lw3asq50s50av8"
            LTC -> "MGnywyDCyBxGo58xnAeSS8RPLhpbenpuSD"
            ZRX -> "0x43e781a556DD3DECF64670740EE661b8d766d86c"
            BAT -> "0xF6D0aaB48BECf69f0cfF1c4693CE67a20295B02B"
            USDC -> "0x1aCfECe40ccbac06A183d67A1CDC7fb3aF1ad906"

            CVC -> "0xbdF431184Dc6b3e7fbF1Eaf60ef3ce3D741946b2"
            DAI -> "0x7B5EFa1038934677be26417d190fF426E5bFC0da"
            DNT -> "0xFd07a94cCbcc262080df2c21241077264C338929"
            GNT -> "0x0D4Ae61164Ead343758A63A6eF02410f66c73310"
            LOOM -> "0xE15897bc9Ec549068694512F464B2892BAE6a866"
            MANA -> "0x0E0403d06638d3cad4c4E622F7a262b6e1f8d4a1"
            MKR -> ""
            ZEC -> "t1azwZhxdz5LaGgxfH9FC2pnfjnBrVJPgfT"
            ZIL -> ""

            USD,
            EUR,
            GBP -> "my irl address?"
        }


    val orderValue : Int
        get() {
            return when (this) {
                USD -> 5 * 1000
                EUR -> 3 * 1000
                GBP -> 2 * 1000
                USDC -> 1 * 1000

                BTC -> 500
                ETH -> 400
                LTC -> 300
                BCH -> 200
                ETC -> 100
                ZRX -> 90
                BAT -> 80

                else -> -99999
            } * -1
        }

    companion object {
        fun forString(string: String?) : KnownCurrency? {
            return when (string) {
                "BTC" -> BTC
                "BCH" -> BCH
                "ETH" -> ETH
                "ETC" -> ETC
                "LTC" -> LTC
                "ZRX" -> ZRX
                "BAT" -> BAT

                "CVC" -> CVC
                "DAI" -> DAI
                "DNT" -> DNT
                "GNT" -> GNT
                "LOOM" -> LOOM
                "MANA" -> MANA
                "MKR" -> MKR
                "ZEC" -> ZEC
                "ZIL" -> ZIL

                "USDC" -> USDC
                "USD" -> USD
                "EUR" -> EUR
                "GBP" -> GBP
                else -> null
            }
        }
    }
}