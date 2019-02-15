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

    inner class KnownCurrencyData(val symbol: String,
                                  val shortName: String?,
                                  val fullName: String,
                                  val minSendAmount: Double,

                                  val iconId: Int? = null,

                                  val primaryColor : Int?,
                                  val primaryColorLight: Int?,

                                  val colorStateList : Int?,
                                  val colorStateListLight: Int?,
                                  val buttonTextColor : Int?,
                                  val buttonTextColorLight: Int?,

                                  val developerAddress : String?) {

        constructor(symbol: String, fullName: String, iconId: Int?, primaryColor: Int?, colorStateList: Int, buttonTextColor: Int?, developerAddress: String?):
                this(symbol, symbol, fullName, 0.0, iconId, primaryColor, null, colorStateList, null, buttonTextColor, null, developerAddress)

        constructor(symbol: String, fullName: String, iconId: Int?, developerAddress: String?):
                this(symbol, symbol, fullName, 0.0, iconId, null, null, null, null, null, null, developerAddress)

        constructor(symbol: String, shortName: String?, fullName: String, iconId: Int?):
                this(symbol, shortName, fullName, 0.0, iconId,null, null, null, null, null, null, null)
    }

    //TODO: this class desperately needs a refactor

    val currencyData: KnownCurrencyData
    get() {
            return when (this) {
                BTC -> KnownCurrencyData("BTC", "BTC", "Bitcoin", .0001, R.drawable.icon_btc,
                        R.color.btc_dk,  R.color.btc_light, R.color.btc_color_state_list_dark, R.color.btc_color_state_list_light, Color.BLACK, Color.WHITE,
                        "3K63fgura9ctK3Wh6ofwyrTgCb4RrwWci6")

                BCH -> KnownCurrencyData("BCH", "BCH", "Bitcoin Cash", .001, R.drawable.icon_bch,
                        R.color.bch_dk,  R.color.bch_light, R.color.bch_color_state_list_dark, R.color.bch_color_state_list_light, Color.WHITE, null,
                        "qztzaeg4axteayx7qngcdt2h72n2lw3asq50s50av8")

                ETH -> KnownCurrencyData("ETH","ETH", "Ethereum", .001, R.drawable.icon_eth,
                        R.color.eth_dk,  R.color.eth_light, R.color.eth_color_state_list_dark, R.color.eth_color_state_list_light, Color.WHITE, null,
                        "0x6CDD817fdDAb3Ee5324e0Bb51b0f49f9d0Fd1247")

                ETC -> KnownCurrencyData("ETC","ETC", "Ether Classic", .001, R.drawable.icon_etc,
                        R.color.etc_dk,  R.color.etc_light, R.color.etc_color_state_list_dark, R.color.etc_color_state_list_light, Color.WHITE, null,
                        "0x6e459139E65B4589e3F91c86D11143dBBA4570cf")

                LTC -> KnownCurrencyData("LTC", "LTC", "Litecoin", .1, R.drawable.icon_ltc,
                        R.color.ltc_dk,  R.color.ltc_light, R.color.ltc_color_state_list_dark, R.color.etc_color_state_list_light, Color.WHITE, null,
                        "MGnywyDCyBxGo58xnAeSS8RPLhpbenpuSD")

                BAT -> KnownCurrencyData("BAT","BAT", "Basic Attention Token", 0.0, R.drawable.icon_bat,
                        R.color.bat_color, null, R.color.bat_color_state_list, null, Color.WHITE, Color.BLACK,
                        "0xF6D0aaB48BECf69f0cfF1c4693CE67a20295B02B")

                ZRX -> KnownCurrencyData("ZRX", "0x", R.drawable.icon_zrx, R.color.zrx_color, R.color.zrx_color_state_list, Color.WHITE,
                        "0x43e781a556DD3DECF64670740EE661b8d766d86c")

                CVC -> KnownCurrencyData("CVC", "Civic", null,
                        "0xbdF431184Dc6b3e7fbF1Eaf60ef3ce3D741946b2")

                DAI -> KnownCurrencyData("DAI", "Dai", null,
                        "0x7B5EFa1038934677be26417d190fF426E5bFC0da")

                DNT -> KnownCurrencyData("DNT", "district0x", null,
                        "0xFd07a94cCbcc262080df2c21241077264C338929")

                GNT -> KnownCurrencyData("GNT", "Golem", null,
                        "0x0D4Ae61164Ead343758A63A6eF02410f66c73310")

                LOOM -> KnownCurrencyData("LOOM", "Loom Network", null,
                        "0xE15897bc9Ec549068694512F464B2892BAE6a866")

                MANA -> KnownCurrencyData("MANA", "Decentraland", null,
                        "0x0E0403d06638d3cad4c4E622F7a262b6e1f8d4a1")

                MKR -> KnownCurrencyData("MKR", "Maker", null, "")

                ZEC -> KnownCurrencyData("ZEC", "Zcash", null,
                        "t1azwZhxdz5LaGgxfH9FC2pnfjnBrVJPgfT")

                ZIL -> KnownCurrencyData("ZIL", "Zilliqa", null, "")

                USDC -> KnownCurrencyData("USDC", "USD Coin", null,
                        "0x1aCfECe40ccbac06A183d67A1CDC7fb3aF1ad906")

                USD -> KnownCurrencyData("$", "USD", "US Dollar", R.drawable.icon_usd)
                EUR -> KnownCurrencyData("€", "EUR", "Euro", R.drawable.icon_usd)
                GBP -> KnownCurrencyData("£", "GBP", "Pound sterling", R.drawable.icon_usd)
            }
        }

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
            return currencyData.symbol
        }

    val fullName : String
        get() {
            return currencyData.fullName
        }

    val minSendAmount : Double
        get() {
            return currencyData.minSendAmount
        }

    val iconId: Int?
        get() {
            return currencyData.iconId
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
        val currencyData = currencyData
        val color =  if (Prefs(context).isDarkModeOn) {
            currencyData.primaryColor ?: R.color.white
        } else {
            currencyData.primaryColorLight ?: currencyData.primaryColor ?: R.color.black
        }
        return ContextCompat.getColor(context, color)
    }

    fun colorStateList(context: Context) : ColorStateList {
        val currencyData = currencyData
        val colorStateList =  if (Prefs(context).isDarkModeOn) {
            currencyData.colorStateList ?: R.color.usd_color_state_list_dark
        } else {
            currencyData.colorStateListLight ?: currencyData.colorStateList ?: R.color.usd_color_state_list_light
        }
        return context.resources.getColorStateList(colorStateList, context.resources.newTheme())
    }

    fun buttonTextColor(context: Context) : Int {
        val currencyData = currencyData
        return if (Prefs(context).isDarkModeOn) {
            currencyData.buttonTextColor ?: Color.WHITE
        } else {
            currencyData.buttonTextColorLight ?: currencyData.buttonTextColor ?: Color.BLACK
        }
    }

    val developerAddress : String
        get()  {
            return currencyData.developerAddress ?: ""
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