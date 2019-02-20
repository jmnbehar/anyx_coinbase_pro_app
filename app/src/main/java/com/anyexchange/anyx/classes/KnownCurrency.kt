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

    BCD,
    HOT,
    REN,
    POE,
    ADA,
    POLY,
    TNT,
    WAN,
    MCO,
    NCASH,
    MOD,
    LSK,
    TNB,
    GVT,
    APPC,
    NEBL,
    QLC,
    NEO,
    CHAT,
    INS,
    XRP,
    ELF,
    ONT,
    PPT,
    RLC,
    BCC,
    SALT,
    SNGLS,
    AMB,
    XZC,
    GXC,
    BTS,
    MTL,
    WTC,
    STORJ,
    KNC,
    BLZ,
    ARK,
    STORM,
    SNT,
    XVG,
    CDT,
    VIB,
    BCPT,
    ENJ,
    VEN,
    RDN,
    RVN,
    DLT,
    TRIG,
    SYS,
    NAV,
    VIBE,
    BCN,
    NAS,
    EDO,
    KMD,
    WPR,
    STRAT,
    CLOAK,
    MDA,
    SUB,
    VET,
    AST,
    ADX,
    OAX,
    OST,
    GRS,
    EVX,
    QKC,
    NXS,
    VIA,
    PIVX,
    ARN,
    KEY,
    IOTX,
    LRC,
    MITH,
    WAVES,
    DATA,
    NPXS,
    LUN,
    NANO,
    XMR,
    TUSD,
    YOYO,
    DGD,
    LEND,
    OMG,
    SKY,
    BNB,
    QSP,
    WABI,
    HSR,
    STEEM,
    CMT,
    HC,
    DENT,
    BNT,
    LINK,
    RCN,
    BTT,
    POWR,
    THETA,
    ENG,
    TRX,
    BRD,
    AE,
    DOCK,
    REP,
    GO,
    FUEL,
    IOTA,
    BQX,
    BCHSV,
    AION,
    SNM,
    XEM,
    REQ,
    AGI,
    PAX,
    FUN,
    WINGS,
    NULS,
    ARDR,
    MTH,
    MFT,
    ZEN,
    ICX,
    IOST,
    RPX,
    SC,
    BTG,
    DCR,
    PHX,
    POA,
    XLM,
    GAS,
    CND,
    DASH,
    EOS,
    ICN,
    QTUM,
    GTO,

    //XTZ
    //DOGE
    //VERI
    //DGB
    //BTM
    //R
    //CNX
    //ETN
    //HT
    //MAID
    //FCT
    //XIN
    //ODE
    //XET
    //STU
    //AOA
    //MOAC
    //NEXO
    //WAX
    //MONA
    //RDD
    //KCS
    //ETP
    //TOMO
    //BTCP
    //ELA


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

    private val currencyData: KnownCurrencyData
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

                MKR -> KnownCurrencyData("MKR", "Maker", null, null)

                ZEC -> KnownCurrencyData("ZEC", "Zcash", null,
                        "t1azwZhxdz5LaGgxfH9FC2pnfjnBrVJPgfT")

                ZIL -> KnownCurrencyData("ZIL", "Zilliqa", null, null)

                USDC -> KnownCurrencyData("USDC", "USD Coin", R.drawable.icon_usdc,
                        "0x1aCfECe40ccbac06A183d67A1CDC7fb3aF1ad906")


                BCD -> KnownCurrencyData("BCD", "Bitcoin Diamond", null, null)
                HOT -> KnownCurrencyData("HOT", "Hydro Protocol", null, null)
                REN -> KnownCurrencyData("REN", "Ren", null, null)
                POE -> KnownCurrencyData("POE", "Po.et", null, null)
                ADA -> KnownCurrencyData("ADA", "Cardano", null, null)
                POLY -> KnownCurrencyData("POLY", "Polymath", null, null)
                TNT -> KnownCurrencyData("TNT", "Tierion", null, null)
                WAN -> KnownCurrencyData("WAN", "Wanchain", null, null)
                MCO -> KnownCurrencyData("MCO", "Crypto.com", null, null)
                NCASH -> KnownCurrencyData("NCASH", "Nucleus Vision", null, null)
                MOD -> KnownCurrencyData("MOD", "Modum", null, null)
                LSK -> KnownCurrencyData("LSK", "Lisk", null, null)
                TNB -> KnownCurrencyData("TNB", "Time New Bank", null, null)
                GVT -> KnownCurrencyData("GVT", "Genesis Vision", null, null)
                APPC -> KnownCurrencyData("APPC", "AppCoins", null, null)
                NEBL -> KnownCurrencyData("NEBL", "Neblio", null, null)
                QLC -> KnownCurrencyData("QLC", "QLC Chain", null, null)
                NEO -> KnownCurrencyData("NEO", "NEO", null, null)
                CHAT -> KnownCurrencyData("CHAT", "ChatCoin", null, null)
                INS -> KnownCurrencyData("INS", "Insolar", null, null)
                XRP -> KnownCurrencyData("XRP", "XRP", null, null)
                ELF -> KnownCurrencyData("ELF", "aelf", null, null)
                ONT -> KnownCurrencyData("ONT", "Ontology", null, null)
                PPT -> KnownCurrencyData("PPT", "Populous", null, null)
                RLC -> KnownCurrencyData("RLC", "iExec RLC", null, null)
                BCC -> KnownCurrencyData("BCC", "BitConnect", null, null)
                SALT -> KnownCurrencyData("SALT", "SALT", null, null)
                SNGLS -> KnownCurrencyData("SNGLS", "SingularDTV", null, null)
                AMB -> KnownCurrencyData("AMB", "Ambrosus", null, null)
                XZC -> KnownCurrencyData("XZC", "Zcoin", null, null)
                GXC -> KnownCurrencyData("GXC", "GXChain", null, null)
                BTS -> KnownCurrencyData("BTS", "Bitshares", null, null)
                MTL -> KnownCurrencyData("MTL", "Metal", null, null)
                WTC -> KnownCurrencyData("WTC", "Walton", null, null)
                STORJ -> KnownCurrencyData("STORJ", "Storj", null, null)
                KNC -> KnownCurrencyData("KNC", "KyberNetwork", null, null)
                BLZ -> KnownCurrencyData("BLZ", "Bluzelle", null, null)
                ARK -> KnownCurrencyData("ARK", "Ark", null, null)
                STORM -> KnownCurrencyData("STORM", "Storm", null, null)
                SNT -> KnownCurrencyData("SNT", "Status", null, null)
                XVG -> KnownCurrencyData("XVG", "Verge", null, null)
                CDT -> KnownCurrencyData("CDT", "Blox", null, null)
                VIB -> KnownCurrencyData("VIB", "Viberate", null, null)
                BCPT -> KnownCurrencyData("BCPT", "BlockMason Credit Protocol", null, null)
                ENJ -> KnownCurrencyData("ENJ", "Enjin Coin", null, null)
                VEN -> KnownCurrencyData("VEN", "Vechain", null, null)
                RDN -> KnownCurrencyData("RDN", "Raiden Network Token", null, null)
                RVN -> KnownCurrencyData("RVN", "Ravencoin", null, null)
                DLT -> KnownCurrencyData("DLT", "Agrello", null, null)
                TRIG -> KnownCurrencyData("TRIG", "Triggers", null, null)
                SYS -> KnownCurrencyData("SYS", "Syscoin", null, null)
                NAV -> KnownCurrencyData("NAV", "NavCoin", null, null)
                VIBE -> KnownCurrencyData("VIBE", "VIBE", null, null)
                BCN -> KnownCurrencyData("BCN", "Bytecoin", null, null)
                NAS -> KnownCurrencyData("NAS", "Nebulas", null, null)
                EDO -> KnownCurrencyData("EDO", "Eidoo", null, null)
                KMD -> KnownCurrencyData("KMD", "Komodo", null, null)
                WPR -> KnownCurrencyData("WPR", "WePower", null, null)
                STRAT -> KnownCurrencyData("STRAT", "Stratis", null, null)
                CLOAK -> KnownCurrencyData("CLOAK", "CloakCoin", null, null)
                MDA -> KnownCurrencyData("MDA", "Moeda Loyalty Points", null, null)
                SUB -> KnownCurrencyData("SUB", "Substratum", null, null)
                VET -> KnownCurrencyData("VET", "VeChainThor", null, null)
                AST -> KnownCurrencyData("AST", "AirSwap", null, null)
                ADX -> KnownCurrencyData("ADX", "AdEx", null, null)
                OAX -> KnownCurrencyData("OAX", "openANX", null, null)
                OST -> KnownCurrencyData("OST", "OST", null, null)
                GRS -> KnownCurrencyData("GRS", "Groestlcoin", null, null)
                EVX -> KnownCurrencyData("EVX", "Everex", null, null)
                QKC -> KnownCurrencyData("QKC", "QuarkChain", null, null)
                NXS -> KnownCurrencyData("NXS", "Nexus", null, null)
                VIA -> KnownCurrencyData("VIA", "Viacoin", null, null)
                PIVX -> KnownCurrencyData("PIVX", "PIVX", null, null)
                ARN -> KnownCurrencyData("ARN", "Aeron", null, null)
                KEY -> KnownCurrencyData("KEY", "SelfKey", null, null)
                IOTX -> KnownCurrencyData("IOTX", "IoTeX", null, null)
                LRC -> KnownCurrencyData("LRC", "Loopring", null, null)
                MITH -> KnownCurrencyData("MITH", "Mithril", null, null)
                WAVES -> KnownCurrencyData("WAVES", "Waves", null, null)
                DATA -> KnownCurrencyData("DATA", "Streamr DATAcoin", null, null)
                NPXS -> KnownCurrencyData("NPXS", "Pundi X", null, null)
                LUN -> KnownCurrencyData("LUN", "Lunyr", null, null)
                NANO -> KnownCurrencyData("NANO", "Nano", null, null)
                XMR -> KnownCurrencyData("XMR", "Monero", null, null)
                TUSD -> KnownCurrencyData("TUSD", "TrueUSD", null, null)
                YOYO -> KnownCurrencyData("YOYO", "YOYOW", null, null)
                DGD -> KnownCurrencyData("DGD", "DigixDAO", null, null)
                LEND -> KnownCurrencyData("LEND", "ETHLend", null, null)
                OMG -> KnownCurrencyData("OMG", "OmiseGO", null, null)
                SKY -> KnownCurrencyData("SKY", "Skycoin", null, null)
                BNB -> KnownCurrencyData("BNB", "Binance Coin", null, null)
                QSP -> KnownCurrencyData("QSP", "Quantstamp", null, null)
                WABI -> KnownCurrencyData("WABI", "TAEL", null, null)
                HSR -> KnownCurrencyData("HSR", "Hshare", null, null)
                STEEM -> KnownCurrencyData("STEEM", "Steem", null, null)
                CMT -> KnownCurrencyData("CMT", "CyberMiles", null, null)
                HC -> KnownCurrencyData("HC", "HyperCash", null, null)
                DENT -> KnownCurrencyData("DENT", "DENT", null, null)
                BNT -> KnownCurrencyData("BNT", "Bancor", null, null)
                LINK -> KnownCurrencyData("LINK", "Chainlink", null, null)
                RCN -> KnownCurrencyData("RCN", "Ripio Credit Network\n", null, null)
                BTT -> KnownCurrencyData("BTT", "BitTorrent", null, null)
                POWR -> KnownCurrencyData("POWR", "Power Ledger", null, null)
                THETA -> KnownCurrencyData("THETA", "Theta Network", null, null)
                ENG -> KnownCurrencyData("ENG", "Enigma", null, null)
                TRX -> KnownCurrencyData("TRX", "TRON", null, null)
                BRD -> KnownCurrencyData("BRD", "Bread", null, null)
                AE -> KnownCurrencyData("AE", "Aeternity", null, null)
                DOCK -> KnownCurrencyData("DOCK", "Dock", null, null)
                REP -> KnownCurrencyData("REP", "Augur", null, null)
                GO -> KnownCurrencyData("GO", "GoChain", null, null)
                FUEL -> KnownCurrencyData("FUEL", "Etherparty", null, null)
                IOTA -> KnownCurrencyData("IOTA", "MIOTA", null, null)
                BQX -> KnownCurrencyData("BQX", "ETHOS", null, null)
                BCHSV -> KnownCurrencyData("BSV", "Bitcoin SV", null, null)
                AION -> KnownCurrencyData("AION", "AION", null, null)
                SNM -> KnownCurrencyData("SNM", "SONM", null, null)
                XEM -> KnownCurrencyData("XEM", "NEM", null, null)
                REQ -> KnownCurrencyData("REQ", "Request Network", null, null)
                AGI -> KnownCurrencyData("AGI", "SingularityNET", null, null)
                PAX -> KnownCurrencyData("PAX", "Paxos Standard", null, null)
                FUN -> KnownCurrencyData("FUN", "FunFair", null, null)
                WINGS -> KnownCurrencyData("WINGS", "WINGS", null, null)
                NULS -> KnownCurrencyData("NULS", "NULS", null, null)
                ARDR -> KnownCurrencyData("ARDR", "Ardor", null, null)
                MTH -> KnownCurrencyData("MTH", "Monetha", null, null)
                MFT -> KnownCurrencyData("MFT", "Mainframe", null, null)
                ZEN -> KnownCurrencyData("ZEN", "Horizen", null, null)
                ICX -> KnownCurrencyData("ICX", "ICON", null, null)
                IOST -> KnownCurrencyData("IOST", "Internet of Services", null, null)
                RPX -> KnownCurrencyData("RPX", "Red Pulse Coin", null, null)
                SC -> KnownCurrencyData("SC", "SiaCoin", null, null)
                BTG -> KnownCurrencyData("BTG", "Bitcoin Gold", null, null)
                DCR -> KnownCurrencyData("DCR", "Decred", null, null)
                PHX -> KnownCurrencyData("PHX", "Red Pulse Phoenix", null, null)
                POA -> KnownCurrencyData("POA", "POA Network", null, null)
                XLM -> KnownCurrencyData("XLM", "Stellar Lumens", null, null)
                GAS -> KnownCurrencyData("GAS", "NeoGas", null, null)
                CND -> KnownCurrencyData("CND", "Cindicator", null, null)
                DASH -> KnownCurrencyData("DASH", "Dash", null, null)
                EOS -> KnownCurrencyData("EOS", "EOS", null, null)
                ICN -> KnownCurrencyData("ICN", "ICONOMI", null, null)
                QTUM -> KnownCurrencyData("QTUM", "Qtum", null, null)
                GTO -> KnownCurrencyData("GTO", "Gifto", null, null)

                USD -> KnownCurrencyData("$", "USD", "US Dollar", R.drawable.icon_usd)
                EUR -> KnownCurrencyData("€", "EUR", "Euro", R.drawable.icon_usd)
                GBP -> KnownCurrencyData("£", "GBP", "Pound sterling", R.drawable.icon_usd)
            }
        }

    override fun toString() : String {
        return currencyData.shortName ?: currencyData.symbol
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
        //TODO: refactor
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

                "BCD" -> BCD
                "HOT" -> HOT
                "REN" -> REN
                "POE" -> POE
                "ADA" -> ADA
                "POLY" -> POLY
                "TNT" -> TNT
                "WAN" -> WAN
                "MCO" -> MCO
                "NCASH" -> NCASH
                "MOD" -> MOD
                "LSK" -> LSK
                "TNB" -> TNB
                "GVT" -> GVT
                "APPC" -> APPC
                "NEBL" -> NEBL
                "QLC" -> QLC
                "NEO" -> NEO
                "CHAT" -> CHAT
                "INS" -> INS
                "XRP" -> XRP
                "ELF" -> ELF
                "ONT" -> ONT
                "PPT" -> PPT
                "RLC" -> RLC
                "BCC" -> BCC
                "SALT" -> SALT
                "SNGLS" -> SNGLS
                "AMB" -> AMB
                "XZC" -> XZC

                "GXC" -> GXC
                "GXS" -> GXC

                "BTS" -> BTS
                "MTL" -> MTL
                "WTC" -> WTC
                "STORJ" -> STORJ
                "KNC" -> KNC
                "BLZ" -> BLZ
                "ARK" -> ARK
                "STORM" -> STORM
                "SNT" -> SNT
                "XVG" -> XVG
                "CDT" -> CDT
                "VIB" ->VIB
                "BCPT" -> BCPT
                "ENJ" -> ENJ
                "VEN" -> VEN
                "RDN" -> RDN
                "RVN" -> RVN
                "DLT" -> DLT
                "TRIG" -> TRIG
                "SYS" -> SYS
                "NAV" -> NAV
                "VIBE" -> VIBE
                "BCN" -> BCN
                "NAS" -> NAS
                "EDO" -> EDO
                "KMD" -> KMD
                "WPR" -> WPR
                "STRAT" -> STRAT
                "CLOAK" -> CLOAK
                "MDA" -> MDA
                "SUB" -> SUB
                "VET" -> VET
                "AST" -> AST
                "ADX" -> ADX
                "OAX" -> OAX
                "OST" -> OST
                "GRS" -> GRS
                "BCHABC" -> BCH
                "EVX" -> EVX
                "QKC" -> QKC
                "NXS" -> NXS
                "VIA" -> VIA
                "PIVX" -> PIVX
                "ARN" -> ARN
                "KEY" -> KEY
                "IOTX" -> IOTX
                "LRC" -> LRC
                "MITH" -> MITH
                "WAVES" -> WAVES
                "DATA" -> DATA
                "NPXS" -> NPXS
                "LUN" -> LUN
                "NANO" -> NANO
                "XMR" -> XMR
                "TUSD" -> TUSD
                "YOYO" -> YOYO
                "DGD" -> DGD
                "LEND" -> LEND
                "OMG" -> OMG
                "SKY" -> SKY
                "BNB" -> BNB
                "QSP" -> QSP
                "WABI" -> WABI
                "HSR" -> HSR
                "STEEM" -> STEEM
                "CMT" -> CMT
                "HC" -> HC
                "DENT" -> DENT
                "BNT" -> BNT
                "LINK" -> LINK
                "RCN" -> RCN
                "BTT" -> BTT
                "POWR" -> POWR
                "THETA" -> THETA
                "ENG" -> ENG
                "TRX" -> TRX
                "BRD" -> BRD
                "AE" -> AE
                "DOCK" -> DOCK
                "REP" -> REP
                "GO" -> GO
                "FUEL" -> FUEL
                "IOTA" -> IOTA
                "BQX" -> BQX
                "BCHSV" -> BCHSV
                "BSV" -> BCHSV
                "AION" -> AION
                "SNM" -> SNM
                "XEM" -> XEM
                "REQ" -> REQ
                "AGI" -> AGI
                "PAX" -> PAX
                "FUN" -> FUN
                "WINGS" -> WINGS
                "NULS" -> NULS
                "ARDR" -> ARDR
                "MTH" -> MTH
                "MFT" -> MFT
                "ZEN" -> ZEN
                "ICX" -> ICX
                "IOST" -> IOST
                "RPX" -> RPX
                "SC" -> SC
                "BTG" -> BTG
                "DCR" -> DCR
                "PHX" -> PHX
                "POA" -> POA
                "XLM" -> XLM
                "GAS" -> GAS
                "CND" -> CND
                "DASH" -> DASH
                "EOS" -> EOS
                "ICN" -> ICN
                "QTUM" -> QTUM
                "GTO" -> GTO

                "USDC" -> USDC
                "USD" -> USD
                "EUR" -> EUR
                "GBP" -> GBP
                else -> null
            }
        }
    }
}