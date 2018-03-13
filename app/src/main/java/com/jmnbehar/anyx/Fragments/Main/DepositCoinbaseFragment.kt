package com.jmnbehar.anyx.Fragments.Main

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jmnbehar.anyx.Activities.MainActivity
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_depost_coinbase.view.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onCheckedChange
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast

/**
 * Created by jmnbehar on 11/5/2017.
 */
class DepositCoinbaseFragment : RefreshFragment() {

    private lateinit var inflater: LayoutInflater

    private lateinit var titleText: TextView

    private lateinit var accountsLabelTxt: TextView
    private lateinit var accountsSpinner: Spinner

    private lateinit var amountLabelText: TextView
    private lateinit var amountEditText: EditText
    private lateinit var amountUnitText: TextView

    private lateinit var infoText: TextView

    private lateinit var submitDepositButton: Button

    private var currency = Currency.USD

    companion object {
        lateinit var account: Account
        fun newInstance(): DepositCoinbaseFragment {
            return DepositCoinbaseFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_depost_coinbase, container, false)

        this.inflater = inflater
        val activity = activity!!
        titleText = rootView.txt_deposit_coinbase_title

        amountLabelText = rootView.txt_deposit_coinbase_amount_label
        amountEditText = rootView.etxt_deposit_coinbase_amount
        amountUnitText = rootView.txt_deposit_coinbase_amount_unit

        accountsLabelTxt = rootView.txt_deposit_coinbase_account_label
        accountsSpinner = rootView.spinner_deposit_coinbase_accounts

        infoText = rootView.txt_deposit_coinbase_info

        submitDepositButton = rootView.btn_deposit_coinbase_deposit
        val buttonColors = account.currency.colorStateList(activity)
        submitDepositButton.backgroundTintList = buttonColors
        val buttonTextColor = account.currency.buttonTextColor(activity)
        submitDepositButton.textColor = buttonTextColor

        //titleText.text = "Buy and Sell " + account.currency.toString()

        var coinbaseAccounts: List<ApiCoinbaseAccount> = listOf()
        MainActivity.progressDialog?.show()
        GdaxApi.coinbaseAccounts().get({
            MainActivity.progressDialog?.dismiss()
            //show a popup, dont allow access to this fragment
        }, { result ->
            coinbaseAccounts = result
        })

        val spinnerList = coinbaseAccounts.map { t -> t.name }
        val arrayAdapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, spinnerList)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        accountsSpinner.adapter = arrayAdapter
        accountsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedItem = coinbaseAccounts[position]
                currency = Currency.fromString(selectedItem.currency)
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

        val onFailure = { result: Result.Failure<String, FuelError> ->  println("Error!: ${result.error}") }

        submitDepositButton.setOnClickListener {
            val amountString = amountEditText.text.toString()
            val amount = amountString.toDoubleOrZero()

            if (amount <= 0) {
                toast("Amount is not valid")
            } else {
                TransferHub.getFromCoinbase(amount, currency)
            }
        }

        return rootView
    }

}
