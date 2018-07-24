package com.anyexchange.anyx.fragments.main

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.adapters.RelatedAccountSpinnerAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.fragment_transfer_in.view.*
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.textColor

/**
 * Created by anyexchange on 11/5/2017.
 */
class TransferInFragment : RefreshFragment() {

    private lateinit var inflater: LayoutInflater

    private lateinit var titleText: TextView

    private lateinit var depositDetailsLayout: LinearLayout

    private lateinit var interactiveLayout: LinearLayout

    private lateinit var currencyTabLayout: TabLayout

    private lateinit var cbAccountsLabelTxt: TextView
    private lateinit var cbAccountsSpinner: Spinner
    private lateinit var cbAccountText: TextView

    private lateinit var depositMaxButton: Button

    private lateinit var amountLabelText: TextView
    private lateinit var amountEditText: EditText
    private lateinit var amountUnitText: TextView

    private lateinit var infoText: TextView
    private lateinit var cbproBalanceText: TextView

    private lateinit var submitDepositButton: Button

    private var coinbaseAccounts: List<Account.CoinbaseAccount> = listOf()
    private var paymentMethods: List<Account.PaymentMethod> = listOf()

    private var relevantAccounts: MutableList<Account.RelatedAccount> = mutableListOf()

    private var currency: Currency = Currency.USD
    private var sourceAccount: Account.RelatedAccount? = null

    companion object {
        fun newInstance(): TransferInFragment {
            return TransferInFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_transfer_in, container, false)
        setupSwipeRefresh(rootView.swipe_refresh_layout)

        this.inflater = inflater
        val activity = activity!!
        titleText = rootView.txt_transfer_in_title

        depositDetailsLayout = rootView.layout_transfer_in_details
        interactiveLayout = rootView.layout_transfer_in_interactive_layout

        currencyTabLayout = rootView.tabl_transfer_in_currency

        amountLabelText = rootView.txt_transfer_in_amount_label
        amountEditText = rootView.etxt_transfer_in_amount
        amountUnitText = rootView.txt_transfer_in_amount_unit

        depositMaxButton = rootView.btn_transfer_in_max

        cbAccountsLabelTxt = rootView.txt_transfer_in_account_label
        cbAccountsSpinner = rootView.spinner_transfer_in_accounts
        cbAccountText = rootView.txt_transfer_in_account_info

        infoText = rootView.txt_transfer_in_info
        cbproBalanceText = rootView.txt_transfer_in_cbpro_account_info

        submitDepositButton = rootView.btn_transfer_in_transfer_in

        val arrayAdapter = RelatedAccountSpinnerAdapter(activity, R.layout.list_row_coinbase_account, relevantAccounts)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cbAccountsSpinner.adapter = arrayAdapter

        currencyTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when(tab.position) {
                    //TODO: update for variable Fiat
                    0 -> switchCurrency(Currency.USD)
                    1 -> switchCurrency(Currency.BTC)
                    2 -> switchCurrency(Currency.ETH)
                    3 -> switchCurrency(Currency.BCH)
                    4 -> switchCurrency(Currency.LTC)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })


        cbAccountsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (relevantAccounts.size > position) {
                    sourceAccount = relevantAccounts[position]
                    if (sourceAccount is Account.CoinbaseAccount) {
                        infoText.setText(R.string.transfer_coinbase_info)
                    } else {
                        infoText.setText(R.string.transfer_bank_info)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
//                cbAccountsSpinner.visibility = View.GONE
            }
        }

        depositMaxButton.setOnClickListener {
            sourceAccount?.balance?.let { balance ->
                amountEditText.setText(balance.btcFormatShortened())
            }
        }

        submitDepositButton.setOnClickListener {
            val amountString = amountEditText.text.toString()
            val amount = amountString.toDoubleOrZero()

            if (amount <= 0) {
                showPopup(R.string.transfer_amount_error)
            } else if (sourceAccount is Account.CoinbaseAccount) {
                val coinbaseAccount = sourceAccount as Account.CoinbaseAccount
                if (amount > coinbaseAccount.balance) {
                    showPopup(R.string.transfer_funds_error)
                } else {
                    (activity as com.anyexchange.anyx.activities.MainActivity).showProgressBar()
                    CBProApi.getFromCoinbase(amount, currency, coinbaseAccount.id).executePost( { result ->
                        val errorMessage = CBProApi.ErrorMessage.forString(result.errorMessage)
                        if (amount > 0 && errorMessage == CBProApi.ErrorMessage.TransferAmountTooLow) {
                            showPopup(R.string.transfer_amount_low_error)
                        } else {
                            showPopup(resources.getString(R.string.error_generic_message, result.errorMessage))
                        }
                        activity.dismissProgressBar()
                    } , {
                        toast(R.string.transfer_received_message)
                        amountEditText.setText("")

                        refresh { activity.dismissProgressBar() }
                    })
                }
            } else if (sourceAccount is Account.PaymentMethod) {
                val paymentMethod = sourceAccount as Account.PaymentMethod
                if (paymentMethod.balance != null && amount > paymentMethod.balance) {
                    showPopup(R.string.transfer_funds_error)
                } else {
                    (activity as com.anyexchange.anyx.activities.MainActivity).showProgressBar()
                    CBProApi.getFromPayment(amount, currency, paymentMethod.id).executePost( { result ->
                        showPopup(resources.getString(R.string.error_generic_message, result.errorMessage))
                        activity.dismissProgressBar()
                    } , {
                        toast(R.string.transfer_received_message)
                        amountEditText.setText("")

                        refresh { activity.dismissProgressBar() }
                    })
                }
            } else {
                showPopup(R.string.error_message)
            }
        }

        return rootView
    }

    override fun onResume() {
        super.onResume()

        titleText.text = getString(R.string.transfer_in_title)

        coinbaseAccounts = Account.list.mapNotNull { account -> account.coinbaseAccount }
        coinbaseAccounts = coinbaseAccounts.filter { account -> account.balance > 0 }

        val fiatCoinbaseAccount = Account.fiatAccount?.coinbaseAccount
        if (fiatCoinbaseAccount != null) {
            coinbaseAccounts = coinbaseAccounts.plus(fiatCoinbaseAccount)
        }

        amountUnitText.text = currency.toString()

        relevantAccounts = coinbaseAccounts.filter { account -> account.currency == currency && account.balance > 0 }.toMutableList()
        if (currency.isFiat) {
            relevantAccounts.addAll(paymentMethods)
        }

        switchCurrency(currency)

        doneLoading()
    }

    private var isRefreshing = false
    override fun refresh(onComplete: (Boolean) -> Unit) {
        if (!isRefreshing) {
            isRefreshing = true
            var didUpdateCBPro = false
            var didUpdateCoinbase = false
            var didUpdatePaymentMethods = false
            CBProApi.accounts().updateAllAccounts({
                toast(R.string.toast_coinbase_pro_site_error)
                isRefreshing = false
                onComplete(false)
            }) {
                didUpdateCBPro = true
                if (didUpdateCoinbase && didUpdatePaymentMethods) {
                    completeRefresh(onComplete)
                    isRefreshing = false
                }
            }
            CBProApi.coinbaseAccounts().linkToAccounts({
                toast(R.string.toast_coinbase_site_error)
                isRefreshing = false
                onComplete(false)
            }, {
                coinbaseAccounts = Account.list.mapNotNull { account -> account.coinbaseAccount }
                val fiatCoinbaseAccount = Account.fiatAccount?.coinbaseAccount
                if (fiatCoinbaseAccount != null) {
                    coinbaseAccounts = coinbaseAccounts.plus(fiatCoinbaseAccount)
                }
                coinbaseAccounts = coinbaseAccounts.filter { account -> account.balance > 0 }
                didUpdateCoinbase = true
                if (didUpdateCBPro && didUpdatePaymentMethods) {
                    completeRefresh(onComplete)
                    isRefreshing = false
                }
            })
            CBProApi.paymentMethods().get({
                paymentMethods = listOf()
                onComplete(false)
            }, { result ->
                paymentMethods = result
                didUpdatePaymentMethods = true
                if (didUpdateCBPro && didUpdateCoinbase) {
                    completeRefresh(onComplete)
                    isRefreshing = false
                }
            })
        }
    }

    private fun completeRefresh(onComplete: (Boolean) -> Unit) {
        if (isVisible) {
            depositDetailsLayout.visibility = View.VISIBLE
            amountUnitText.text = currency.toString()

            switchCurrency(currency)
        }
        onComplete(true)
    }

    private fun switchCurrency(currency: Currency) {
        this.currency = currency
        amountEditText.setText("")
        relevantAccounts = coinbaseAccounts.filter { account -> account.currency == currency && account.balance > 0 }.toMutableList()
        if (currency.isFiat) {
            val relevantPaymentMethods = paymentMethods.filter { pm -> pm.apiPaymentMethod.allow_withdraw && pm.apiPaymentMethod.currency == currency.toString() }
            relevantAccounts.addAll(relevantPaymentMethods)
        }

        when (relevantAccounts.size) {
            0 -> {
                sourceAccount = null
                cbAccountText.text = resources.getText(R.string.transfer_coinbase_account_empty, currency.toString())
                cbAccountText.visibility = View.VISIBLE
                cbAccountsSpinner.visibility = View.GONE
                interactiveLayout.visibility = View.INVISIBLE
            }
            1 -> {
                sourceAccount = relevantAccounts.first()
                cbAccountText.text = sourceAccount.toString()
                cbAccountText.visibility = View.VISIBLE
                cbAccountsSpinner.visibility = View.GONE
                interactiveLayout.visibility = View.VISIBLE
            }
            else -> {
                sourceAccount = relevantAccounts.first()

                val arrayAdapter = RelatedAccountSpinnerAdapter(activity!!, R.layout.list_row_coinbase_account, relevantAccounts)
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                cbAccountsSpinner.adapter = arrayAdapter

                cbAccountText.visibility = View.GONE
                cbAccountsSpinner.visibility = View.VISIBLE
                interactiveLayout.visibility = View.VISIBLE
            }
        }
        if (sourceAccount is Account.CoinbaseAccount) {
            infoText.setText(R.string.transfer_coinbase_info)
        } else {
            infoText.setText(R.string.transfer_bank_info)
        }
        activity?.let {activity ->
            amountUnitText.text = currency.toString()

            val buttonColors = currency.colorStateList(activity)
            val buttonTextColor = currency.buttonTextColor(activity)

            depositMaxButton.backgroundTintList = buttonColors
            submitDepositButton.backgroundTintList = buttonColors

            depositMaxButton.textColor = buttonTextColor
            submitDepositButton.textColor = buttonTextColor

            val tabAccentColor = currency.colorAccent(activity)
            currencyTabLayout.setSelectedTabIndicatorColor(tabAccentColor)

            updateCBProAccountText()
        }
    }

    private fun updateCBProAccountText() {
        val cbproAccount = Account.forCurrency(currency)
        amountUnitText.text = currency.toString()

        val cbproAccountBalanceString = if (currency.isFiat) {
            "${(cbproAccount?.balance ?: 0.0).fiatFormat()} $currency"
        } else {
            "${(cbproAccount?.balance ?: 0.0).btcFormatShortened()} $currency"
        }
        cbproBalanceText.text = resources.getString(R.string.transfer_account_balance_text, currency.toString(), cbproAccountBalanceString)
    }
}
