package com.anyexchange.cryptox.classes

/**
 * Created by anyexchange on 3/22/2018.
 */


enum class VerificationStatus {
    Success,
    NoTransferPermission,
    NoTradePermission,
    NoViewPermission,
    NoPaymentMethods,
    CBProError,
    UnknownError;

    val isVerified: Boolean
        get() = when (this) {
                Success -> true
                else    -> false
            }

}