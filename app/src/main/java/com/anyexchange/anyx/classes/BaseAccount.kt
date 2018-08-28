package com.anyexchange.anyx.classes


abstract class BaseAccount {
    abstract val id: String
    abstract val balance: Double?
    abstract val currency: Currency
}