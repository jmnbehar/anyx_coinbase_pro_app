package com.jmnbehar.anyx.Fragments.Main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.jmnbehar.anyx.Activities.MainActivity
import com.jmnbehar.anyx.Adapters.PaymentMethodListAdapter
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_depost_coinbase.view.*
import org.jetbrains.anko.support.v4.toast

/**
 * Created by jmnbehar on 11/5/2017.
 */
class DepositBankFragment : RefreshFragment() {

    private lateinit var inflater: LayoutInflater

    private lateinit var titleText: TextView

    private lateinit var depositDetailsLayout: LinearLayout

    private lateinit var accountsLabelTxt: TextView
    private lateinit var paymentSpinner: Spinner

    private lateinit var depositMaxButton: Button

    private lateinit var amountLabelText: TextView
    private lateinit var amountEditText: EditText
    private lateinit var amountUnitText: TextView

    private lateinit var infoText: TextView

    private lateinit var submitDepositButton: Button

    private var paymentMethods: List<ApiPaymentMethod> = listOf()
    private var paymentMethod: ApiPaymentMethod? = null

    var currency = Currency.USD

    companion object {
        fun newInstance(): DepositBankFragment {
            return DepositBankFragment()
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
        paymentSpinner = rootView.spinner_deposit_coinbase_accounts

        infoText = rootView.txt_deposit_coinbase_info

        submitDepositButton = rootView.btn_deposit_coinbase_deposit
//        val buttonColors = account.currency.colorStateList(activity)
//        submitDepositButton.backgroundTintList = buttonColors
//        val buttonTextColor = account.currency.buttonTextColor(activity)
//        submitDepositButton.textColor = buttonTextColor
        titleText.text = "Deposit from Bank Account"

        amountUnitText.text = currency.toString()

        (activity as MainActivity).showProgressBar()

        GdaxApi.paymentMethods().get({
            doneLoading()
            showPopup( "Can't access payment methods", { activity.onBackPressed() })
        }, { result ->
            //TODO: test this thoroughly
            paymentMethods = result.filter { paymentMethod -> paymentMethod.allow_deposit }

            if (paymentMethods.isEmpty()) {
                depositDetailsLayout.visibility = View.GONE
                titleText.text = "No valid bank accounts"
            } else {

                val arrayAdapter = PaymentMethodListAdapter(activity, R.layout.list_row_payment_method, paymentMethods)
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                paymentSpinner.adapter = arrayAdapter

                val paymentMethod = paymentSpinner.selectedItem as ApiPaymentMethod

                if (paymentMethod.balance.toDoubleOrNull() != null) {
                    depositMaxButton.visibility = View.VISIBLE
                } else {
                    depositMaxButton.visibility = View.GONE
                }

                titleText.text = "Deposit from Bank Account"
            }
            doneLoading()
        })

        depositMaxButton.visibility = View.GONE
        depositMaxButton.setOnClickListener {
            val selectedCoinbaseAccount = paymentSpinner.selectedItem as Account.CoinbaseAccount
            val amount = selectedCoinbaseAccount.balance
            amountEditText.setText(amount.fiatFormat())
        }

        submitDepositButton.setOnClickListener {
            val amountString = amountEditText.text.toString()
            val amount = amountString.toDoubleOrZero()

            val paymentMethod = paymentSpinner.selectedItem as ApiPaymentMethod
            val currency = Currency.forString(paymentMethod.currency) ?: Currency.USD

            if (amount <= 0) {
                showPopup("Amount is not valid", { })
            } else {

                GdaxApi.sendToPayment(amount, currency, paymentMethod.id).executePost( { errorResult ->

                    showPopup("Deposit failed\n Error: ${errorResult.error.message}", { })
                }, { result ->
                    toast("Deposit received")
                    amountEditText.setText("")
                } )
            }
        }

        return rootView
    }

}
