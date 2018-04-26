package com.anyexchange.anyx.Fragments.Main

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.Adapters.CoinbaseAccountSpinnerAdapter
import com.anyexchange.anyx.Classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.fragment_transfer_in.view.*
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.textColor

/**
 * Created by anyexchange on 11/5/2017.
 */
class TransferInCoinbaseFragment : RefreshFragment() {

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
    private lateinit var gdaxBalanceText: TextView

    private lateinit var submitDepositButton: Button

    private var coinbaseAccounts: List<Account.CoinbaseAccount> = listOf()
    private var currency: Currency = Currency.USD
    private var coinbaseAccount: Account.CoinbaseAccount? = null

    companion object {
        fun newInstance(): TransferInCoinbaseFragment {
            return TransferInCoinbaseFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_transfer_in, container, false)
        setupSwipeRefresh(rootView)

        this.inflater = inflater
        val activity = activity!!
        titleText = rootView.txt_transfer_in_coinbase_title

        depositDetailsLayout = rootView.layout_transfer_in_coinbase_details
        interactiveLayout = rootView.layout_transfer_in_interactive_layout

        currencyTabLayout = rootView.tabl_transfer_in_currency

        amountLabelText = rootView.txt_transfer_in_coinbase_amount_label
        amountEditText = rootView.etxt_transfer_in_coinbase_amount
        amountUnitText = rootView.txt_transfer_in_coinbase_amount_unit

        depositMaxButton = rootView.btn_transfer_in_coinbase_max

        cbAccountsLabelTxt = rootView.txt_transfer_in_coinbase_account_label
        cbAccountsSpinner = rootView.spinner_transfer_in_coinbase_accounts
        cbAccountText = rootView.txt_transfer_in_coinbase_account_info

        infoText = rootView.txt_transfer_in_coinbase_info
        gdaxBalanceText = rootView.txt_transfer_in_coinbase_gdax_account_info

        submitDepositButton = rootView.btn_transfer_in_coinbase_transfer_in

        currencyTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when(tab.position) {
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

        titleText.text = "Transfer from Coinbase"

        coinbaseAccounts = Account.list.mapNotNull { account -> account.coinbaseAccount }
        coinbaseAccounts = coinbaseAccounts.filter { account -> account.balance > 0 }

        val fiatCoinbaseAccount = Account.usdAccount?.coinbaseAccount
        if (fiatCoinbaseAccount != null) {
            coinbaseAccounts = coinbaseAccounts.plus(fiatCoinbaseAccount)
        }

        if (coinbaseAccounts.isEmpty()) {
            depositDetailsLayout.visibility = View.GONE
            titleText.text = "All Coinbase accounts are empty"

        } else {
            depositDetailsLayout.visibility = View.VISIBLE
            titleText.text = "Transfer from Coinbase"

            amountUnitText.text = currency.toString()
        }

        switchCurrency(currency)

        val arrayAdapter = CoinbaseAccountSpinnerAdapter(activity, R.layout.list_row_coinbase_account, coinbaseAccounts)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cbAccountsSpinner.adapter = arrayAdapter
        doneLoading()


//        cbAccountsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
//                coinbaseAccount = coinbaseAccounts[position]
//                val currency = coinbaseAccount?.currency
//                if (currency != null) {
//                    amountUnitText.text = currency.toString()
//
//                    val buttonColors = currency.colorStateList(activity)
//                    val buttonTextColor = currency.buttonTextColor(activity)
//
//                    depositMaxButton.backgroundTintList = buttonColors
//                    submitDepositButton.backgroundTintList = buttonColors
//
//                    depositMaxButton.textColor = buttonTextColor
//                    submitDepositButton.textColor = buttonTextColor
//                }
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>) {
////                cbAccountsSpinner.visibility = View.GONE
//            }
//        }

        depositMaxButton.setOnClickListener {
            val amount = coinbaseAccount?.balance ?: 0.0
            amountEditText.setText(amount.btcFormatShortened())
        }

        submitDepositButton.setOnClickListener {
            val amountString = amountEditText.text.toString()
            val amount = amountString.toDoubleOrZero()

            if (amount <= 0) {
                showPopup("Amount is not valid", { })
            } else if (coinbaseAccount != null) {
                val coinbaseAccount = coinbaseAccount!!
                if (amount > coinbaseAccount.balance) {
                    showPopup("Not enough funds", { })
                } else {
                    (activity as com.anyexchange.anyx.Activities.MainActivity).showProgressBar()
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

        depositDetailsLayout.visibility = View.VISIBLE
        titleText.text = "Transfer into GDAX"

        if (coinbaseAccount != null) {
            if (coinbaseAccount!!.balance > 0) {
                val currency = coinbaseAccount!!.currency
                amountUnitText.text = currency.toString()
            } else {
                this.coinbaseAccount = coinbaseAccounts.firstOrNull()
                cbAccountsSpinner.setSelection(0)
                val currency = coinbaseAccount?.currency
                amountUnitText.text = currency.toString()
            }
            updateGdaxAccountText()
        }
        switchCurrency(currency)
        onComplete()
    }

    private fun switchCurrency(currency: Currency) {
        this.currency = currency
        amountEditText.setText("")
        val relevantAccounts = coinbaseAccounts.filter { account -> account.currency == currency && account.balance > 0 }

        when (relevantAccounts.size) {
            0 -> {
                coinbaseAccount = null
                cbAccountText.text = "Coinbase $currency wallet is Empty"
                cbAccountText.visibility = View.VISIBLE
                cbAccountsSpinner.visibility = View.GONE
                interactiveLayout.visibility = View.INVISIBLE
            }
            1 -> {
                coinbaseAccount = relevantAccounts.first()
                cbAccountText.text = coinbaseAccount.toString()
                cbAccountText.visibility = View.VISIBLE
                cbAccountsSpinner.visibility = View.GONE
                interactiveLayout.visibility = View.VISIBLE
            }
            else -> {
                coinbaseAccount = relevantAccounts.first()

                (cbAccountsSpinner.adapter as? CoinbaseAccountSpinnerAdapter)?.coinbaseAccountList = relevantAccounts
                (cbAccountsSpinner.adapter as? CoinbaseAccountSpinnerAdapter)?.notifyDataSetChanged()

                cbAccountText.visibility = View.GONE
                cbAccountsSpinner.visibility = View.VISIBLE
                interactiveLayout.visibility = View.VISIBLE
            }
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

            updateGdaxAccountText()
        }
    }

    private fun updateGdaxAccountText() {
        val gdaxAccount = Account.forCurrency(currency)
        amountUnitText.text = currency.toString()

        val gdaxAccountBalanceString = if (currency.isFiat) {
            "${(gdaxAccount?.balance ?: 0.0).fiatFormat()} $currency"
        } else {
            "${(gdaxAccount?.balance ?: 0.0).btcFormatShortened()} $currency"
        }
        gdaxBalanceText.text = "GDAX $currency Balance: $gdaxAccountBalanceString"
    }
}
