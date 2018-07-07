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
    CBProError,
    UnknownError;

    val isVerified: Boolean
        get() = when (this) {
                VerificationStatus.Success           -> true
                VerificationStatus.RepayError        -> true
                VerificationStatus.NoTransferPermission  -> false
                VerificationStatus.NoTradePermission     -> false
                VerificationStatus.NoViewPermission      -> false
                VerificationStatus.NoPaymentMethods      -> false
                VerificationStatus.CBProError            -> false
                VerificationStatus.UnknownError          -> false
            }

}

enum class VerificationFundSource {
    Pro,
    Coinbase,
    Buy;

    override fun toString(): String {
        return when (this) {
            Pro -> "pro"
            Coinbase -> "coinbase"
            Buy -> "buy"
        }
    }

    companion object {
        fun fromString(string: String) : VerificationFundSource? {
            return when (string) {
                "pro" -> Pro
                "coinbase" -> Coinbase
                "buy" -> Buy
                else -> null
            }
        }
    }
}