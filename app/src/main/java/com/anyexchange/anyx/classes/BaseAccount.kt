package com.anyexchange.anyx.classes


abstract class BaseAccount {
    abstract val id: String
    abstract val balance: Double?
    abstract val currency: Currency

    override fun equals(other: Any?): Boolean {
        return if (other is BaseAccount) {
            (other.id == this.id && other.balance == this.balance && other.currency.id == this.currency.id)
        } else {
            super.equals(other)
        }
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (balance?.hashCode() ?: 0)
        result = 31 * result + currency.hashCode()
        return result
    }
}