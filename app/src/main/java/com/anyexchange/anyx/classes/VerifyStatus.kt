package com.anyexchange.anyx.classes

/**
 * Created by anyexchange on 3/22/2018.
 */


enum class VerificationStatus {
    Success,
    RepayError,
    NoTransferPermission,
    NoTradePermission,
    NoViewPermission,
    NoPaymentMethods,
    GdaxError,
    UnknownError;

    val isVerified: Boolean
        get() = when (this) {
                VerificationStatus.Success           -> true
                VerificationStatus.RepayError        -> true
                VerificationStatus.NoTransferPermission  -> false
                VerificationStatus.NoTradePermission     -> false
                VerificationStatus.NoViewPermission      -> false
                VerificationStatus.NoPaymentMethods      -> false
                VerificationStatus.GdaxError             -> false
                VerificationStatus.UnknownError          -> false
            }

}

enum class VerificationFundSource {
    GDAX,
    Coinbase,
    Buy;

    override fun toString(): String {
        return when (this) {
            GDAX -> "gdax"
            Coinbase -> "coinbase"
            Buy -> "buy"
        }
    }

    companion object {
        fun fromString(string: String) : VerificationFundSource? {
            return when (string) {
                "gdax" -> GDAX
                "coinbase" -> Coinbase
                "buy" -> Buy
                else -> null
            }
        }
    }
}