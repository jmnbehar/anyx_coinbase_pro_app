package com.anyexchange.anyx.fragments.main

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.activities.VerifyActivity
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.activities.MainActivity
import com.anyexchange.anyx.adapters.spinnerAdapters.CurrencySpinnerAdapter
import com.anyexchange.anyx.adapters.spinnerAdapters.FloatSpinnerAdapter
import com.anyexchange.anyx.api.AnyApi
import com.anyexchange.anyx.api.CBProApi
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.android.synthetic.main.fragment_settings.view.*
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

    private var titleText: TextView? = null
    private var logoutButton: Button? = null
    private var verifyButton: Button? = null
    private var cbproEulaButton: Button? = null
    private var anyxEulaButton: Button? = null
    private var emailDevButton: Button? = null
    private var updateProductsButton: Button? = null
    private var darkModeCheckBox: CheckBox? = null
    private var showTradeConfirmCheckBox: CheckBox? = null
    private var showSendConfirmCheckBox: CheckBox? = null
    private var quickChangeThresholdSpinner: Spinner? = null
    private var defaultQuoteCurrencySpinner: Spinner? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_settings, container, false)

        titleText = rootView.txt_setting_title
        logoutButton = rootView.btn_setting_log_out
        verifyButton = rootView.btn_setting_verify_account
        cbproEulaButton = rootView.btn_setting_show_cbpro_eula
        anyxEulaButton = rootView.btn_setting_show_anyx_eula
        emailDevButton = rootView.btn_setting_email_dev
        updateProductsButton = rootView.btn_setting_update_products
        darkModeCheckBox = rootView.cb_setting_dark_mode
        showTradeConfirmCheckBox = rootView.cb_setting_show_trade_confirm
        showSendConfirmCheckBox = rootView.cb_setting_show_send_confirm
        quickChangeThresholdSpinner = rootView.spinner_settings_quick_change_threshold
        defaultQuoteCurrencySpinner = rootView.spinner_settings_default_quote

        showDarkMode(rootView)


        logoutButton?.setOnClickListener  {
            logOut()
        }

        verifyButton?.setOnClickListener  {
            val intent = Intent(activity, VerifyActivity::class.java)
            startActivity(intent)
        }

        cbproEulaButton?.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coinbase.com/legal/user_agreement"))
            startActivity(browserIntent)
        }

        emailDevButton?.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:anyx.app@gmail.com")
            intent.putExtra(Intent.EXTRA_SUBJECT, "AnyX Feedback")

            context?.packageManager?.let { packageManager ->
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
            }
        }

        updateProductsButton?.setOnClickListener { _ ->
            showProgressSpinner()
            val onFailure: (Result.Failure<String, FuelError>) -> Unit = {
                dismissProgressSpinner()
                toast("Failed to update products")
            }
            val anyApi = AnyApi(apiInitData)
            anyApi.getAllProducts(onFailure) {
                anyApi.getAllAccounts(onFailure, {
                    dismissProgressSpinner()
                    toast("Products updated")
                })
            }

        }

        (activity as? MainActivity)?.let { activity ->
            anyxEulaButton?.visibility = View.VISIBLE
            anyxEulaButton?.setOnClickListener {
                activity.goToFragment(FragmentType.EULA)
            }
        } ?: run {
            anyxEulaButton?.visibility = View.GONE
        }

        val context = context!!
        val prefs = Prefs(context)

        val tempThreshold = prefs.quickChangeThreshold
        quickChangeThresholdSpinner?.visibility = View.VISIBLE
        val floatList = listOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
        val arrayAdapter = FloatSpinnerAdapter(context, floatList)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        quickChangeThresholdSpinner?.adapter = arrayAdapter
        quickChangeThresholdSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.quickChangeThreshold = floatList[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        val quickChangeThresholdIndex = floatList.indexOf(tempThreshold)
        if (quickChangeThresholdIndex != -1) {
            quickChangeThresholdSpinner?.setSelection(quickChangeThresholdIndex)
        }


        val tempDefaultQuote = prefs.defaultQuoteCurrency
        val defaultQuoteList = Currency.validDefaultQuotes
        defaultQuoteCurrencySpinner?.visibility = View.VISIBLE
        val currencyAdapter = CurrencySpinnerAdapter(context, defaultQuoteList)
        defaultQuoteCurrencySpinner?.adapter = currencyAdapter
        defaultQuoteCurrencySpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.defaultQuoteCurrency = defaultQuoteList[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        val defaultQuoteIndex = defaultQuoteList.indexOf(tempDefaultQuote)
        if (defaultQuoteIndex != -1) {
            defaultQuoteCurrencySpinner?.setSelection(defaultQuoteIndex)
        }

        showTradeConfirmCheckBox?.isChecked = prefs.shouldShowTradeConfirmModal
        showTradeConfirmCheckBox?.setOnCheckedChangeListener {  _, isChecked ->
            prefs.shouldShowTradeConfirmModal = isChecked
        }

        showSendConfirmCheckBox?.isChecked = prefs.shouldShowSendConfirmModal
        showSendConfirmCheckBox?.setOnCheckedChangeListener {  _, isChecked ->
            prefs.shouldShowSendConfirmModal = isChecked
        }

        darkModeCheckBox?.isChecked = prefs.isDarkModeOn
        darkModeCheckBox?.setOnCheckedChangeListener {  _, isChecked ->
            prefs.isDarkModeOn = isChecked
            showDarkMode()

            if (isChecked) {
                darkModeCheckBox?.textColor = Color.WHITE
                showTradeConfirmCheckBox?.textColor = Color.WHITE
                showSendConfirmCheckBox?.textColor = Color.WHITE
                titleText?.textColor = Color.WHITE
            } else {
                darkModeCheckBox?.textColor = Color.BLACK
                showTradeConfirmCheckBox?.textColor = Color.BLACK
                showSendConfirmCheckBox?.textColor = Color.BLACK
                titleText?.textColor = Color.BLACK
            }
        }
        showDarkMode()

        return rootView
    }

    private fun logOut() {
        context?.let {
            val prefs = Prefs(it)
            CBProApi.credentials = null
            for (product in Product.map.values) {
                product.accounts = mapOf()
            }
            prefs.stashProducts()
            prefs.nukeStashedOrders()
            prefs.nukeStashedFills()
        }
    }


    override fun onResume() {
        super.onResume()
        dismissProgressSpinner()
        context?.let {
            val prefs = Prefs(it)
            val apiKey = CBProApi.credentials?.apiKey
            when {
                apiKey == null -> verifyButton?.visibility = View.GONE
                prefs.isApiKeyValid(apiKey) == true -> {
                    verifyButton?.visibility = View.GONE
                    txt_setting_verify_account.visibility = View.GONE
                }
                else -> {
                    verifyButton?.visibility = View.VISIBLE
                    txt_setting_verify_account.visibility = View.VISIBLE
                }
            }
        }
    }
}