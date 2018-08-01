package com.anyexchange.anyx.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.adapters.RelatedAccountSpinnerAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.textColor

/**
 * Created by anyexchange on 11/5/2017.
 */
class SweepCoinbaseFragment : RefreshFragment() {

    private lateinit var inflater: LayoutInflater

    private lateinit var titleText: TextView

    private lateinit var depositDetailsLayout: LinearLayout

    private lateinit var accountsLabelTxt: TextView
    private lateinit var accountsSpinner: Spinner

    private lateinit var depositMaxButton: Button

    private lateinit var amountLabelText: TextView
    private lateinit var amountEditText: EditText
    private lateinit var amountUnitText: TextView

    private lateinit var infoText: TextView
    private lateinit var cbproBalanceText: TextView

    private lateinit var submitDepositButton: Button

    private var coinbaseAccounts: List<Account.CoinbaseAccount> = listOf()
    private var coinbaseAccount: Account.CoinbaseAccount? = null

    companion object {
        fun newInstance(): SweepCoinbaseFragment {
            return SweepCoinbaseFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_transfer_in, container, false)
//        setupSwipeRefresh(rootView)

        this.inflater = inflater
        val activity = activity!!

//        val buttonColors = account.currency.colorStateList(activity)
//        submitDepositButton.backgroundTintList = buttonColors
//        val buttonTextColor = account.currency.buttonTextColor(activity)
//        submitDepositButton.textColor = buttonTextColor

        //titleText.text = "Buy and Sell " + account.currency.toString()

        titleText.text =  getString(R.string.transfer_in_title)

        coinbaseAccounts = Account.cryptoAccounts.mapNotNull { account -> account.coinbaseAccount }
        val fiatCoinbaseAccount = Account.defaultFiatAccount?.coinbaseAccount
        if (fiatCoinbaseAccount != null) {
            coinbaseAccounts = coinbaseAccounts.plus(fiatCoinbaseAccount)
        }
        coinbaseAccounts = coinbaseAccounts.filter { account -> account.balance > 0 }

        if (coinbaseAccounts.isEmpty()) {
            depositDetailsLayout.visibility = View.GONE
            titleText.text =  getString(R.string.transfer_cb_accounts_empty)

        } else {
            depositDetailsLayout.visibility = View.VISIBLE
            titleText.text = getString(R.string.transfer_in_title)

            coinbaseAccount = coinbaseAccounts.first()
            val currency = coinbaseAccount?.currency
            if (currency != null) {
                amountUnitText.text = currency.toString()
            }
        }
        updateCBProAccountText()

        val arrayAdapter = RelatedAccountSpinnerAdapter(activity, R.layout.list_row_coinbase_account, coinbaseAccounts)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        accountsSpinner.adapter = arrayAdapter
        dismissProgressSpinner()


        accountsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                coinbaseAccount = coinbaseAccounts[position]
                val currency = coinbaseAccount?.currency
                if (currency != null) {
                    amountUnitText.text = currency.toString()

                    val buttonColors = currency.colorStateList(activity)
                    val buttonTextColor = currency.buttonTextColor(activity)

                    depositMaxButton.backgroundTintList = buttonColors
                    submitDepositButton.backgroundTintList = buttonColors

                    depositMaxButton.textColor = buttonTextColor
                    submitDepositButton.textColor = buttonTextColor
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
//                accountsSpinner.visibility = View.GONE
            }
        }

        depositMaxButton.setOnClickListener {
            val amount = coinbaseAccount?.balance ?: 0.0
            amountEditText.setText(amount.btcFormatShortened())
        }

        submitDepositButton.setOnClickListener {
            val amountString = amountEditText.text.toString()
            val amount = amountString.toDoubleOrZero()

            if (amount <= 0) {
                showPopup("Amount is not valid", { })
            } else if (accountsSpinner.selectedItem is Account.CoinbaseAccount) {
                val coinbaseAccount = accountsSpinner.selectedItem as Account.CoinbaseAccount
                val currency = coinbaseAccount.currency

                    if (amount > coinbaseAccount.balance) {
                        showPopup("Not enough funds", { })
                    } else {
                        showProgressSpinner()
                        CBProApi.getFromCoinbase(amount, currency, coinbaseAccount.id).executePost( { result ->
                            showPopup("Transfer failed\n Error: ${result.error.message}", { })
                            dismissProgressSpinner()
                        } , {
                            toast("Transfer received")
                            amountEditText.setText("")

                            refresh { dismissProgressSpinner() }
                        })
                    }
                } else {
                    showPopup("Coinbase account could not be accessed", { })
                }
        }

        return rootView
    }

    private var isRefreshing = false
    override fun refresh(onComplete: (Boolean) -> Unit) {
        if (!isRefreshing) {
            isRefreshing = true
            var didUpdateCBPro = false
            var didUpdateCoinbase = false
            CBProApi.accounts().updateAllAccounts({
                onComplete(false)
                toast("Cannot access Coinbase Pro")
                isRefreshing = false
            }) {
                didUpdateCBPro = true
                if (didUpdateCoinbase) {
                    completeRefresh(onComplete)
                    isRefreshing = false
                }
            }
            CBProApi.coinbaseAccounts().linkToAccounts({
                toast("Cannot access Coinbase")
                isRefreshing = false
            }, {
                didUpdateCoinbase = true
                if (didUpdateCBPro) {
                    completeRefresh(onComplete)
                    isRefreshing = false
                }
            })
        }
    }

    private fun completeRefresh(onComplete: (Boolean) -> Unit) {
        coinbaseAccounts = Account.cryptoAccounts.mapNotNull { account -> account.coinbaseAccount }
        val fiatCoinbaseAccount = Account.defaultFiatAccount?.coinbaseAccount
        if (fiatCoinbaseAccount != null) {
            coinbaseAccounts = coinbaseAccounts.plus(fiatCoinbaseAccount)
        }
        coinbaseAccounts = coinbaseAccounts.filter { account -> account.balance > 0 }

        coinbaseAccount = if (coinbaseAccount != null) {
            coinbaseAccounts.find { account -> account.currency == coinbaseAccount?.currency }
        } else {
            coinbaseAccounts.firstOrNull()
        }

        if (coinbaseAccounts.isEmpty()) {
            depositDetailsLayout.visibility = View.GONE
            titleText.text = getString(R.string.transfer_cb_accounts_empty)
        } else {
            depositDetailsLayout.visibility = View.VISIBLE
            titleText.text = getString(R.string.transfer_in_title)

            (accountsSpinner.adapter as RelatedAccountSpinnerAdapter).relatedAccountList = coinbaseAccounts
            (accountsSpinner.adapter as RelatedAccountSpinnerAdapter).notifyDataSetChanged()


            val coinbaseAccount = coinbaseAccount
            if (coinbaseAccount != null) {
                val currency = coinbaseAccount.currency
                amountUnitText.text = currency.toString()
                updateCBProAccountText()
            }
        }
        onComplete(true)
    }

    private fun updateCBProAccountText() {
        val coinbaseAccount = coinbaseAccount
        if (coinbaseAccount != null) {
            val currency = coinbaseAccount.currency
            val cbproAccount = Account.forCurrency(currency)
            amountUnitText.text = currency.toString()
            if (currency.isFiat) {
                val cbproAccountBalance = (cbproAccount?.balance ?: 0.0).fiatFormat(Account.fiatCurrency)
                cbproBalanceText.text = "Coinbase Pro $currency Balance: $cbproAccountBalance"
            } else {
                val cbproAccountBalance = (cbproAccount?.balance ?: 0.0).btcFormatShortened()
                cbproBalanceText.text = "Coinbase Pro $currency Balance: $cbproAccountBalance $currency"
            }
        }
    }
}
