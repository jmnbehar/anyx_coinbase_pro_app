package com.anyexchange.cryptox.classes

import com.anyexchange.cryptox.api.BinanceDepositAddress
import com.anyexchange.cryptox.api.CBProDepositAddress


class DepositAddressInfo(
        val address: String,
        val expires_at: String?,
        val warning_title: String?,
        val warning_details: String?
) {
    constructor(cbProDepositAddress: CBProDepositAddress) : this(cbProDepositAddress.address, cbProDepositAddress.expires_at, cbProDepositAddress.warning_title, cbProDepositAddress.warning_details)

    constructor(binanceDepositAddress: BinanceDepositAddress) : this(binanceDepositAddress.address, null, null, null)
}