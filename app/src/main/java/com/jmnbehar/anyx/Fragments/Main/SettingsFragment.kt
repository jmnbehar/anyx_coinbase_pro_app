package com.jmnbehar.anyx.Fragments.Main

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jmnbehar.anyx.Activities.LoginActivity
import com.jmnbehar.anyx.Activities.MainActivity
import com.jmnbehar.anyx.Activities.VerifyActivity
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_settings.view.*
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.textColor


/**
 * Created by josephbehar on 1/20/18.
 */

class SettingsFragment : RefreshFragment() {
    companion object {
        fun newInstance(): SettingsFragment
        {
            return SettingsFragment()
        }
    }

    private lateinit var titleText: TextView
    private lateinit var logoutButton: Button
    private lateinit var verifyButton: Button
    private lateinit var disclaimerButton: Button
    private lateinit var darkModeCheckBox: CheckBox
    private lateinit var showTradeConfirmCheckBox: CheckBox
    private lateinit var showSendConfirmCheckBox: CheckBox

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_settings, container, false)

        titleText = rootView.txt_setting_title
        logoutButton = rootView.btn_setting_log_out
        verifyButton = rootView.btn_setting_verify_account
        disclaimerButton = rootView.btn_setting_show_disclaimer
        darkModeCheckBox = rootView.cb_setting_dark_mode
        showTradeConfirmCheckBox = rootView.cb_setting_show_trade_confirm
        showSendConfirmCheckBox = rootView.cb_setting_show_send_confirm

        showDarkMode(rootView)

        val prefs = Prefs(activity!!)

        if (GdaxApi.isLoggedIn) {
            logoutButton.text = "Log Out"
        } else {
            logoutButton.text = "Log In"
        }

        logoutButton.setOnClickListener  {
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(Constants.logout, true)
            prefs.shouldAutologin = false
            GdaxApi.credentials = null
            prefs.stashOrders(null)
            prefs.stashFills(null)
            startActivity(intent)
            activity!!.finishAffinity()
        }

        //TODO: uncomment this when done testing verification
//        if (!GdaxApi.isLoggedIn || GdaxApi.credentials?.isValidated == true) {
//            verifyButton.visibility = View.GONE
//        } else {
//            verifyButton.visibility = View.VISIBLE
//        }

        verifyButton.setOnClickListener  {
            var verifyAccount: Account? = null
            GdaxApi.accounts().updateAllAccounts({ result ->
                toast("Gdax Server Error")
            }, {
                if (Account.list.isNotEmpty()) {
                    val nonEmptyAccounts = Account.list.filter { account -> account.balance >= account.currency.maxVerifyAmount }
                    verifyAccount     = nonEmptyAccounts.find { account -> account.currency == Currency.ETH }
                    if (verifyAccount == null) {
                        verifyAccount = nonEmptyAccounts.find { account -> account.currency == Currency.BTC }
                    }
                    if (verifyAccount == null) {
                        verifyAccount = nonEmptyAccounts.find { account -> account.currency == Currency.BCH }
                    }
                    if (verifyAccount == null) {
                        verifyAccount = nonEmptyAccounts.find { account -> account.currency == Currency.LTC }
                    }
                }
                if (verifyAccount != null) {
                    val currency = verifyAccount!!.currency
                    launchVerificationActivity(currency, VerificationFundSource.GDAX)
                } else {
                    GdaxApi.coinbaseAccounts().linkToAccounts({ result ->
                        launchVerificationActivity(Currency.BTC, VerificationFundSource.Buy)
                    }, {
                        val cbAccounts = Account.list.mapNotNull { account -> account.coinbaseAccount }
                        val nonEmptyCBAccounts = cbAccounts.filter { cbAccount -> cbAccount.balance > cbAccount.currency.maxVerifyAmount }
                        var verifyCBAccount = nonEmptyCBAccounts.find { account -> account.currency == Currency.ETH }
                        if (verifyCBAccount == null) {
                            verifyCBAccount = nonEmptyCBAccounts.find { account -> account.currency == Currency.BTC }
                        }
                        if (verifyCBAccount == null) {
                            verifyCBAccount = nonEmptyCBAccounts.find { account -> account.currency == Currency.BCH }
                        }
                        if (verifyCBAccount == null) {
                            verifyCBAccount = nonEmptyCBAccounts.find { account -> account.currency == Currency.LTC }
                        }
                        if (verifyCBAccount == null) {
                            launchVerificationActivity(Currency.BTC, VerificationFundSource.Buy)
                        } else {
                            launchVerificationActivity(verifyCBAccount.currency, VerificationFundSource.Coinbase)
                        }
                    })
                }


            })

//            val intent = Intent(activity, VerifyActivity::class.java)
////            intent.putExtra(Constants.isMobileLoginHelp, true)
//
//            startActivity(intent)
        }

        showTradeConfirmCheckBox.isChecked = prefs.shouldShowTradeConfirmModal
        showTradeConfirmCheckBox.setOnCheckedChangeListener {  _, isChecked ->
            prefs.shouldShowTradeConfirmModal = isChecked
        }

        showSendConfirmCheckBox.isChecked = prefs.shouldShowSendConfirmModal
        showSendConfirmCheckBox.setOnCheckedChangeListener {  _, isChecked ->
            prefs.shouldShowSendConfirmModal = isChecked
        }

        darkModeCheckBox.isChecked = prefs.isDarkModeOn
        darkModeCheckBox.setOnCheckedChangeListener {  _, isChecked ->
            prefs.isDarkModeOn = isChecked
            showDarkMode()

            if (isChecked) {
                darkModeCheckBox.textColor = Color.WHITE
                showTradeConfirmCheckBox.textColor = Color.WHITE
                showSendConfirmCheckBox.textColor = Color.WHITE
                titleText.textColor = Color.WHITE
            } else {
                darkModeCheckBox.textColor = Color.BLACK
                showTradeConfirmCheckBox.textColor = Color.BLACK
                showSendConfirmCheckBox.textColor = Color.BLACK
                titleText.textColor = Color.BLACK
            }
        }
        showDarkMode()


        return rootView
    }


    fun launchVerificationActivity(currency: Currency?, verificationFundSource: VerificationFundSource) {
        val credentials = GdaxApi.credentials
        (activity as MainActivity).showProgressBar()
        if (credentials != null) {

            AnyxApi.Verify(credentials.apiKey, currency ?: Currency.BTC).executePost({
                doneLoading()
                toast("Cannot access AnyX servers.")
            }, { result ->
                doneLoading()
                val intent = Intent(activity, VerifyActivity::class.java)

                val byteArray = result.component1()
                if (byteArray != null) {
                    val responseString = String(byteArray)
                    val dataAmount: AnyXVerify = Gson().fromJson(responseString, object : TypeToken<AnyXVerify>() {}.type)
                    val amount = dataAmount.data.amount.toDoubleOrNull()
                    if (amount != null) {
                        intent.putExtra(Constants.verifyAmount, amount)
                        intent.putExtra(Constants.verifyCurrency, currency.toString())
                        intent.putExtra(Constants.verifyFundSource, verificationFundSource.toString())
                        startActivity(intent)
                    } else {
                        toast("Error")
                    }
                } else {
                    toast("Error")
                }
            })
        }
    }
}