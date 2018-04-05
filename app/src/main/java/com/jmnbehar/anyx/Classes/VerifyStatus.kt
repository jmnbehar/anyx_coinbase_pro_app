package com.jmnbehar.anyx.Classes

import com.jmnbehar.anyx.Fragments.Verify.VerifySendFragment

/**
 * Created by jmnbehar on 3/22/2018.
 */


enum class VerificationStatus {
    Success,
    RepayError,
    RepayErrorEmailed,
    NoTransferPermission,
    NoTradePermission,
    NoTwoFactorPermission,
    NoViewPermission,
    NoPaymentMethods,
    GdaxError,
    UnknownError;

    val isVerified: Boolean
        get() = when (this) {
                VerificationStatus.Success           -> true
                VerificationStatus.RepayErrorEmailed -> true
                VerificationStatus.RepayError        -> true
                VerificationStatus.NoTransferPermission  -> false
                VerificationStatus.NoTradePermission     -> false
                VerificationStatus.NoTwoFactorPermission -> false
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