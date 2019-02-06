package com.anyexchange.anyx.classes

import com.anyexchange.anyx.fragments.login.LoginFragment
import com.anyexchange.anyx.fragments.main.*


enum class FragmentType {
    CHART,
    BALANCES,
    ACCOUNTS,
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
            BALANCES -> "BALANCES"
            ACCOUNTS -> "ACCOUNTS"
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
                is BalancesFragment -> BALANCES
                is AccountsFragment -> ACCOUNTS
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