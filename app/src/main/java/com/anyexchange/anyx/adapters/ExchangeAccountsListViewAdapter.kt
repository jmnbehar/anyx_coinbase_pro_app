package com.anyexchange.anyx.adapters

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.api.BinanceApi
import com.anyexchange.anyx.api.CBProApi
import com.anyexchange.anyx.classes.*
import kotlinx.android.synthetic.main.fragment_login.view.*
import kotlinx.android.synthetic.main.list_row_exchange_account.view.*
import org.jetbrains.anko.toast
import se.simbio.encryption.Encryption

/**
 * Created by anyexchange on 11/12/2017.
 */

class ExchangeAccountsListViewAdapter(val context: Context, private var exchanges: Array<Exchange>, private var resources: Resources, private var refreshAllProductsAndAccounts: () -> Unit) : BaseAdapter() {

    override fun getCount(): Int {
        return exchanges.size + 1
    }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    internal class ViewHolder {
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
    }


    internal class SettingsViewHolder {
        var saveApiInfoCheckBox: CheckBox? = null
        var savePassphraseCheckBox: CheckBox? = null
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        val prefs = Prefs(context)
        if (i == exchanges.size) {
            //Show login settings
            val viewHolder: SettingsViewHolder?
            val outputView: View
            if (convertView == null) {
                viewHolder = SettingsViewHolder()

                val vi = viewGroup.inflate(R.layout.list_row_exchange_account_settings)

                viewHolder.saveApiInfoCheckBox = vi.cb_save_api_key
                viewHolder.savePassphraseCheckBox = vi.cb_save_passphrase

                vi.tag = viewHolder
                outputView = vi
            } else {
                viewHolder = convertView.tag as SettingsViewHolder
                outputView = convertView
            }
            if (prefs.shouldSaveApiInfo) {
                viewHolder.saveApiInfoCheckBox?.isChecked = true
                if (prefs.shouldSavePassphrase) {
                    viewHolder.savePassphraseCheckBox?.isChecked = true
                }
            } else {
                viewHolder.savePassphraseCheckBox?.isChecked = false
                viewHolder.savePassphraseCheckBox?.isEnabled = false
            }

            viewHolder.saveApiInfoCheckBox?.setOnCheckedChangeListener { _, isChecked ->
                prefs.shouldSaveApiInfo = isChecked
                if (!isChecked) {
                    viewHolder.savePassphraseCheckBox?.isChecked = false
                    viewHolder.savePassphraseCheckBox?.isEnabled = false
                } else {
                    viewHolder.savePassphraseCheckBox?.isEnabled = true
                }
            }
            viewHolder.savePassphraseCheckBox?.setOnCheckedChangeListener { _, isChecked ->
                prefs.shouldSavePassphrase = isChecked
            }
            return outputView
        } else {
            //show regular login cell
            val viewHolder: ViewHolder?
            val outputView: View
            if (convertView == null) {
                viewHolder = ViewHolder()

                val vi = viewGroup.inflate(R.layout.list_row_exchange_account)

                viewHolder.exchangeLogoView = vi.img_exchange_logo
                viewHolder.exchangeNameView = vi.txt_exchange_name

                viewHolder.apiKeyLayout = vi.layout_exchange_account_api_key
                viewHolder.apiKeyLabelView = vi.txt_exchange_account_api_key_label
                viewHolder.apiKeyView = vi.txt_exchange_account_api_key

                viewHolder.loginEditLayout = vi.layout_exchange_account_edit_login

                viewHolder.apiKeyEditText = vi.etxt_exchange_account_api_key
                viewHolder.apiSecretEditText = vi.etxt_exchange_account_secret
                viewHolder.passphraseEditText = vi.etxt_exchange_account_passphrase

                viewHolder.loginButton = vi.btn_exchange_account_login
                viewHolder.logoutButton = vi.btn_exchange_account_logout

                vi.tag = viewHolder
                outputView = vi
            } else {
                viewHolder = convertView.tag as ViewHolder
                outputView = convertView
            }

            val exchange = exchanges[i]

            viewHolder.exchangeLogoView?.setImageResource(R.drawable.fail_icon)

            viewHolder.exchangeNameView?.text = exchange.name


            val isLoggedIn = when (exchange) {
                Exchange.CBPro -> CBProApi.credentials != null
                Exchange.Binance -> BinanceApi.credentials != null
            }

            if (isLoggedIn) {
                viewHolder.logoutButton?.visibility = View.VISIBLE
                viewHolder.logoutButton?.setOnClickListener {
                    when (exchange) {
                        Exchange.CBPro -> CBProApi.credentials = null
                        Exchange.Binance -> BinanceApi.credentials = null
                    }
                    notifyDataSetChanged()
                    genericLogOut()
                }
            } else {
                viewHolder.logoutButton?.visibility = View.GONE
            }

            if (!viewHolder.shouldShowEditLayout) {
                //Show static cbProApiKey

                val apiKey: String? = when (exchange) {
                    Exchange.CBPro -> CBProApi.credentials?.apiKey
                    Exchange.Binance -> BinanceApi.credentials?.apiKey
                }

                viewHolder.loginEditLayout?.visibility = View.GONE
                viewHolder.apiKeyLayout?.visibility = View.VISIBLE

                if (apiKey != null) {
                    viewHolder.apiKeyLabelView?.visibility = View.VISIBLE
                    viewHolder.apiKeyView?.visibility = View.VISIBLE
                    viewHolder.apiKeyLabelView?.text = "API Key:"
                    viewHolder.apiKeyView?.text = apiKey

                    viewHolder.loginButton?.text = "Change Log In Info"
                } else {
                    viewHolder.apiKeyLabelView?.visibility = View.INVISIBLE
                    viewHolder.apiKeyView?.visibility = View.INVISIBLE

                    viewHolder.loginButton?.visibility = View.VISIBLE
                    viewHolder.loginButton?.text = "Log In"
                }

                viewHolder.loginButton?.setOnClickListener {
                    viewHolder.shouldShowEditLayout = true
                    notifyDataSetChanged()
                }

            } else {
                //Show edit layout:
                val apiKey: String?
                val apiSecret: String?
                var apiPassphrase: String? = null
                when (exchange) {
                    Exchange.CBPro -> {
                        apiKey = CBProApi.credentials?.apiKey ?: prefs.cbProApiKey
                        apiSecret = CBProApi.credentials?.apiSecret ?: prefs.cbProApiSecret

                        viewHolder.passphraseEditText?.visibility = View.VISIBLE
                        val encryptedPassphrase = CBProApi.credentials?.apiPassPhrase ?: prefs.cbProPassphrase
                        val iv = ByteArray(16)
                        val encryption = Encryption.getDefault(apiKey, apiSecret + Constants.salt, iv)
                        apiPassphrase = encryption.decryptOrNull(encryptedPassphrase)
                    }
                    Exchange.Binance -> {
                        apiKey = BinanceApi.credentials?.apiKey ?: prefs.binanceApiKey
                        apiSecret = BinanceApi.credentials?.apiSecret ?: prefs.binanceApiSecret
                        viewHolder.passphraseEditText?.visibility = View.GONE
                    }
                }
                //show editable login info
                viewHolder.loginEditLayout?.visibility = View.VISIBLE
                viewHolder.apiKeyLayout?.visibility = View.GONE

                viewHolder.loginButton?.visibility = View.VISIBLE
                viewHolder.loginButton?.text = "Save Log In Info"

                if(apiKey != null) {
                    viewHolder.apiKeyEditText?.setText(apiKey)
                }
                if (apiSecret != null) {
                    viewHolder.apiSecretEditText?.setText(apiSecret)
                }

                if (apiPassphrase != null) {
                    viewHolder.passphraseEditText?.setText(apiPassphrase)
                }

                viewHolder.loginButton?.setOnClickListener {
                    saveLoginInfo(viewHolder, exchange)
                }
            }

            return outputView
        }
    }

//    private fun genericLogIn() {
//        (activity as? MainActivity)?.goToFragment(tradeFragment!!, FragmentType.TRADE.toString())
//
//    }

    private fun genericLogOut() {
        val prefs = Prefs(context)
        prefs.isLoggedIn = false
        for (product in Product.map.values) {
            product.accounts = mapOf()
        }
        prefs.stashedProducts = Product.map.values.toList()
        prefs.nukeStashedOrders()
        prefs.nukeStashedFills()
    }

    private fun saveLoginInfo(viewHolder: ViewHolder, exchange: Exchange) {
        val prefs = Prefs(context)

        var apiKey = ""
        var apiSecret = ""
        var passphrase = ""

        val loginStars = resources.getString(R.string.login_stars)
        if (viewHolder.apiKeyEditText?.text.toString() != loginStars) {
            apiKey = viewHolder.apiKeyEditText?.text.toString().trimEnd()
        }
        if (viewHolder.apiSecretEditText?.text.toString() != loginStars) {
            apiSecret = viewHolder.apiSecretEditText?.text.toString().trimEnd()
        }
        if (viewHolder.passphraseEditText?.text.toString() != loginStars) {
            passphrase = viewHolder.passphraseEditText?.text.toString()
        }

        val didChangeLoginInfo = when (exchange) {
            Exchange.CBPro -> {
                apiKey != CBProApi.credentials?.apiKey || apiSecret != CBProApi.credentials?.apiSecret || passphrase != CBProApi.credentials?.apiPassPhrase
            }
            Exchange.Binance -> {
                apiKey != BinanceApi.credentials?.apiKey || apiSecret != BinanceApi.credentials?.apiSecret
            }
        }


        if (!didChangeLoginInfo) {
            viewHolder.shouldShowEditLayout = false
            notifyDataSetChanged()
        } else {
            if (prefs.shouldSaveApiInfo) {
                val iv = ByteArray(16)
                val encryption = Encryption.getDefault(apiKey, apiSecret + Constants.salt, iv)
                val passphraseEncrypted = encryption.encryptOrNull(passphrase)
                prefs.cbProApiKey = apiKey
                prefs.cbProApiSecret = apiSecret
                if (prefs.shouldSavePassphrase) {
                    prefs.cbProPassphrase = passphraseEncrypted
                }
            }

            when {
                apiKey.isBlank() -> context.toast(R.string.login_error_missing_api_key)
                apiSecret.isBlank() -> context.toast(R.string.login_error_missing_api_secret)
                passphrase.isBlank() -> context.toast(R.string.login_error_missing_passphrase)
                else -> {
                    when (exchange) {
                        Exchange.CBPro -> {

                            if (prefs.shouldSaveApiInfo) {
                                val iv = ByteArray(16)
                                val encryption = Encryption.getDefault(apiKey, apiSecret + Constants.salt, iv)
                                val passphraseEncrypted = encryption.encryptOrNull(passphrase)
                                prefs.cbProApiKey = apiKey
                                prefs.cbProApiSecret = apiSecret
                                if (prefs.shouldSavePassphrase) {
                                    prefs.cbProPassphrase = passphraseEncrypted
                                }
                            }
                            val isApiKeyValid = prefs.isApiKeyValid(apiKey)
                            CBProApi.credentials = CBProApi.ApiCredentials(apiKey, apiSecret, passphrase, isApiKeyValid)
                            refreshAllProductsAndAccounts()
                        }
                        Exchange.Binance -> {
                            if (prefs.shouldSaveApiInfo) {
                                prefs.binanceApiKey = apiKey
                                prefs.binanceApiSecret = apiSecret
                            }
                            BinanceApi.credentials = BinanceApi.ApiCredentials(apiKey, apiSecret)
                            refreshAllProductsAndAccounts()
                        }
                    }
                    viewHolder.shouldShowEditLayout = false
                    notifyDataSetChanged()
                }
            }
        }
    }

    /*
    fun refreshAllProductsAndAccounts() {
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

    */
}