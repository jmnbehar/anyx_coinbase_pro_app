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
    GdaxError;

    override fun toString(): String {
        return when (this) {
            Success ->"\n\nYour account is verified, and your ${VerifySendFragment.amount.btcFormatShortened()} ${VerifySendFragment.currency} will be returned to your Coinbase Account with email ${VerifySendFragment.email} within two days."
            RepayErrorEmailed -> "\n\nYour account is verified, we will review the email you sent us and your ${VerifySendFragment.amount.btcFormatShortened()} ${VerifySendFragment.currency} will be returned to your Coinbase Account with email ${VerifySendFragment.email}."
            RepayError -> "Your account is verified, but there was a problem with our servers so you may not be automatically repaid. " +
                    "\n\nIf you don't receive your ${VerifySendFragment.amount.btcFormatShortened()} ${VerifySendFragment.currency} in your Coinbase Account within two days, please reach out to our verification support at anyx.verify@gmail.com"
            NoTransferPermission -> missingPermissionString("Transfer")
            NoTradePermission -> missingPermissionString("Trade")
            NoTwoFactorPermission -> missingPermissionString("Bypass Two-Factor Auth")
            NoViewPermission -> missingPermissionString("View")
            GdaxError -> missingPermissionString("Transfer")
        }
    }

    val isVerified: Boolean
        get() = when (this) {
                VerificationStatus.Success           -> true
                VerificationStatus.RepayErrorEmailed -> true
                VerificationStatus.RepayError        -> true
                VerificationStatus.NoTransferPermission  -> false
                VerificationStatus.NoTradePermission     -> false
                VerificationStatus.NoTwoFactorPermission -> false
                VerificationStatus.NoViewPermission      -> false
                VerificationStatus.GdaxError             -> false
            }

    fun missingPermissionString(permission: String) : String {
        return "Your account could not be verified because your API Key does not have the \"$permission\" permission. " +
                "\n\nPlease create a new API Key with View, Transfer, Bypass Two-Factor Auth, and Trade permissions."
    }
}

enum class VerificationFundSource {
    GDAX,
    Coinbase,
    Buy;
}