package com.anyexchange.cryptox.api

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


data class CBProProduct(
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

data class CBProOrder(
        val id: String,
        val price: String?,
        val size: String?,
        val product_id: String,
        val side: String,
        val stp: String,
        val funds: String?,
        val specified_funds: String?,
        val type: String,
        val time_in_force: String,
        val expire_time: String?,
        val post_only: Boolean,
        val created_at: String,
        val done_at: String?,
        val done_reason: String,
        val fill_fees: String,
        val filled_size: String,
        val executed_value: String,
        val status: String,
        val settled: Boolean,
        val stop: String?,
        val stop_price: String?)

data class CBProFill(
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

data class CBProTicker(
        val trade_id: Int,
        val price: String?,
        val size: String?,
        val volume: String,
        val time: String?)

data class CBProTime(
        val iso: String,
        val epoch: Double)

//data class ApiCurrencies(
//        val id: String,
//        val name: String,
//        val min_size: String)
//
//data class ApiStats(
//        val open: String,
//        val high: String,
//        val low: String,
//        val volume: String)
//
//data class ApiTime(
//        val iso: String,
//        val epoch: String)

@SuppressLint("ParcelCreator")
@Parcelize
data class CBProAccount(
        val id: String,
        val currency: String,
        val balance: String,
        val holds: String,
        val available: String,
        val profile_id: String) : Parcelable

data class ApiCoinbaseAccount(
        val id: String,
        val name: String,
        val balance: String,
        val currency: String,
        val type: String,
        val primary: Boolean,
        val active: Boolean
        /* val wire_deposit_information: String */)

data class CBProPaymentMethod(
        val id: String,
        val type: String,
        val name: String,
        val currency: String,
        val balance: String?,
        val primary_buy: Boolean,
        val primary_sell: Boolean,
        val allow_deposit: Boolean,
        val allow_withdraw: Boolean
        /* val limits: String */)

data class CBProReportInfo(
        val id: String,
        val type: String,
        val status: String,
        val created_at: String?,
        val completed_at: String,
        val expires_at: String,
        val file_url: String?
        /* val params: String */)

data class CBProDepositAddress(
        val id: String,
        val address: String,
        val name: String?,
        val created_at: String?,
        val updated_at: String?,
        val expires_at: String?,
        val network: String?,
        val uri_scheme: String?,
        val resource: String?,
        val resource_path: String?,
        val warning_title: String?,
        val warning_details: String?,
        val callback_url: String?,
        val exchange_deposit_address: Boolean?
)