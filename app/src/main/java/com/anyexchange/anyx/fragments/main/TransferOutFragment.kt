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

    private lateinit var gdaxBalanceText: TextView

    private lateinit var submitWithdrawalButton: Button

    private lateinit var interactiveLayout: LinearLayout

    private lateinit var currencyTabLayout: TabLayout


    private var coinbaseAccounts: List<Account.CoinbaseAccount> = listOf()
    private var paymentMethods: List<Account.PaymentMethod> = listOf()
    private var relevantAccounts: MutableList<Account.RelatedAccount> = mutableListOf()

    private var destinationAccount: Account.RelatedAccount? = null
    private var currency: Currency = Currency.USD

    companion object {
        fun newInstance(): TransferOutFragment {
            return TransferOutFragment()
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

        val arrayAdapter = RelatedAccountSpinnerAdapter(activity, R.layout.list_row_coinbase_account, relevantAccounts)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cbAccountsSpinner.adapter = arrayAdapter

        cbAccountsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
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
            val gdaxAccount = Account.forCurrency(currency)
            gdaxAccount?.balance?.let { balance ->
                amountEditText.setText(balance.btcFormatShortened())
            }
        }

        submitWithdrawalButton.setOnClickListener {
            val amountString = amountEditText.text.toString()
            val amount = amountString.toDoubleOrZero()

            if (amount <= 0) {
                showPopup("Amount is not valid", { })
            } else if (destinationAccount is Account.CoinbaseAccount) {
                val coinbaseAccount = destinationAccount as Account.CoinbaseAccount
                val gdaxAccount = Account.forCurrency(currency)

                if (amount > gdaxAccount?.availableBalance ?: 0.0) {
                    showPopup("Not enough funds", { })
                } else {
                    (activity as com.anyexchange.anyx.activities.MainActivity).showProgressBar()
                    GdaxApi.sendToCoinbase(amount, currency, coinbaseAccount.id).executePost({ result ->
                        showPopup("Error: " + result.errorMessage, { })
                        activity.dismissProgressBar()
                    }, {
                        toast("Transfer Sent")
                        amountEditText.setText("")

                        refresh { activity.dismissProgressBar() }
                    })
                }
            } else if (destinationAccount is Account.PaymentMethod) {
                val paymentMethod = destinationAccount as Account.PaymentMethod
                if (paymentMethod.balance != null && amount > paymentMethod.balance) {
                    showPopup("Not enough funds", { })
                } else {
                    (activity as com.anyexchange.anyx.activities.MainActivity).showProgressBar()
                    GdaxApi.sendToPayment(amount, currency, paymentMethod.id).executePost( { result ->
                        showPopup("Error: " + result.errorMessage, { })
                        activity.dismissProgressBar()
                    }, {
                        toast("Transfer Sent")
                        amountEditText.setText("")
                        refresh { activity.dismissProgressBar() }
                    })
                }
            } else {
                toast("Error")
            }
        }

        return rootView
    }

    override fun onResume() {
        super.onResume()

        switchCurrency(currency)

        doneLoading()
    }

    private var isRefreshing = false
    override fun refresh(onComplete: (Boolean) -> Unit) {
        if (!isRefreshing) {
            isRefreshing = true
            var didUpdateGDAX = false
            var didUpdateCoinbase = false
            var didUpdatePaymentMethods = false

            GdaxApi.accounts().updateAllAccounts({ result ->
                onComplete(false)
                toast("Cannot access Coinbase Pro")
                isRefreshing = false
            }) {
                didUpdateGDAX = true
                if (didUpdateCoinbase && didUpdatePaymentMethods) {
                    completeRefresh(onComplete)
                    isRefreshing = false
                }
            }
            GdaxApi.coinbaseAccounts().linkToAccounts({ result ->
                toast("Cannot access Coinbase")
                onComplete(false)
                isRefreshing = false
            }, {
                val gdaxAccounts = Account.list.toMutableList()
                val fiatAccount = Account.usdAccount
                if (fiatAccount != null) {
                    gdaxAccounts.add(fiatAccount)
                }
                coinbaseAccounts = gdaxAccounts.mapNotNull { account -> account.coinbaseAccount }.toMutableList()
                didUpdateCoinbase = true
                if (didUpdateGDAX && didUpdatePaymentMethods) {
                    completeRefresh(onComplete)
                    isRefreshing = false
                }
            })
            GdaxApi.paymentMethods().get({
                toast("Error")
                onComplete(false)
            }, { result ->
                paymentMethods = result
                didUpdatePaymentMethods = true
                if (didUpdateGDAX && didUpdateCoinbase) {
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
                val cbAccountBalance = if (currency.isFiat) {
                     0.0.fiatFormat()
                } else {
                    0.0.btcFormatShortened()
                }
                gdaxBalanceText.text = "Coinbase Pro $currency Balance: $cbAccountBalance"

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
                gdaxBalanceText.text = "Available Coinbase Pro $currency Balance: $gdaxAccountBalance"
            } else {
                val gdaxAccountBalance = (gdaxAccount?.availableBalance ?: 0.0).btcFormatShortened()
                gdaxBalanceText.text = "Available Coinbase Pro $currency Balance: $gdaxAccountBalance $currency"
            }
        } else {
            gdaxBalanceText.text = "Coinbase Pro $currency wallet is empty"
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
