package com.jmnbehar.anyx.Fragments.Main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.jmnbehar.anyx.Activities.MainActivity
import com.jmnbehar.anyx.Adapters.CoinbaseAccountListAdapter
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_depost_coinbase.view.*
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast

/**
 * Created by jmnbehar on 11/5/2017.
 */
class DepositCoinbaseFragment : RefreshFragment() {

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

    private lateinit var submitDepositButton: Button

    private var coinbaseAccounts: List<Account.CoinbaseAccount> = listOf()
    private var coinbaseAccount: Account.CoinbaseAccount? = null

    companion object {
        fun newInstance(): DepositCoinbaseFragment {
            return DepositCoinbaseFragment()
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

        submitDepositButton = rootView.btn_deposit_coinbase_deposit
//        val buttonColors = account.currency.colorStateList(activity)
//        submitDepositButton.backgroundTintList = buttonColors
//        val buttonTextColor = account.currency.buttonTextColor(activity)
//        submitDepositButton.textColor = buttonTextColor

        //titleText.text = "Buy and Sell " + account.currency.toString()

        titleText.text = "Deposit from Coinbase"



        coinbaseAccounts = Account.list.mapNotNull { account -> account.coinbaseAccount }
        coinbaseAccounts = coinbaseAccounts.filter { cbAccount -> cbAccount.balance > 0 }
        if (coinbaseAccounts.isEmpty()) {
            depositDetailsLayout.visibility = View.GONE
            titleText.text = "All Coinbase accounts are empty"
        } else {
            depositDetailsLayout.visibility = View.VISIBLE
            titleText.text = "Deposit from Coinbase"

            val arrayAdapter = CoinbaseAccountListAdapter(activity, R.layout.list_row_coinbase_account, coinbaseAccounts)

            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            accountsSpinner.adapter = arrayAdapter

            coinbaseAccount = coinbaseAccounts.first()
            val currency = coinbaseAccount?.currency
            if (currency != null) {
                amountUnitText.text = currency.toString()
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

        depositMaxButton.setOnClickListener {
            val selectedCoinbaseAccount = accountsSpinner.selectedItem as Account.CoinbaseAccount
            val amount = selectedCoinbaseAccount.balance
            amountEditText.setText(amount.btcFormatShortened())
        }

        submitDepositButton.setOnClickListener {
            val amountString = amountEditText.text.toString()
            val amount = amountString.toDoubleOrZero()

            val selectedCoinbaseAccount = accountsSpinner.selectedItem as Account.CoinbaseAccount
            val currency = selectedCoinbaseAccount.currency

            if (amount <= 0) {
                showPopup("Amount is not valid", { })
            } else if (amount > selectedCoinbaseAccount.balance) {
                showPopup("Not enough funds", { })
            } else {
                TransferHub.getFromCoinbase(amount, currency, { errorString ->
                    showPopup("Deposit failed\n Error: $errorString", { })

                } , {
                    toast("Deposit received")
                    amountEditText.setText("")
                })
            }
        }

        return rootView
    }

}
