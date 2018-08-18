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
import kotlinx.android.synthetic.main.fragment_transfer_out.view.*
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.textColor


/**
 * Created by anyexchange on 11/5/2017.
 */
class TransferOutFragment : RefreshFragment() {

    private lateinit var inflater: LayoutInflater

    private lateinit var titleText: TextView

    private lateinit var withdrawDetailsLayout: LinearLayout

    private lateinit var cbAccountsLabelTxt: TextView
    private lateinit var cbAccountsSpinner: Spinner
    private lateinit var cbAccountText: TextView

    private lateinit var withdrawMaxButton: Button

    private lateinit var amountLabelText: TextView
    private lateinit var amountEditText: EditText
    private lateinit var amountUnitText: TextView

    private lateinit var infoText: TextView

    private lateinit var cbproBalanceText: TextView

    private lateinit var submitWithdrawalButton: Button

    private lateinit var interactiveLayout: LinearLayout

    private lateinit var currencyTabLayout: TabLayout


    private var coinbaseAccounts: List<Account.CoinbaseAccount> = listOf()
    private var paymentMethods: List<Account.PaymentMethod> = listOf()
    private var relevantAccounts: MutableList<Account.RelatedAccount> = mutableListOf()

    private var destinationAccount: Account.RelatedAccount? = null
    private var currency: Currency = Account.defaultFiatCurrency

    companion object {
        fun newInstance(): TransferOutFragment {
            return TransferOutFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_transfer_out, container, false)
        setupSwipeRefresh(rootView.swipe_refresh_layout)

        this.inflater = inflater
        val activity = activity!!
        titleText = rootView.txt_transfer_out_title

        withdrawDetailsLayout = rootView.layout_transfer_out_details

        amountLabelText = rootView.txt_transfer_out_amount_label
        amountEditText = rootView.etxt_transfer_out_amount
        amountUnitText = rootView.txt_transfer_out_amount_unit

        withdrawMaxButton = rootView.btn_transfer_out_max

        cbAccountsLabelTxt = rootView.txt_transfer_out_account_label
        cbAccountsSpinner = rootView.spinner_transfer_out_accounts
        cbAccountText = rootView.txt_transfer_out_account_info
        cbAccountText = rootView.txt_transfer_out_account_info

        infoText = rootView.txt_transfer_out_info

        cbproBalanceText = rootView.txt_transfer_out_cbpro_account_info
        interactiveLayout = rootView.layout_transfer_out_amount_layout
        submitWithdrawalButton = rootView.btn_transfer_out_transfer_out

        //TODO: nuke this tab layout, use spinner
        currencyTabLayout = rootView.tabl_transfer_out_currency
        currencyTabLayout.setupAllCurrencyTabs { switchCurrency(it) }

        val arrayAdapter = RelatedAccountSpinnerAdapter(activity, R.layout.list_row_coinbase_account, relevantAccounts)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cbAccountsSpinner.adapter = arrayAdapter

        cbAccountsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (relevantAccounts.size > position) {
                    destinationAccount = relevantAccounts[position]
                    if (destinationAccount is Account.CoinbaseAccount) {
                        infoText.setText(R.string.transfer_coinbase_info)
                    } else {
                        infoText.setText(R.string.transfer_bank_info)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
//                accountsSpinner.visibility = View.GONE
            }
        }

        withdrawMaxButton.setOnClickListener {
            val cbproAccount = Account.forCurrency(currency)
            cbproAccount?.balance?.let { balance ->
                amountEditText.setText(balance.btcFormatShortened())
            }
        }

        submitWithdrawalButton.setOnClickListener {
            val amountString = amountEditText.text.toString()
            val amount = amountString.toDoubleOrZero()

            if (amount <= 0) {
                showPopup(R.string.transfer_amount_error)
            } else if (destinationAccount is Account.CoinbaseAccount) {
                val coinbaseAccount = destinationAccount as Account.CoinbaseAccount
                val cbproAccount = Account.forCurrency(currency)

                if (amount > cbproAccount?.availableBalance ?: 0.0) {
                    showPopup(R.string.transfer_funds_error)
                } else {
                    showProgressSpinner()
                    CBProApi.sendToCoinbase(apiInitData, amount, currency, coinbaseAccount.id).executePost({ result ->
                        showPopup(resources.getString(R.string.error_generic_message, result.errorMessage))
                        dismissProgressSpinner()
                    }, {
                        toast(R.string.transfer_sent_message)
                        amountEditText.setText("")

                        refresh { dismissProgressSpinner() }
                    })
                }
            } else if (destinationAccount is Account.PaymentMethod) {
                val paymentMethod = destinationAccount as Account.PaymentMethod
                if (paymentMethod.balance != null && amount > paymentMethod.balance) {
                    showPopup(R.string.transfer_funds_error)
                } else {
                    showProgressSpinner()
                    CBProApi.sendToPayment(apiInitData, amount, currency, paymentMethod.id).executePost( { result ->
                        showPopup(resources.getString(R.string.error_generic_message, result.errorMessage))
                        dismissProgressSpinner()
                    }, {
                        toast(R.string.transfer_sent_message)
                        amountEditText.setText("")
                        refresh { dismissProgressSpinner() }
                    })
                }
            } else {
                toast(R.string.error_message)
            }
        }

        return rootView
    }

    override fun onResume() {
        super.onResume()

        switchCurrency(currency)

        dismissProgressSpinner()
    }

    private var isRefreshing = false
    override fun refresh(onComplete: (Boolean) -> Unit) {
        if (!isRefreshing) {
            isRefreshing = true
            var didUpdateCBPro = false
            var didUpdateCoinbase = false
            var didUpdatePaymentMethods = false

            CBProApi.accounts(apiInitData).updateAllAccounts({ _ ->
                onComplete(false)
                toast(R.string.toast_coinbase_pro_site_error)
                isRefreshing = false
            }) {
                didUpdateCBPro = true
                if (didUpdateCoinbase && didUpdatePaymentMethods) {
                    completeRefresh(onComplete)
                    isRefreshing = false
                }
            }
            CBProApi.coinbaseAccounts(apiInitData).linkToAccounts({ _ ->
                toast(R.string.toast_coinbase_site_error)
                onComplete(false)
                isRefreshing = false
            }, {
                val cbproAccounts = Account.cryptoAccounts.toMutableList()
                //TODO: work on adding more fiat accounts in here
                val fiatAccount = Account.defaultFiatAccount
                if (fiatAccount != null) {
                    cbproAccounts.add(fiatAccount)
                }
                coinbaseAccounts = cbproAccounts.mapNotNull { account -> account.coinbaseAccount }.toMutableList()
                didUpdateCoinbase = true
                if (didUpdateCBPro && didUpdatePaymentMethods) {
                    completeRefresh(onComplete)
                    isRefreshing = false
                }
            })
            CBProApi.paymentMethods(apiInitData).get({
                toast(R.string.error_message)
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
            switchCurrency()
        }
        onComplete(true)
    }

    private fun switchCurrency(newCurrency: Currency? = null) {
        newCurrency?.let { currency ->
            this.currency = currency
        }
        amountEditText.setText("")

        relevantAccounts = coinbaseAccounts.filter { account -> account.currency == currency }.toMutableList()
        if (currency.isFiat) {
            val relevantPaymentMethods = paymentMethods.filter { pm -> pm.apiPaymentMethod.allow_deposit && pm.apiPaymentMethod.currency == currency.toString() }
            relevantAccounts.addAll(relevantPaymentMethods)
        }

        when (relevantAccounts.size) {
            0 -> {
                destinationAccount = null
                cbAccountText.visibility = View.VISIBLE
                cbAccountsSpinner.visibility = View.GONE
            }
            1 -> {
                destinationAccount = relevantAccounts.first()
                cbAccountText.text = destinationAccount.toString()
                cbAccountText.visibility = View.VISIBLE
                cbAccountsSpinner.visibility = View.GONE
            }
            else -> {
                destinationAccount = relevantAccounts.first()

                val arrayAdapter = RelatedAccountSpinnerAdapter(activity!!, R.layout.list_row_coinbase_account, relevantAccounts)
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                cbAccountsSpinner.adapter = arrayAdapter

                cbAccountText.visibility = View.GONE
                cbAccountsSpinner.visibility = View.VISIBLE
            }
        }
        if (destinationAccount is Account.CoinbaseAccount) {
            infoText.setText(R.string.transfer_coinbase_info)
        } else {
            infoText.setText(R.string.transfer_bank_info)
        }

        val cbproAccount = Account.forCurrency(currency)
        if ((cbproAccount?.availableBalance ?: 0.0) > 0.0) {
            interactiveLayout.visibility = View.VISIBLE
        } else {
            interactiveLayout.visibility = View.INVISIBLE
        }
        amountUnitText.text = currency.toString()
        if ((cbproAccount?.availableBalance ?: 0.0) > 0) {
            val cbproAccountBalance = if (currency.isFiat) {
                (cbproAccount?.availableBalance ?: 0.0).fiatFormat(Account.defaultFiatCurrency)
            } else {
                (cbproAccount?.availableBalance ?: 0.0).btcFormatShortened()
            }
            cbproBalanceText.text = resources.getString(R.string.transfer_pro_account_balance_text, currency.toString(), cbproAccountBalance)
        } else {
            cbproBalanceText.text = resources.getString(R.string.transfer_pro_account_empty)
        }

        activity?.let {activity ->
            amountUnitText.text = currency.toString()

            val buttonColors = currency.colorStateList(activity)
            val buttonTextColor = currency.buttonTextColor(activity)

            withdrawMaxButton.backgroundTintList = buttonColors
            submitWithdrawalButton.backgroundTintList = buttonColors

            withdrawMaxButton.textColor = buttonTextColor
            submitWithdrawalButton.textColor = buttonTextColor

            val tabAccentColor = currency.colorAccent(activity)
            currencyTabLayout.setSelectedTabIndicatorColor(tabAccentColor)
        }
    }
}
