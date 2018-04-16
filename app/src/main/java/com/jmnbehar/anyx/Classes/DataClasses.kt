package com.jmnbehar.anyx.Classes


/**
 * Created by jmnbehar on 12/19/2017.
 */

enum class TradeSide {
    BUY,
    SELL;

    override fun toString() : String {
        return when (this) {
            BUY -> "buy"
            SELL -> "sell"
        }
    }
    companion object {
        fun fromString(string: String) : TradeSide {
            return when (string) {
                "buy"  -> BUY
                "sell" -> SELL
                else   -> BUY
            }
        }
    }
}

enum class TradeType {
    MARKET,
    LIMIT,
    STOP;

    override fun toString() : String {
        return when (this) {
            MARKET -> "market"
            LIMIT -> "limit"
            STOP -> "stop"
        }
    }
    companion object {
        fun fromString(string: String) : TradeType {
            return when (string) {
                "market"  ->  MARKET
                "limit"  -> LIMIT
                "stop" -> STOP
                else -> MARKET
            }
        }
    }
}

data class AnyXVerify(
        val data: AnyXVerifyInner
)

data class AnyXVerifyInner(
        val amount: String
)

data class AnyXIsVerified (
        val verified: Boolean,
        val status: String?
)

data class ApiProduct(
        val id: String,
        val base_currency: String,
        val quote_currency: String,
        val base_min_size: String,
        val base_max_size: String,
        val quote_increment: String,
        val display_name: String,
        val status: String,
        val margin_enabled: Boolean,
        val status_message: String?)

data class ApiOrder(
        val id: String,
        val price: String,
        val size: String?,
        val product_id: String,
        val side: String,
        val stp: String,
        val funds: String?,
        val specified_funds: String?,
        val type: String,
        val time_in_force: String,
        val post_only: Boolean,
        val created_at: String,
        val done_at: String,
        val done_reason: String,
        val fill_fees: String,
        val filled_size: String,
        val executed_value: String,
        val status: String,
        val settled: Boolean)

data class ApiFill(
        val trade_id: Int,
        val product_id: String,
        val price: String,
        val size: String,
        val order_id: String,
        val created_at: String,
        val liquidity: String,
        val fee: String,
        val settled: Boolean,
        val side: String)

data class ApiTicker(
        val trade_id: Int,
        val price: String,
        val size: String,
        val volume: String,
        val time: String)

data class ApiCurrencies(
        val id: String,
        val name: String,
        val min_size: String)

data class ApiStats(
        val open: String,
        val high: String,
        val low: String,
        val volume: String)

data class ApiTime(
        val iso: String,
        val epoch: String)

data class ApiAccount(
        val id: String,
        val currency: String,
        val balance: String,
        val holds: String,
        val available: String,
        val profile_id: String)

data class ApiCoinbaseAccount(
        val id: String,
        val name: String,
        val balance: String,
        val currency: String,
        val type: String,
        val primary: Boolean,
        val active: Boolean
        /* val wire_deposit_information: String */)

data class ApiPaymentMethod(
        val id: String,
        val type: String,
        val name: String,
        val currency: String,
        val balance: String,
        val primary_buy: Boolean,
        val primary_sell: Boolean,
        val allow_deposit: Boolean,
        val allow_withdraw: Boolean
        /* val limits: String */)

data class ApiReportInfo(
        val id: String,
        val type: String,
        val status: String,
        val created_at: String?,
        val completed_at: String,
        val expires_at: String,
        val file_url: String?
        /* val params: String */)


//data class ApiCoinbaseAccount(
//        val id: String,
//        val name: String,
//        val balance: String,
//        val currency: String,
//        val type: String,
//        val primary: Boolean,
//        val active: Boolean
//        /* val wire_deposit_information: String */)