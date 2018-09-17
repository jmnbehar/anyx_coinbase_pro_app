package com.anyexchange.anyx.classes

import com.anyexchange.anyx.fragments.login.LoginFragment
import com.anyexchange.anyx.fragments.main.*


enum class FragmentType {
    CHART,
    ACCOUNT,
    SEND,
    RECEIVE,
    SEND_RECEIVE,
    ALERTS,
    TRANSFER,
    SETTINGS,
    TRADE,
    HOME,
    LOGIN,
    EULA,
    OTHER;


    override fun toString() : String {
        return when (this) {
            CHART -> "CHART"
            ACCOUNT -> "ACCOUNT"
            SEND -> "SEND"
            RECEIVE -> "RECEIVE"
            SEND_RECEIVE -> "SEND_RECEIVE"
            ALERTS -> "ALERTS"
            TRANSFER -> "TRANSFER"
            SETTINGS -> "SETTINGS"
            TRADE -> "TRADE"
            HOME -> "HOME"
            LOGIN -> "LOGIN"
            EULA -> "EULA"
            OTHER -> "OTHER"
        }
    }

    companion object {
        fun forString(tag: String) : FragmentType {
            for (fragmentType in FragmentType.values()) {
                if (tag == fragmentType.toString()) {
                    return fragmentType
                }
            }
            return OTHER
        }

        fun forFragment(fragment: RefreshFragment?) : FragmentType {
            return when (fragment) {
                is ChartFragment -> CHART
                is AccountsFragment -> ACCOUNT
                is SendFragment -> SEND
                is ReceiveFragment -> RECEIVE
                is SendReceiveFragment -> SEND_RECEIVE
                is AlertsFragment -> ALERTS
                is TransferFragment -> TRANSFER
                is SettingsFragment -> SETTINGS
                is TradeFragment -> TRADE
                is LoginFragment -> LOGIN
                is HomeFragment -> HOME
                is EulaFragment -> EULA
                else -> OTHER
            }
        }
    }
}