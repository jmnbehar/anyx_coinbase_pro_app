package com.jmnbehar.anyx.Fragments.Main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.jmnbehar.anyx.Activities.MainActivity
import com.jmnbehar.anyx.Adapters.AccountListViewAdapter
import com.jmnbehar.anyx.Adapters.CoinbaseAccountListAdapter
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_depost_coinbase.view.*
import kotlinx.android.synthetic.main.fragment_withdraw_coinbase.view.*
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast

/**
 * Created by jmnbehar on 11/5/2017.
 */
class WithdrawCoinbaseFragment : RefreshFragment() {

    private lateinit var inflater: LayoutInflater

    private lateinit var titleText: TextView

    private lateinit var withdrawDetailsLayout: LinearLayout

    private lateinit var accountsLabelTxt: TextView
    private lateinit var accountsSpinner: Spinner

    private lateinit var withdrawMaxButton: Button

    private lateinit var amountLabelText: TextView
    private lateinit var amountEditText: EditText
    private lateinit var amountUnitText: TextView

    private lateinit var infoText: TextView

    private lateinit var gdaxBalanceText: TextView

    private lateinit var submitWithdrawalButton: Button

    private var coinbaseAccounts: List<Account.CoinbaseAccount> = listOf()
    private var coinbaseAccount: Account.CoinbaseAccount? = null

    companion object {
        fun newInstance(): WithdrawCoinbaseFragment {
            return WithdrawCoinbaseFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_withdraw_coinbase, container, false)
        setupSwipeRefresh(rootView)

        this.inflater = inflater
        val activity = activity!!
        titleText = rootView.txt_withdraw_coinbase_title

        withdrawDetailsLayout = rootView.layout_withdraw_coinbase_details

        amountLabelText = rootView.txt_withdraw_coinbase_amount_label
        amountEditText = rootView.etxt_withdraw_coinbase_amount
        amountUnitText = rootView.txt_withdraw_coinbase_amount_unit

        withdrawMaxButton = rootView.btn_withdraw_coinbase_max

        accountsLabelTxt = rootView.txt_withdraw_coinbase_account_label
        accountsSpinner = rootView.spinner_withdraw_coinbase_accounts

        infoText = rootView.txt_withdraw_coinbase_info

        gdaxBalanceText = rootView.txt_withdraw_coinbase_gdax_account_info

        submitWithdrawalButton = rootView.btn_withdraw_coinbase_withdraw
//        val buttonColors = account.currency.colorStateList(activity)
//        submitWithdrawalButton.backgroundTintList = buttonColors
//        val buttonTextColor = account.currency.buttonTextColor(activity)
//        submitWithdrawalButton.textColor = buttonTextColor

        //titleText.text = "Buy and Sell " + account.currency.toString()

        coinbaseAccounts = Account.list.mapNotNull { account -> account.coinbaseAccount }
        coinbaseAccounts = coinbaseAccounts.filter { cbAccount -> (Account.forCurrency(cbAccount.currency)?.balance ?: 0.0) > 0 }
        val arrayAdapter = CoinbaseAccountListAdapter(activity, R.layout.list_row_coinbase_account, coinbaseAccounts)

        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        accountsSpinner.adapter = arrayAdapter

//        completeRefresh( { } )
        val nonEmptyAccount = Account.list.find { account -> account.balance > 0 }
        if (nonEmptyAccount == null) {
            withdrawDetailsLayout.visibility = View.GONE
            titleText.text = "All Coinbase accounts are empty"
        } else {
            withdrawDetailsLayout.visibility = View.VISIBLE
            titleText.text = "Withdraw to Coinbase"


            coinbaseAccount = coinbaseAccounts.first()
            val coinbaseAccount = coinbaseAccount
            if (coinbaseAccount != null) {
                val currency = coinbaseAccount.currency
                val gdaxAccount = Account.forCurrency(currency)
                val gdaxAccountBalance = (gdaxAccount?.balance ?: 0.0).btcFormatShortened()
                amountUnitText.text = currency.toString()
                gdaxBalanceText.text = "GDAX $currency Balance: $gdaxAccountBalance $currency"
            }
        }
        doneLoading()


        accountsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                coinbaseAccount = coinbaseAccounts[position]
                val currency = coinbaseAccount?.currency
                if (currency != null) {
                    amountUnitText.text = currency.toString()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
//                accountsSpinner.visibility = View.GONE
            }
        }

//        amountEditText.addTextChangedListener(object : TextWatcher {
//            override fun afterTextChanged(p0: Editable?) {
//                val amount = p0.toString().toDoubleOrZero()
//                updateTotalText(amount)
//            }
//            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//        })

        withdrawMaxButton.setOnClickListener {
            val coinbaseAccount = accountsSpinner.selectedItem as Account.CoinbaseAccount
            val currency = coinbaseAccount.currency
            val gdaxAccount = Account.forCurrency(currency)
            val amount = gdaxAccount?.balance
            if (amount != null) {
                amountEditText.setText(amount.btcFormatShortened())
            }
        }

        submitWithdrawalButton.setOnClickListener {
            val amountString = amountEditText.text.toString()
            val amount = amountString.toDoubleOrZero()

            if (amount <= 0) {
                showPopup("Amount is not valid", { })
            } else {
                if (accountsSpinner.selectedItem is Account.CoinbaseAccount) {
                    val coinbaseAccount = accountsSpinner.selectedItem as Account.CoinbaseAccount
                    val currency = coinbaseAccount.currency
                    val gdaxAccount = Account.forCurrency(currency)

                    if (amount > gdaxAccount?.balance ?: 0.0) {
                        showPopup("Not enough funds", { })
                    }
                    (activity as MainActivity).showProgressBar()
                    GdaxApi.getFromCoinbase(amount, currency, coinbaseAccount.id).executePost( { result ->
                        showPopup("Error" + result.error.message, { })
                        activity.dismissProgressBar()
                    } , {
                        toast("Received")
                        amountEditText.setText("")

                        refresh { activity.dismissProgressBar() }
                    })
                } else {
                    showPopup("Coinbase account could not be accessed", { })
                }
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
            Account.updateAllAccounts({ onComplete()
                toast("Cannot access GDAX")
                isRefreshing = false
            }) {
                didUpdateGDAX = true
                if (didUpdateCoinbase) {
                    completeRefresh(onComplete)
                    isRefreshing = false
                }
            }
            TransferHub.linkCoinbaseAccounts({
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
        val nonEmptyAccount = Account.list.find { account -> account.balance > 0 }
        if (nonEmptyAccount == null) {
            withdrawDetailsLayout.visibility = View.GONE
            titleText.text = "All Coinbase accounts are empty"
        } else {
            withdrawDetailsLayout.visibility = View.VISIBLE
            titleText.text = "Withdraw to Coinbase"

            coinbaseAccounts = Account.list.mapNotNull { account -> account.coinbaseAccount }
            coinbaseAccounts = coinbaseAccounts.filter { cbAccount -> (Account.forCurrency(cbAccount.currency)?.balance ?: 0.0) > 0 }

            (accountsSpinner.adapter as CoinbaseAccountListAdapter).coinbaseAccountList = coinbaseAccounts
            (accountsSpinner.adapter as CoinbaseAccountListAdapter).notifyDataSetChanged()

            coinbaseAccount = coinbaseAccounts.first()
            val coinbaseAccount = coinbaseAccount
            if (coinbaseAccount != null) {
                val currency = coinbaseAccount.currency
                val gdaxAccount = Account.forCurrency(currency)
                val gdaxAccountBalance = (gdaxAccount?.balance ?: 0.0).btcFormatShortened()
                amountUnitText.text = currency.toString()
                gdaxBalanceText.text = "GDAX $currency Balance: $gdaxAccountBalance $currency"
            }
        }
        onComplete()
    }
}
