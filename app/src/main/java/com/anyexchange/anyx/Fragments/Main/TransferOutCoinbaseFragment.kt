package com.anyexchange.anyx.Fragments.Main

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.Adapters.RelatedAccountSpinnerAdapter
import com.anyexchange.anyx.Classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.fragment_transfer_out.view.*
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.textColor


/**
 * Created by anyexchange on 11/5/2017.
 */
class TransferOutCoinbaseFragment : RefreshFragment() {

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

    private lateinit var gdaxBalanceText: TextView

    private lateinit var submitWithdrawalButton: Button

    private lateinit var interactiveLayout: LinearLayout

    private lateinit var currencyTabLayout: TabLayout

    private var coinbaseAccounts: MutableList<Account.CoinbaseAccount> = mutableListOf()
    private var destinationAccount: Account.CoinbaseAccount? = null
    private var currency: Currency = Currency.USD

    companion object {
        fun newInstance(): TransferOutCoinbaseFragment {
            return TransferOutCoinbaseFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_transfer_out, container, false)
        setupSwipeRefresh(rootView)

        this.inflater = inflater
        val activity = activity!!
        titleText = rootView.txt_transfer_out_coinbase_title

        withdrawDetailsLayout = rootView.layout_transfer_out_coinbase_details

        amountLabelText = rootView.txt_transfer_out_coinbase_amount_label
        amountEditText = rootView.etxt_transfer_out_coinbase_amount
        amountUnitText = rootView.txt_transfer_out_coinbase_amount_unit

        withdrawMaxButton = rootView.btn_transfer_out_coinbase_max

        cbAccountsLabelTxt = rootView.txt_transfer_out_coinbase_account_label
        cbAccountsSpinner = rootView.spinner_transfer_out_coinbase_accounts
        cbAccountText = rootView.txt_transfer_out_coinbase_account_info

        infoText = rootView.txt_transfer_out_coinbase_info

        gdaxBalanceText = rootView.txt_transfer_out_coinbase_gdax_account_info
        interactiveLayout = rootView.layout_transfer_out_coinbase_amount_layout
        submitWithdrawalButton = rootView.btn_transfer_out_coinbase_transfer_out

        currencyTabLayout = rootView.tabl_transfer_out_currency
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

        val arrayAdapter = RelatedAccountSpinnerAdapter(activity, R.layout.list_row_coinbase_account, coinbaseAccounts)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cbAccountsSpinner.adapter = arrayAdapter

        completeRefresh {  }

        doneLoading()


//        accountsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
//                destinationAccount = coinbaseAccounts[position]
//                val currency = destinationAccount?.currency
//                if (currency != null) {
//                    amountUnitText.text = currency.toString()
//
//                    val buttonColors = currency.colorStateList(activity)
//                    val buttonTextColor = currency.buttonTextColor(activity)
//
//                    withdrawMaxButton.backgroundTintList = buttonColors
//                    submitWithdrawalButton.backgroundTintList = buttonColors
//
//                    withdrawMaxButton.textColor = buttonTextColor
//                    submitWithdrawalButton.textColor = buttonTextColor
//
//                    updateGdaxAccountText()
//                }
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>) {
////                accountsSpinner.visibility = View.GONE
//            }
//        }

        //TODO: think about holds, adjust max accordingly
        withdrawMaxButton.setOnClickListener {
            val gdaxAccount = Account.forCurrency(currency)
            val amount = gdaxAccount?.availableBalance
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
                val gdaxAccount = Account.forCurrency(currency)

                if (amount > gdaxAccount?.availableBalance ?: 0.0) {
                    showPopup("Not enough funds", { })
                }
                (activity as com.anyexchange.anyx.Activities.MainActivity).showProgressBar()
                destinationAccount?.let { cbAccount ->
                    GdaxApi.sendToCoinbase(amount, currency, cbAccount.id).executePost({ result ->
                        val errorMessage = GdaxApi.ErrorMessage.forString(result.errorMessage)
                        if (amount > 0 && errorMessage == GdaxApi.ErrorMessage.TransferAmountTooLow) {
                            showPopup("Error: Amount too low", { })
                        } else {
                            showPopup("Error: " + result.errorMessage, { })
                        }
                        activity.dismissProgressBar()
                    }, {
                        toast("Transfer Sent")
                        amountEditText.setText("")

                        refresh { activity.dismissProgressBar() }
                    })
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
            GdaxApi.accounts().updateAllAccounts({ result ->
                onComplete()
                toast("Cannot access GDAX")
                isRefreshing = false
            }) {
                didUpdateGDAX = true
                if (didUpdateCoinbase) {
                    completeRefresh(onComplete)
                    isRefreshing = false
                }
            }
            GdaxApi.coinbaseAccounts().linkToAccounts({ result ->
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
        val gdaxAccounts = Account.list.toMutableList()
        val fiatAccount = Account.usdAccount
        if (fiatAccount != null) {
            gdaxAccounts.add(fiatAccount)
        }
        coinbaseAccounts = gdaxAccounts.mapNotNull { account -> account.coinbaseAccount }.toMutableList()
        switchCurrency()
        onComplete()
    }

    private fun switchCurrency(newCurrency: Currency? = null) {
        newCurrency?.let { currency ->
            this.currency = currency
        }
        amountEditText.setText("")

        val relevantAccounts = coinbaseAccounts.filter { account -> account.currency == currency }

        when (relevantAccounts.size) {
            0 -> {
                destinationAccount = null
                val cbAccountBalance = if (currency.isFiat) {
                     0.0.fiatFormat()
                } else {
                    0.0.btcFormatShortened()
                }
                gdaxBalanceText.text = "GDAX $currency Balance: $cbAccountBalance"

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

        val gdaxAccount = Account.forCurrency(currency)
        if ((gdaxAccount?.availableBalance ?: 0.0) > 0.0) {
            interactiveLayout.visibility = View.VISIBLE
        } else {
            interactiveLayout.visibility = View.INVISIBLE
        }
        amountUnitText.text = currency.toString()
        if ((gdaxAccount?.availableBalance ?: 0.0) > 0) {
            if (currency.isFiat) {
                val gdaxAccountBalance = (gdaxAccount?.availableBalance ?: 0.0).fiatFormat()
                gdaxBalanceText.text = "Available GDAX $currency Balance: $gdaxAccountBalance"
            } else {
                val gdaxAccountBalance = (gdaxAccount?.availableBalance ?: 0.0).btcFormatShortened()
                gdaxBalanceText.text = "Available GDAX $currency Balance: $gdaxAccountBalance $currency"
            }
        } else {
            gdaxBalanceText.text = "GDAX $currency wallet is empty"
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
