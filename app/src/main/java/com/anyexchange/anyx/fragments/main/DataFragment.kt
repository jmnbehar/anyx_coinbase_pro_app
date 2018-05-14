package com.anyexchange.anyx.fragments.main

import android.os.Bundle
import android.support.v4.app.Fragment
import com.anyexchange.anyx.classes.*

/**
 * Created by josephbehar on 5/11/18.
 */
class DataFragment : Fragment() {
    companion object {
        fun newInstance(): DataFragment
        {
            return DataFragment()
        }
    }

    private var backupCredentials: GdaxApi.ApiCredentials? = null
    private var backupAccountList: MutableList<Account> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // retain this fragment
        retainInstance = true
    }

    fun backupData() {
        backupCredentials = GdaxApi.credentials
        backupAccountList = Account.list
        activity?.let { activity ->
            val prefs = Prefs(activity)
            prefs.stashedAccountList = Account.list
        }
    }

    fun restoreData() {
        if (backupCredentials != null && GdaxApi.credentials == null || GdaxApi.credentials?.apiKey?.isEmpty() == true) {
            GdaxApi.credentials = backupCredentials
        }
        if (backupAccountList.isNotEmpty()) {
            Account.list = backupAccountList
        } else if (Account.list.isEmpty()){
            activity?.let { activity ->
                val prefs = Prefs(activity)
                Account.list = prefs.stashedAccountList
            }
        }
    }
}
