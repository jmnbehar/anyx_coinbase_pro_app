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