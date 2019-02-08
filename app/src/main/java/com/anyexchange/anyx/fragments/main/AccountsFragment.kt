package com.anyexchange.anyx.fragments.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.adapters.ExchangeAccountsListViewAdapter
import com.anyexchange.anyx.api.AnyApi
import com.anyexchange.anyx.api.BinanceApi
import com.anyexchange.anyx.api.CBProApi
import com.anyexchange.anyx.classes.*
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import kotlinx.android.synthetic.main.fragment_accounts.view.*
import org.jetbrains.anko.toast
import se.simbio.encryption.Encryption

class AccountsFragment : RefreshFragment() {
    private var exchangeAccountListAdapter: ExchangeAccountsListViewAdapter? = null

    private class ExchangeAccountCell {
        var exchangeLogoView: ImageView? = null
        var exchangeNameView: TextView? = null

        var apiKeyLayout: LinearLayout? = null
        var apiKeyLabelView: TextView? = null
        var apiKeyView: TextView? = null

        //TODO: consider adding a permissions view here
        //TODO: consider adding a test button here


        var shouldShowEditLayout = false
        var loginEditLayout: LinearLayout? = null
        var apiKeyEditText: EditText? = null
        var apiSecretEditText: EditText? = null
        var passphraseEditText: EditText? = null

        var loginButton: Button? = null
        var logoutButton: Button? = null


        fun setupCell(context: Context, exchange: Exchange, genericLogOut: () -> Unit, refreshAllProductsAndAccounts: () -> Unit) {
            exchangeLogoView?.setImageResource(R.drawable.fail_icon)
            exchangeNameView?.text = exchange.name

            if (shouldShowEditLayout) {
                setToEditMode(context, exchange, genericLogOut, refreshAllProductsAndAccounts)
            } else {
                setToStaticMode(context, exchange, genericLogOut, refreshAllProductsAndAccounts)
            }

            val isLoggedIn = when (exchange) {
                Exchange.CBPro -> CBProApi.credentials != null
                Exchange.Binance -> BinanceApi.credentials != null
            }
            if (isLoggedIn) {
                logoutButton?.visibility = View.VISIBLE
                logoutButton?.setOnClickListener {
                    when (exchange) {
                        Exchange.CBPro -> CBProApi.credentials = null
                        Exchange.Binance -> BinanceApi.credentials = null
                    }
                    setToEditMode(context, exchange, genericLogOut, refreshAllProductsAndAccounts)
                    genericLogOut()
                }
            } else {
                logoutButton?.visibility = View.GONE
            }
        }

        fun setToStaticMode(context: Context, exchange: Exchange, genericLogOut: () -> Unit, refreshAllProductsAndAccounts: () -> Unit) {

            loginEditLayout?.visibility = View.GONE
            apiKeyLayout?.visibility = View.VISIBLE

            val apiKey: String? = when (exchange) {
                Exchange.CBPro -> CBProApi.credentials?.apiKey
                Exchange.Binance -> BinanceApi.credentials?.apiKey
            }

            if (apiKey != null) {
                apiKeyLabelView?.visibility = View.VISIBLE
                apiKeyView?.visibility = View.VISIBLE
                apiKeyLabelView?.text = "API Key:"
                apiKeyView?.text = apiKey

                loginButton?.text = "Change Log In Info"
            } else {
                apiKeyLabelView?.visibility = View.INVISIBLE
                apiKeyView?.visibility = View.INVISIBLE

                loginButton?.visibility = View.VISIBLE
                loginButton?.text = "Log In"
            }

            loginButton?.setOnClickListener {
                setToEditMode(context, exchange, genericLogOut, refreshAllProductsAndAccounts)
            }
        }

        fun setToEditMode(context: Context, exchange: Exchange, genericLogOut: () -> Unit, refreshAllProductsAndAccounts: () -> Unit) {
            val prefs = Prefs(context)

            val apiKey: String?
            val apiSecret: String?
            var apiPassphrase: String? = null
            when (exchange) {
                Exchange.CBPro -> {
                    apiKey = CBProApi.credentials?.apiKey ?: prefs.cbProApiKey
                    apiSecret = CBProApi.credentials?.apiSecret ?: prefs.cbProApiSecret

                    passphraseEditText?.visibility = View.VISIBLE
                    val encryptedPassphrase = CBProApi.credentials?.apiPassPhrase ?: prefs.cbProPassphrase
                    val iv = ByteArray(16)
                    val encryption = Encryption.getDefault(apiKey, apiSecret + Constants.salt, iv)
                    apiPassphrase = encryption.decryptOrNull(encryptedPassphrase)
                }
                Exchange.Binance -> {
                    apiKey = BinanceApi.credentials?.apiKey ?: prefs.binanceApiKey
                    apiSecret = BinanceApi.credentials?.apiSecret ?: prefs.binanceApiSecret
                    passphraseEditText?.visibility = View.GONE
                }
            }
            //show editable login info
            loginEditLayout?.visibility = View.VISIBLE
            apiKeyLayout?.visibility = View.GONE

            loginButton?.visibility = View.VISIBLE
            loginButton?.text = "Save Log In Info"

            if(apiKey != null) {
                apiKeyEditText?.setText(apiKey)
            }
            if (apiSecret != null) {
                apiSecretEditText?.setText(apiSecret)
            }

            if (apiPassphrase != null) {
                passphraseEditText?.setText(apiPassphrase)
            }

            apiKeyEditText?.requestFocus()
            apiKeyEditText?.isSelected = true

            loginButton?.setOnClickListener {

                val newApiKey = apiKeyEditText?.text.toString().trimEnd()
                val newApiSecret = apiSecretEditText?.text.toString().trimEnd()
                val newApiPassphrase = passphraseEditText?.text.toString().trimEnd()

                val didChangeLoginInfo = when (exchange) {
                    Exchange.CBPro -> {
                        newApiKey != CBProApi.credentials?.apiKey || newApiSecret != CBProApi.credentials?.apiSecret || newApiPassphrase != CBProApi.credentials?.apiPassPhrase
                    }
                    Exchange.Binance -> {
                        newApiKey != BinanceApi.credentials?.apiKey || newApiSecret != BinanceApi.credentials?.apiSecret
                    }
                }
                if (!didChangeLoginInfo) {
                    setToStaticMode(context, exchange, genericLogOut, refreshAllProductsAndAccounts)
                } else {
                    when {
                        newApiKey.isBlank() -> context.toast(R.string.login_error_missing_api_key)
                        newApiSecret.isBlank() -> context.toast(R.string.login_error_missing_api_secret)
                        newApiPassphrase.isBlank() -> context.toast(R.string.login_error_missing_passphrase)
                        else -> {

                            when (exchange) {
                                Exchange.CBPro -> {
                                    if (prefs.shouldSaveApiInfo) {
                                        val iv = ByteArray(16)
                                        val encryption = Encryption.getDefault(apiKey, apiSecret + Constants.salt, iv)
                                        val passphraseEncrypted = encryption.encryptOrNull(newApiPassphrase)
                                        prefs.cbProApiKey = apiKey
                                        prefs.cbProApiSecret = apiSecret
                                        if (prefs.shouldSavePassphrase) {
                                            prefs.cbProPassphrase = passphraseEncrypted
                                        }
                                    }
                                    val isApiKeyValid = prefs.isApiKeyValid(newApiKey)
                                    CBProApi.credentials = CBProApi.ApiCredentials(newApiKey, newApiSecret, newApiPassphrase, isApiKeyValid)
                                    refreshAllProductsAndAccounts()
                                }
                                Exchange.Binance -> {
                                    if (prefs.shouldSaveApiInfo) {
                                        prefs.binanceApiKey = apiKey
                                        prefs.binanceApiSecret = apiSecret
                                    }
                                    BinanceApi.credentials = BinanceApi.ApiCredentials(newApiKey, newApiSecret)
                                    refreshAllProductsAndAccounts()
                                }
                            }
                            setToStaticMode(context, exchange, genericLogOut, refreshAllProductsAndAccounts)
                        }
                    }
                }
            }
        }

    }


    private var cbProAccountCell = ExchangeAccountCell()
    private var binanceAccountCell = ExchangeAccountCell()

    companion object {

        fun newInstance() : AccountsFragment {
            return AccountsFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_accounts, container, false)
        setupSwipeRefresh(rootView.swipe_refresh_layout)

        val context = context!!

        cbProAccountCell.exchangeLogoView = rootView.img_exchange_cbpro_logo
        cbProAccountCell.exchangeNameView = rootView.txt_exchange_cbpro_name
        cbProAccountCell.apiKeyLayout = rootView.layout_exchange_cbpro_account_api_key
        cbProAccountCell.apiKeyLabelView = rootView.txt_exchange_cbpro_account_api_key_label
        cbProAccountCell.apiKeyView = rootView.txt_exchange_cbpro_account_api_key
        cbProAccountCell.loginEditLayout = rootView.layout_exchange_cbpro_account_edit_login
        cbProAccountCell.apiKeyEditText = rootView.etxt_exchange_cbpro_account_api_key
        cbProAccountCell.apiSecretEditText = rootView.etxt_exchange_cbpro_account_secret
        cbProAccountCell.passphraseEditText = rootView.etxt_exchange_cbpro_account_passphrase
        cbProAccountCell.loginButton = rootView.btn_exchange_cbpro_account_login
        cbProAccountCell.logoutButton = rootView.btn_exchange_cbpro_account_logout

        cbProAccountCell.setupCell(context, Exchange.CBPro, { genericLogOut() }, { refreshAllProductsAndAccounts() })

        binanceAccountCell.exchangeLogoView = rootView.img_exchange_binance_logo
        binanceAccountCell.exchangeNameView = rootView.txt_exchange_binance_name
        binanceAccountCell.apiKeyLayout = rootView.layout_exchange_binance_account_api_key
        binanceAccountCell.apiKeyLabelView = rootView.txt_exchange_binance_account_api_key_label
        binanceAccountCell.apiKeyView = rootView.txt_exchange_binance_account_api_key
        binanceAccountCell.loginEditLayout = rootView.layout_exchange_binance_account_edit_login
        binanceAccountCell.apiKeyEditText = rootView.etxt_exchange_binance_account_api_key
        binanceAccountCell.apiSecretEditText = rootView.etxt_exchange_binance_account_secret
        binanceAccountCell.passphraseEditText = rootView.etxt_exchange_binance_account_passphrase
        binanceAccountCell.loginButton = rootView.btn_exchange_binance_account_login
        binanceAccountCell.logoutButton = rootView.btn_exchange_binance_account_logout

        binanceAccountCell.setupCell(context, Exchange.Binance, { genericLogOut() }, { refreshAllProductsAndAccounts() })

        return rootView
    }

    override fun refresh(onComplete: (Boolean) -> Unit) {
        super.refresh(onComplete)
        exchangeAccountListAdapter?.notifyDataSetChanged()
    }



    private fun genericLogOut() {
        val prefs = Prefs(context!!)
        prefs.isLoggedIn = false
        for (product in Product.map.values) {
            product.accounts = mapOf()
        }
        prefs.stashedProducts = Product.map.values.toList()
        prefs.nukeStashedOrders()
        prefs.nukeStashedFills()
    }

    private fun refreshAllProductsAndAccounts() {
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
}
