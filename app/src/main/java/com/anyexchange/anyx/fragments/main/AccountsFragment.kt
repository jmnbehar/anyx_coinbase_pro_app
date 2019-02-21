package com.anyexchange.anyx.fragments.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat.startActivity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.activities.LoginHelpActivity
import com.anyexchange.anyx.api.AnyApi
import com.anyexchange.anyx.api.BinanceApi
import com.anyexchange.anyx.api.CBProApi
import com.anyexchange.anyx.classes.*
import kotlinx.android.synthetic.main.fragment_accounts.view.*
import org.jetbrains.anko.toast
import se.simbio.encryption.Encryption

class AccountsFragment : RefreshFragment() {
    private class ExchangeAccountCell(
            private var layout: LinearLayout,

            private var exchangeLogoView: ImageView,
            private var exchangeNameView: TextView,

            private var apiKeyLayout: LinearLayout,
            private var apiKeyLabelView: TextView,
            private var apiKeyView: TextView,

            //TODO: consider adding a permissions view here
            //TODO: consider adding a test button here

            private var loginEditLayout: LinearLayout,
            private var apiKeyEditText: EditText,
            private var apiSecretEditText: EditText,
            private var passphraseEditText: EditText?,

            private var helpButton: Button,
            private var cancelButton: Button,
            private var loginButton: Button,
            private var logoutButton: Button) {

        var shouldShowEditLayout = false

        fun hide() {
            layout.visibility = View.GONE
        }

        fun setupCell(context: Context, exchange: Exchange, genericLogOut: () -> Unit, refreshAllProductsAndAccounts: () -> Unit) {
            layout.visibility = View.VISIBLE

            exchangeLogoView.setImageResource(exchange.iconId)
            exchangeNameView.text = exchange.toString()

            if (shouldShowEditLayout) {
                setToEditMode(context, exchange, genericLogOut, refreshAllProductsAndAccounts)
            } else {
                setToStaticMode(context, exchange, genericLogOut, refreshAllProductsAndAccounts)
            }

            logoutButton.setOnClickListener {
                when (exchange) {
                    Exchange.CBPro -> CBProApi.credentials = null
                    Exchange.Binance -> BinanceApi.credentials = null
                }
                genericLogOut()
            }

            cancelButton.setOnClickListener {
                setToStaticMode(context, exchange, genericLogOut, refreshAllProductsAndAccounts)
            }

            helpButton.setOnClickListener {
                //Creating the instance of PopupMenu
                val popup = PopupMenu(context, helpButton)
                //Inflating the Popup using xml file
                popup.menuInflater.inflate(R.menu.login_help_menu, popup.menu)
                //registering popup with OnMenuItemClickListener
                val intent = Intent(context, LoginHelpActivity::class.java)
                popup.setOnMenuItemClickListener { item: MenuItem? ->
                    when (item?.itemId ?: R.id.login_help_mobile) {
                        R.id.login_help_mobile -> {
                            popup.dismiss()
                            intent.putExtra(Constants.isMobileLoginHelp, true)
                            startActivity(context, intent, null)
                        }
                        R.id.login_help_desktop -> {
                            popup.dismiss()
                            intent.putExtra(Constants.isMobileLoginHelp, false)
                            startActivity(context, intent, null)
                        }
                    }
                    true
                }
                popup.show()  //showing popup menu
            }


            if (exchange.isLoggedIn()) {
                logoutButton.visibility = View.VISIBLE
            } else {
                logoutButton.visibility = View.GONE
            }
        }

        fun setToStaticMode(context: Context, exchange: Exchange, genericLogOut: () -> Unit, refreshAllProductsAndAccounts: () -> Unit) {

            loginEditLayout.visibility = View.GONE
            apiKeyLayout.visibility = View.VISIBLE

            val apiKey: String? = when (exchange) {
                Exchange.CBPro -> CBProApi.credentials?.apiKey
                Exchange.Binance -> BinanceApi.credentials?.apiKey
            }

            if (apiKey != null) {
                apiKeyLabelView.visibility = View.VISIBLE
                apiKeyView.visibility = View.VISIBLE
                apiKeyLabelView.text = context.getString(R.string.accounts_api_key_label)
                apiKeyView.text = apiKey

                loginButton.text = context.getString(R.string.accounts_change_login_info)
            } else {
                apiKeyLabelView.visibility = View.INVISIBLE
                apiKeyView.visibility = View.INVISIBLE

                loginButton.visibility = View.VISIBLE
                loginButton.text = context.getString(R.string.accounts_login_button_label)

            }

            cancelButton.visibility = View.GONE
            helpButton.visibility = View.GONE

            loginButton.setOnClickListener {
                setToEditMode(context, exchange, genericLogOut, refreshAllProductsAndAccounts)
            }

            if (exchange.isLoggedIn()) {
                logoutButton.visibility = View.VISIBLE
            } else {
                logoutButton.visibility = View.GONE
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

                    apiPassphrase = CBProApi.credentials?.apiPassPhrase

                    if (apiPassphrase == null) {
                        val encryptedPassphrase = prefs.cbProPassphrase
                        val iv = ByteArray(16)
                        val encryption = Encryption.getDefault(apiKey, apiSecret + Constants.salt, iv)

                        apiPassphrase = encryption.decryptOrNull(encryptedPassphrase)
                    }
                }
                Exchange.Binance -> {
                    apiKey = BinanceApi.credentials?.apiKey ?: prefs.binanceApiKey
                    apiSecret = BinanceApi.credentials?.apiSecret ?: prefs.binanceApiSecret
                    passphraseEditText?.visibility = View.GONE
                }
            }
            //show editable login info
            loginEditLayout.visibility = View.VISIBLE
            apiKeyLayout.visibility = View.GONE

            loginButton.visibility = View.VISIBLE
            loginButton.text = context.getString(R.string.accounts_save_login_info)

            if(apiKey != null) {
                apiKeyEditText.setText(apiKey)
            }
            if (apiSecret != null) {
                apiSecretEditText.setText(apiSecret)
            }

            if (apiPassphrase != null) {
                passphraseEditText?.setText(apiPassphrase)
            }

            apiKeyEditText.requestFocus()
            apiKeyEditText.isSelected = true

            cancelButton.visibility = View.VISIBLE
            helpButton.visibility = View.VISIBLE

            loginButton.setOnClickListener {
                login(context, exchange, genericLogOut, refreshAllProductsAndAccounts)
            }
        }


        fun login(context: Context, exchange: Exchange, genericLogOut: () -> Unit, refreshAllProductsAndAccounts: () -> Unit) {
            val newApiKey = apiKeyEditText.text.toString().trimEnd()
            val newApiSecret = apiSecretEditText.text.toString().trimEnd()
            val newApiPassphrase = passphraseEditText?.text.toString().trimEnd()
            val prefs = Prefs(context)

            val didChangeLoginInfo = when (exchange) {
                Exchange.CBPro -> {
                    newApiKey != CBProApi.credentials?.apiKey || newApiSecret != CBProApi.credentials?.apiSecret || newApiPassphrase != CBProApi.credentials?.apiPassPhrase
                }
                Exchange.Binance -> {
                    newApiKey != BinanceApi.credentials?.apiKey || newApiSecret != BinanceApi.credentials?.apiSecret
                }
            }
            if (!didChangeLoginInfo) {
                when (exchange) {
                    Exchange.CBPro -> {
                        if (prefs.cbProApiKey == null) {
                            prefs.stashCBProCreds(newApiKey, newApiSecret, newApiPassphrase)
                        }
                    }
                    Exchange.Binance -> {
                        if (prefs.binanceApiKey == null) {
                            prefs.stashBinanceCreds(newApiKey, newApiSecret)
                        }
                    }
                }
                setToStaticMode(context, exchange, genericLogOut, refreshAllProductsAndAccounts)
            } else {
                when {
                    newApiKey.isBlank() -> context.toast(R.string.login_error_missing_api_key)
                    newApiSecret.isBlank() -> context.toast(R.string.login_error_missing_api_secret)
                    newApiPassphrase.isBlank() -> context.toast(R.string.login_error_missing_passphrase)
                    else -> {

                        when (exchange) {
                            Exchange.CBPro -> {
                                prefs.stashCBProCreds(newApiKey, newApiSecret, newApiPassphrase)
                                val isApiKeyValid = prefs.isApiKeyValid(newApiKey)
                                CBProApi.credentials = CBProApi.ApiCredentials(newApiKey, newApiSecret, newApiPassphrase, isApiKeyValid)
                                refreshAllProductsAndAccounts()
                            }
                            Exchange.Binance -> {
                                prefs.stashBinanceCreds(newApiKey, newApiSecret)

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

    private lateinit var cbProAccountCell : ExchangeAccountCell
    private lateinit var binanceAccountCell : ExchangeAccountCell

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

        cbProAccountCell = ExchangeAccountCell(rootView.layout_exchange_cbpro,
                rootView.img_exchange_cbpro_logo,
                rootView.txt_exchange_cbpro_name,
                rootView.layout_exchange_cbpro_account_api_key,
                rootView.txt_exchange_cbpro_account_api_key_label,
                rootView.txt_exchange_cbpro_account_api_key,
                rootView.layout_exchange_cbpro_account_edit_login,
                rootView.etxt_exchange_cbpro_account_api_key,
                rootView.etxt_exchange_cbpro_account_secret,
                rootView.etxt_exchange_cbpro_account_passphrase,
                rootView.btn_accounts_cbpro_help,
                rootView.btn_accounts_cbpro_cancel,
                rootView.btn_exchange_cbpro_account_login,
                rootView.btn_exchange_cbpro_account_logout)

        cbProAccountCell.setupCell(context, Exchange.CBPro, { genericLogOut(Exchange.CBPro) }, { refreshAllProductsAndAccounts() })


        binanceAccountCell = ExchangeAccountCell(rootView.layout_exchange_binance,
                rootView.img_exchange_binance_logo,
                rootView.txt_exchange_binance_name,
                rootView.layout_exchange_binance_account_api_key,
                rootView.txt_exchange_binance_account_api_key_label,
                rootView.txt_exchange_binance_account_api_key,
                rootView.layout_exchange_binance_account_edit_login,
                rootView.etxt_exchange_binance_account_api_key,
                rootView.etxt_exchange_binance_account_secret,
                rootView.etxt_exchange_binance_account_passphrase,
                rootView.btn_accounts_binance_help,
                rootView.btn_accounts_binance_cancel,
                rootView.btn_exchange_binance_account_login,
                rootView.btn_exchange_binance_account_logout)

        val prefs = Prefs(context)
        if (prefs.isAnyXProActive) {
            binanceAccountCell.setupCell(context, Exchange.Binance, { genericLogOut(Exchange.Binance) }, { refreshAllProductsAndAccounts() })
        } else {
            binanceAccountCell.hide()
        }

        return rootView
    }

//    override fun refresh(onComplete: (Boolean) -> Unit) {
//        super.refresh(onComplete)
//    }

    private fun genericLogOut(exchange: Exchange) {
        showPopup("Are you sure you want to log out from $exchange? Your credentials will not be saved.", {
            for (product in Product.map.values) {
                val tempAccounts = product.accounts.toMutableMap()
                tempAccounts.remove(exchange)
                product.accounts = tempAccounts
            }
            println("nuking accounts")
            val prefs = Prefs(context!!)
            prefs.stashProducts()
            prefs.nukeStashedOrders()
            prefs.nukeStashedFills()
            when (exchange) {
                Exchange.CBPro ->  prefs.stashCBProCreds(null, null, null)
                Exchange.Binance ->  prefs.stashBinanceCreds(null, null)
            }
        }, "No", { /* do nothing */ })
    }

    private fun refreshAllProductsAndAccounts() {
        showProgressSpinner()
        AnyApi(apiInitData).reloadAllProducts(context,
                {
                    dismissProgressSpinner()
                    toast("Login Failed") },
                {
                    dismissProgressSpinner()
                    toast("Logged In")
                })
    }
}
