package com.jmnbehar.anyx.Fragments.Main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.jmnbehar.anyx.Activities.MainActivity
import com.jmnbehar.anyx.Adapters.CoinbaseAccountSpinnerAdapter
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_depost_coinbase.view.*
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.textColor

/**
 * Created by jmnbehar on 11/5/2017.
 */
class TransferInCoinbaseFragment : RefreshFragment() {

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
    private lateinit var gdaxBalanceText: TextView

    private lateinit var submitDepositButton: Button

    private var coinbaseAccounts: List<Account.CoinbaseAccount> = listOf()
    private var coinbaseAccount: Account.CoinbaseAccount? = null

    companion object {
        fun newInstance(): TransferInCoinbaseFragment {
            return TransferInCoinbaseFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_depost_coinbase, container, false)
        setupSwipeRefresh(rootView)

        this.inflater = inflater
        val activity = activity!!
        titleText = rootView.txt_deposit_coinbase_title

        depositDetailsLayout = rootView.layout_deposit_coinbase_details

        amountLabelText = rootView.txt_deposit_coinbase_amount_label
        amountEditText = rootView.etxt_deposit_coinbase_amount
        amountUnitText = rootView.txt_deposit_coinbase_amount_unit

        depositMaxButton = rootView.btn_deposit_coinbase_max

        accountsLabelTxt = rootView.txt_deposit_coinbase_account_label
        accountsSpinner = rootView.spinner_deposit_coinbase_accounts

        infoText = rootView.txt_deposit_coinbase_info
        gdaxBalanceText = rootView.txt_deposit_coinbase_gdax_account_info

        submitDepositButton = rootView.btn_deposit_coinbase_deposit
//        val buttonColors = account.currency.colorStateList(activity)
//        submitDepositButton.backgroundTintList = buttonColors
//        val buttonTextColor = account.currency.buttonTextColor(activity)
//        submitDepositButton.textColor = buttonTextColor

        //titleText.text = "Buy and Sell " + account.currency.toString()

        titleText.text = "Transfer from Coinbase"

        coinbaseAccounts = Account.list.mapNotNull { account -> account.coinbaseAccount }
        val fiatCoinbaseAccount = Account.usdAccount?.coinbaseAccount
        if (fiatCoinbaseAccount != null) {
            coinbaseAccounts = coinbaseAccounts.plus(fiatCoinbaseAccount)
        }
        coinbaseAccounts = coinbaseAccounts.filter { account -> account.balance > 0 }

        if (coinbaseAccounts.isEmpty()) {
            depositDetailsLayout.visibility = View.GONE
            titleText.text = "All Coinbase accounts are empty"

        } else {
            depositDetailsLayout.visibility = View.VISIBLE
            titleText.text = "Transfer from Coinbase"

            coinbaseAccount = coinbaseAccounts.first()
            val currency = coinbaseAccount?.currency
            if (currency != null) {
                amountUnitText.text = currency.toString()
            }
        }
        updateGdaxAccountText()

        val arrayAdapter = CoinbaseAccountSpinnerAdapter(activity, R.layout.list_row_coinbase_account, coinbaseAccounts)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        accountsSpinner.adapter = arrayAdapter
        doneLoading()


        accountsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
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
                        (activity as MainActivity).showProgressBar()
                        GdaxApi.getFromCoinbase(amount, currency, coinbaseAccount.id).executePost( { result ->
                            val errorMessage = GdaxApi.ErrorMessage.forString(result.errorMessage)
                            if (amount > 0 && errorMessage == GdaxApi.ErrorMessage.TransferAmountTooLow) {
                                showPopup("Error: Amount too low", { })
                            } else {
                                showPopup("Error: " + result.errorMessage, { })
                            }
                            activity.dismissProgressBar()
                        } , {
                            toast("Transfer received")
                            amountEditText.setText("")

                            refresh { activity.dismissProgressBar() }
                        })
                    }
                } else {
                    showPopup("Coinbase account could not be accessed", { })
                }
        }

        return rootView
    }

    private var isRefreshing = false
    override fun refresh(onComplete: () -> Unit) {
        if (!isRefreshing) {
            isRefreshing = true
            var didUpdateGDAX = false
            var didUpdateCoinbase = false
            GdaxApi.accounts().updateAllAccounts({ onComplete()
                toast("Cannot access GDAX")
                isRefreshing = false
            }) {
                didUpdateGDAX = true
                if (didUpdateCoinbase) {
                    completeRefresh(onComplete)
                    isRefreshing = false
                }
            }
            GdaxApi.coinbaseAccounts().linkToAccounts({
                toast("Cannot access Coinbase")
                isRefreshing = false
            }, {
                didUpdateCoinbase = true
                if (didUpdateGDAX) {
                    completeRefresh(onComplete)
                    isRefreshing = false
                }
            })
        }
    }

    private fun completeRefresh(onComplete: () -> Unit) {
        coinbaseAccounts = Account.list.mapNotNull { account -> account.coinbaseAccount }
        val fiatCoinbaseAccount = Account.usdAccount?.coinbaseAccount
        if (fiatCoinbaseAccount != null) {
            coinbaseAccounts = coinbaseAccounts.plus(fiatCoinbaseAccount)
        }
        coinbaseAccounts = coinbaseAccounts.filter { account -> account.balance > 0 }

        if (coinbaseAccount != null) {
            coinbaseAccount = coinbaseAccounts.find { account -> account.currency == coinbaseAccount?.currency }
        } else {
            coinbaseAccount = coinbaseAccounts.firstOrNull()
        }

        if (coinbaseAccounts.isEmpty()) {
            depositDetailsLayout.visibility = View.GONE
            titleText.text = "All Coinbase accounts are empty"
        } else {
            depositDetailsLayout.visibility = View.VISIBLE
            titleText.text = "Transfer from Coinbase"

            (accountsSpinner.adapter as CoinbaseAccountSpinnerAdapter).coinbaseAccountList = coinbaseAccounts
            (accountsSpinner.adapter as CoinbaseAccountSpinnerAdapter).notifyDataSetChanged()

            if (coinbaseAccount != null) {
                if (coinbaseAccount!!.balance > 0) {
                    val currency = coinbaseAccount!!.currency
                    amountUnitText.text = currency.toString()
                    updateGdaxAccountText()
                } else {
                    this.coinbaseAccount = coinbaseAccounts.firstOrNull()
                    accountsSpinner.setSelection(0)
                    val currency = coinbaseAccount?.currency
                    amountUnitText.text = currency.toString()
                    updateGdaxAccountText()

                }
            }
        }
        onComplete()
    }

    private fun updateGdaxAccountText() {
        val coinbaseAccount = coinbaseAccount
        if (coinbaseAccount != null) {
            val currency = coinbaseAccount.currency
            val gdaxAccount = Account.forCurrency(currency)
            amountUnitText.text = currency.toString()
            if (currency.isFiat) {
                val gdaxAccountBalance = (gdaxAccount?.balance ?: 0.0).fiatFormat()
                gdaxBalanceText.text = "GDAX $currency Wallet ($gdaxAccountBalance)"
            } else {
                val gdaxAccountBalance = (gdaxAccount?.balance ?: 0.0).btcFormatShortened()
                gdaxBalanceText.text = "GDAX $currency Wallet ($gdaxAccountBalance $currency)"
            }
        }
    }
}
