package com.anyexchange.anyx.fragments.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.adapters.spinnerAdapters.RelatedAccountSpinnerAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.activities.ScanActivity
import com.anyexchange.anyx.api.AnyApi
import com.anyexchange.anyx.api.CBProApi
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import kotlinx.android.synthetic.main.fragment_transfer.view.*
import net.glxn.qrgen.android.QRCode
import org.jetbrains.anko.textColor

/**
 * Created by anyexchange on 11/5/2017.
 */
class TransferFragment : RefreshFragment() {

    private lateinit var inflater: LayoutInflater

    private lateinit var titleText: TextView

    private lateinit var transferDetailsLayout: LinearLayout

    private lateinit var interactiveLayout: LinearLayout

    private lateinit var destinationAddressEditLayout: LinearLayout
    private var destinationAddressEditText: EditText? = null
    private var scanButton: ImageButton? = null

    private lateinit var depositAddressViewLayout: LinearLayout
    private var qrCodeImageView: ImageView? = null

    private var sourceAccountsLabelTxt: TextView? = null
    private var sourceAccountsSpinner: Spinner? = null
    private var sourceAccountText: TextView? = null

    private var transferMaxButton: Button? = null

    private var amountLabelText: TextView? = null
    private var amountEditText: EditText? = null
    private var amountUnitText: TextView? = null

    private var infoText: TextView? = null
    private var destAccountsSpinner: Spinner? = null
    private var destBalanceText: TextView? = null

    private var submitTransferButton: Button? = null

    private var coinbaseAccounts: List<Account.CoinbaseAccount> = listOf()

    private var sourceAccounts: MutableList<BaseAccount> = mutableListOf()
    private val sourceAccount: BaseAccount?
        get() {
            val spinnerSelection = sourceAccountsSpinner?.selectedItem as? BaseAccount
            return if (spinnerSelection?.currency == currency) {
                spinnerSelection
            } else {
                sourceAccounts.firstOrNull()
            }
        }
    private var destAccounts: List<BaseAccount> = mutableListOf()
    private val destAccount: BaseAccount?
        get() {
            return if (destAccounts.size > 1) {
                destAccountsSpinner?.selectedItem as? BaseAccount ?: destAccounts.firstOrNull()
            } else {
                destAccounts.firstOrNull()
            }
        }

    var currency: Currency = ChartFragment.currency
        set(value) {
            field = value
            ChartFragment.currency = value
        }

    var blockNextSelectSource = false
    var blockNextSelectDest = false


    companion object {
        fun newInstance(): TransferFragment {
            return TransferFragment()
        }

        val hasRelevantData: Boolean
            get() {
                val cryptoCbProAccounts = Account.allCryptoAccounts().filter { it.exchange == Exchange.CBPro }
                val cryptoCBAccounts = cryptoCbProAccounts.mapNotNull { account -> account.coinbaseAccount }
                val fiatCBProAccounts = Account.fiatAccounts.filter { it.exchange == Exchange.CBPro }
                val fiatCBAccounts = fiatCBProAccounts.mapNotNull { account -> account.coinbaseAccount }
                val cbAccountsAreMissing = (cryptoCBAccounts.size < cryptoCbProAccounts.size ||
                                            fiatCBAccounts.size < fiatCBProAccounts.size)
                return (!cbAccountsAreMissing && Account.paymentMethods.isNotEmpty() && fiatCBProAccounts.isNotEmpty())
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_transfer, container, false)
        setupSwipeRefresh(rootView.swipe_refresh_layout)

        this.inflater = inflater
        titleText = rootView.txt_transfer_title

        transferDetailsLayout = rootView.layout_transfer_details
        interactiveLayout = rootView.layout_transfer_interactive_layout

        amountLabelText = rootView.txt_transfer_amount_label
        amountEditText = rootView.etxt_transfer_amount
        amountUnitText = rootView.txt_transfer_amount_unit

        transferMaxButton = rootView.btn_transfer_max

        sourceAccountsLabelTxt = rootView.txt_transfer_account_label
        sourceAccountsSpinner = rootView.spinner_transfer_accounts
        sourceAccountText = rootView.txt_transfer_account_info

        destinationAddressEditLayout = rootView.layout_transfer_external_address_input
        destinationAddressEditText = rootView.etxt_send_destination
        scanButton = rootView.btn_send_destination_camera
        scanButton?.setOnClickListener { getAddressFromCamera() }

        depositAddressViewLayout = rootView.layout_transfer_deposit_qr_code
        qrCodeImageView = rootView.img_receive_qr_code

        destAccountsSpinner = rootView.spinner_transfer_destination_accounts
        infoText = rootView.txt_transfer_info
        destBalanceText = rootView.txt_transfer_destination_info

        submitTransferButton = rootView.btn_transfer_submit_transfer

        context?.let {
            sourceAccountsSpinner?.adapter = RelatedAccountSpinnerAdapter(it, sourceAccounts)
            sourceAccountsSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (sourceAccounts.size > position && !blockNextSelectSource) {
                        sourceAccountSelected()
                    }
                    blockNextSelectSource = false
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
            destAccountsSpinner?.adapter = RelatedAccountSpinnerAdapter(it, listOf())
            destAccountsSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (!blockNextSelectDest) {
                        setInfoAndButtons()
                    }
                    blockNextSelectDest = false
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        }

        transferMaxButton?.setOnClickListener {
            sourceAccount?.balance?.let { balance ->
                amountEditText?.setText(balance.btcFormatShortened())
            }
        }

        submitTransferButton?.setOnClickListener {
            submitTransfer()
        }

        return rootView
    }

    override fun onResume() {
        super.onResume()

        val relevantCurrencies = Account.fiatAccounts.asSequence().map { it.currency }.toMutableList()
        val cryptoList = Product.map.keys.map { Currency(it) }
        relevantCurrencies.addAll(cryptoList)
        showNavSpinner(currency, relevantCurrencies) { selectedCurrency ->
            currency = selectedCurrency
            switchCurrency(selectedCurrency)
        }

        titleText.text = getString(R.string.transfer_title)

        val cryptoCBAccounts = Account.allCryptoAccounts().mapNotNull { account -> account.coinbaseAccount }
        val stableCBAccounts = Account.fiatAccounts.mapNotNull { account -> account.coinbaseAccount }
        coinbaseAccounts =  cryptoCBAccounts.plus(stableCBAccounts)

        amountUnitText?.text = currency.toString()

        switchCurrency(currency)

        dismissProgressSpinner()
    }

    private var isRefreshing = false
    override fun refresh(onComplete: (Boolean) -> Unit) {
        //TODO: don't reset spinners on refresh
        if (!isRefreshing) {
            isRefreshing = true
            var didUpdateCBPro = false
            var didUpdateCoinbase = false
            var didUpdatePaymentMethods = false
            CBProApi.accounts(apiInitData).updateAllAccounts({
                toast(R.string.toast_coinbase_pro_site_error)
                isRefreshing = false
                onComplete(false)
            }) {
                didUpdateCBPro = true
                if (didUpdateCoinbase && didUpdatePaymentMethods) {
                    completeRefresh(onComplete)
                    isRefreshing = false
                }
            }
            CBProApi.coinbaseAccounts(apiInitData).linkToAccounts({
                toast(R.string.toast_coinbase_site_error)
                isRefreshing = false
                onComplete(false)
            }, { allCoinbaseAccounts ->
                coinbaseAccounts = allCoinbaseAccounts
                didUpdateCoinbase = true
                if (didUpdateCBPro && didUpdatePaymentMethods) {
                    completeRefresh(onComplete)
                    isRefreshing = false
                }
            })
            CBProApi.paymentMethods(apiInitData).get({
                onComplete(false)
            }, { result ->
                Account.paymentMethods = result
                didUpdatePaymentMethods = true
                if (didUpdateCBPro && didUpdateCoinbase) {
                    completeRefresh(onComplete)
                    isRefreshing = false
                }
            })
        }
    }

    private fun completeRefresh(onComplete: (Boolean) -> Unit) {
        val tempSourceIndex = sourceAccountsSpinner?.selectedItemPosition ?: -1
        val tempDestIndex = destAccountsSpinner?.selectedItemPosition ?: -1
        if (isVisible) {
            transferDetailsLayout.visibility = View.VISIBLE
            amountUnitText?.text = currency.toString()
            switchCurrency(currency)
        }
        if (tempSourceIndex >= 0 && tempSourceIndex < sourceAccounts.size) {
            sourceAccountsSpinner?.setSelection(tempSourceIndex)
            sourceAccountSelected()
        }
        if (tempDestIndex >= 0 && tempDestIndex < destAccounts.size) {
            destAccountsSpinner?.setSelection(tempDestIndex)
            setInfoAndButtons()
        }
        onComplete(true)
    }

    private fun switchCurrency(currency: Currency) {
        this.currency = currency
        amountEditText?.setText("")

        val accounts = Product.map[currency.id]?.accounts?.values ?: listOf()
        var accountsChecked = 0
        for (account in accounts) {
            if (account.depositInfo == null) {
                account.getDepositAddress(apiInitData, {
                    accountsChecked++
                    if (accountsChecked == accounts.size) {
                        completeSwitchCurrency()
                    }
                }) {
                    accountsChecked++
                    if (accountsChecked == accounts.size) {
                        completeSwitchCurrency()
                    }
                }
            }
        }
        if (accounts.isEmpty()) {
            completeSwitchCurrency()
        }
    }

    private fun completeSwitchCurrency() {
        val tempRelevantAccounts: MutableList<BaseAccount> = coinbaseAccounts.asSequence().filter { account -> account.currency == currency }.toMutableList()

        when (currency.type) {
            Currency.Type.CRYPTO -> {
                Product.map[currency.id]?.accounts?.values?.let {
                    tempRelevantAccounts.addAll(it)
                }
            }
            Currency.Type.FIAT, Currency.Type.STABLECOIN -> {
                tempRelevantAccounts.addAll(Account.fiatAccounts.filter { it.currency == currency })
                tempRelevantAccounts.addAll(Account.paymentMethods.filter { pm -> pm.apiPaymentMethod.allow_withdraw && pm.apiPaymentMethod.currency == currency.toString() })
            }
        }
        val dummyExternalAccount = Account.ExternalAccount(currency)
        tempRelevantAccounts.add(dummyExternalAccount)

        sourceAccounts = tempRelevantAccounts

        when (sourceAccounts.size) {
            0 -> {
                sourceAccountText?.text = resources.getString(R.string.transfer_coinbase_account_empty, currency.toString())
                sourceAccountText?.visibility = View.VISIBLE
                sourceAccountsSpinner?.visibility = View.GONE
            }
            1 -> {
                sourceAccountText?.text = sourceAccount.toString()
                sourceAccountText?.visibility = View.VISIBLE
                sourceAccountsSpinner?.visibility = View.GONE
            }
            else -> {
                context?.let {
                    sourceAccountsSpinner?.adapter = RelatedAccountSpinnerAdapter(it, sourceAccounts)
                }

                sourceAccountsSpinner?.visibility = View.VISIBLE
                sourceAccountText?.visibility = View.GONE
                blockNextSelectSource = true
            }
        }

        setDestAccounts()

        setInfoAndButtons()
    }

    private fun sourceAccountSelected() {
        setDestAccounts()
        setInfoAndButtons()
    }

    private fun setDestAccounts() {
        val cbproAccount = when (currency.type) {
            Currency.Type.CRYPTO -> Product.map[currency.id]?.accounts?.get(Exchange.CBPro)
            else -> Account.fiatAccounts.find { it.currency == currency }
        }
        val sourceAccount = sourceAccount
        destAccounts = when(sourceAccount) {
            is Account.CoinbaseAccount -> listOfNotNull(cbproAccount)
            is Account.PaymentMethod ->  listOfNotNull(cbproAccount)
            is Account -> {
                val tempDestAccounts = mutableListOf<BaseAccount>()

                val otherExchangeAccounts = Product.map[currency.id]?.accounts?.values?.filter { it.exchange != sourceAccount.exchange } ?: listOf()
                tempDestAccounts.addAll(otherExchangeAccounts)

                if (sourceAccount.exchange == Exchange.CBPro) {
                    val relevantCBAccounts = coinbaseAccounts.asSequence().filter { it.currency == currency }
                    tempDestAccounts.addAll(relevantCBAccounts)
                }
                if (currency.type == Currency.Type.FIAT) {
                    tempDestAccounts.addAll(Account.paymentMethods.filter { pm -> pm.apiPaymentMethod.allow_withdraw && pm.apiPaymentMethod.currency == currency.toString() })
                }
                val dummyExternalAccount = Account.ExternalAccount(currency)
                tempDestAccounts.add(dummyExternalAccount)

                tempDestAccounts.toList()
            }
            is Account.ExternalAccount -> Product.map[currency.id]?.accounts?.values?.toList() ?: listOf()
            else  -> listOf()
        }
        when (destAccounts.size) {
            0 -> {
                destBalanceText?.text = resources.getString(R.string.transfer_no_destinations_text)
                destBalanceText?.visibility = View.VISIBLE
                destAccountsSpinner?.visibility = View.GONE
            }
            1 -> {
                destBalanceText?.text = destAccount.toString()
                destBalanceText?.visibility = View.VISIBLE
                destAccountsSpinner?.visibility = View.GONE
            }
            else -> {
                context?.let {
                    destAccountsSpinner?.adapter = RelatedAccountSpinnerAdapter(it, destAccounts)
                    destBalanceText?.visibility = View.GONE
                    destAccountsSpinner?.visibility = View.VISIBLE
                    blockNextSelectDest = true
                }
            }
        }
        amountUnitText?.text = currency.toString()
    }

    private fun setInfoAndButtons() {
        infoText?.visibility = View.VISIBLE
        interactiveLayout.visibility = View.VISIBLE
        depositAddressViewLayout.visibility = View.GONE
        destinationAddressEditLayout.visibility = View.GONE

        when (sourceAccount) {
            is Account.CoinbaseAccount -> {
                setInteractiveLayoutEnabled((sourceAccount?.balance ?: 0.0) > 0.0)
                infoText?.setText(R.string.transfer_coinbase_info)
            }
            is Account.PaymentMethod -> {
                infoText?.setText(R.string.transfer_bank_info)
            }
            is Account -> {
                setInteractiveLayoutEnabled((sourceAccount?.balance ?: 0.0) > 0.0)
                when (destAccount) {
                    is Account.CoinbaseAccount -> infoText?.setText(R.string.transfer_coinbase_info)
                    is Account.PaymentMethod -> infoText?.setText(R.string.transfer_bank_info)
                    is Account.ExternalAccount -> {
                        destinationAddressEditLayout.visibility = View.VISIBLE
                        infoText?.setText(R.string.transfer_bank_info)
                    }
                }
            }
            is Account.ExternalAccount -> {
                interactiveLayout.visibility = View.GONE
                depositAddressViewLayout.visibility = View.VISIBLE

                (destAccount as? Account)?.depositInfo
                showAddressInfo((destAccount as? Account)?.depositInfo)
            }
            else -> {
                interactiveLayout.visibility = View.GONE
                infoText?.visibility = View.GONE
            }
        }
        if (destAccounts.isEmpty()) {
            setInteractiveLayoutEnabled(false)
        }

        context?.let { context ->
            amountUnitText?.text = currency.toString()

            val buttonColors = currency.colorStateList(context)
            val buttonTextColor = currency.buttonTextColor(context)

            transferMaxButton?.backgroundTintList = buttonColors
            submitTransferButton?.backgroundTintList = buttonColors

            transferMaxButton?.textColor = buttonTextColor
            submitTransferButton?.textColor = buttonTextColor
        }
    }

    private fun setInteractiveLayoutEnabled(enabled: Boolean) {
        if (!enabled) {
            submitTransferButton?.alpha = 0.2f
            transferMaxButton?.alpha = 0.2f
            amountEditText?.setText("0.0")
        } else {
            submitTransferButton?.alpha = 1.0f
            transferMaxButton?.alpha = 1.0f
            amountEditText?.setText("")
        }
        submitTransferButton?.isEnabled = enabled
        amountEditText?.isEnabled = enabled
        transferMaxButton?.isEnabled = enabled
    }

    private fun submitTransfer() {
        val amountString = amountEditText?.text.toString()
        val amount = amountString.toDoubleOrZero()

        if (amount <= 0) {
            showPopup(R.string.transfer_amount_error)
        } else {
            val sourceAccount = sourceAccount
            when (sourceAccount) {
                is Account.CoinbaseAccount -> {
                    //send from cb to cbpro
                    transferFromCoinbasePrime(amount, sourceAccount)
                }
                is Account.PaymentMethod -> {
                    //send from bank to cb pro
                    transferFromPaymentMethod(amount, sourceAccount)
                }
                is Account.ExternalAccount -> {
                    //This can never happen
                }
                is Account -> {
                    transferFromAccount(amount, sourceAccount)
                }
                else -> {
                    showPopup(R.string.error_message)
                }
            }
        }
    }

    private fun transferFromCoinbasePrime(amount: Double, coinbaseAccount: Account.CoinbaseAccount) {
        //send from cb to cbpro
        if (amount > coinbaseAccount.balance) {
            showPopup(R.string.transfer_funds_error)
        } else {
            showProgressSpinner()
            CBProApi.getFromCoinbase(apiInitData, amount, currency, coinbaseAccount.id).executePost( { result ->
                val errorMessage = CBProApi.ErrorMessage.forString(result.errorMessage)
                if (amount > 0 && errorMessage == CBProApi.ErrorMessage.TransferAmountTooLow) {
                    showPopup(R.string.transfer_amount_low_error)
                } else {
                    showPopup(resources.getString(R.string.error_generic_message, result.errorMessage))
                }
                dismissProgressSpinner()
            } , {
                basicOnSuccess()
            })
        }
    }

    private fun transferFromPaymentMethod(amount: Double, paymentMethod: Account.PaymentMethod) {
        //send from bank to cb pro
        val balance = paymentMethod.balance
        if (balance != null && amount > balance) {
            showPopup(R.string.transfer_funds_error)
        } else {
            showProgressSpinner()
            CBProApi.getFromPayment(apiInitData, amount, currency, paymentMethod.id).executePost( basicOnFailure , {
                basicOnSuccess()
            })
        }
    }

    private fun transferFromAccount(amount: Double, sourceAccount: Account) {
        val destAccount = destAccount
        when (destAccount) {
            is Account.CoinbaseAccount -> {
                //send from cbPro to cb
                val coinbaseAccount = destAccount
                val cbproAccount = Product.map[currency.id]?.accounts?.get(Exchange.CBPro)

                if (amount > cbproAccount?.availableBalance ?: 0.0) {
                    showPopup(R.string.transfer_funds_error)
                } else {
                    showProgressSpinner()
                    CBProApi.sendToCoinbase(apiInitData, amount, currency, coinbaseAccount.id).executePost(basicOnFailure, {
                        basicOnSuccess()
                    })
                }
            }
            is Account.PaymentMethod -> {
                //send from cbpro to bank
                val paymentMethod = destAccount as Account.PaymentMethod
                if (paymentMethod.balance != null && amount > paymentMethod.balance) {
                    showPopup(R.string.transfer_funds_error)
                } else {
                    showProgressSpinner()
                    CBProApi.sendToPayment(apiInitData, amount, currency, paymentMethod.id).executePost(basicOnFailure) {
                        basicOnSuccess()
                    }
                }
            }
            is Account.ExternalAccount -> {
                val destAddress = destinationAddressEditText.toString()
                showPopup("Are you sure you want to send $amount $currency to $destAddress?", {
                    AnyApi(apiInitData).sendCrypto(currency, amount, sourceAccount.exchange, destAddress, basicOnFailure) {
                        basicOnSuccess()
                    }
                }, "No", { /* do nothing */ })
            }
            is Account -> {
                //send between exchanges
                val destAddress = destAccount.depositInfo?.address
                if (sourceAccount.exchange != destAccount.exchange && destAddress != null) {
                    showPopup("Are you sure you want to send $amount $currency from ${sourceAccount.exchange} to ${destAccount.exchange}?", {
                        AnyApi(apiInitData).sendCrypto(currency, amount, sourceAccount.exchange, destAddress, basicOnFailure) {
                            basicOnSuccess()
                        }
                    }, "No", { /* do nothing */ })
                } else {
                    basicOnFailure(Result.Failure(FuelError(Exception())))
                }
            }
        }
    }


    private val basicOnFailure: (result: Result.Failure<Any, FuelError>) -> Unit = { result ->
        showPopup(resources.getString(R.string.error_generic_message, result.errorMessage))
        dismissProgressSpinner()
    }

    private fun basicOnSuccess() {
        toast(R.string.transfer_sent_message)
        amountEditText?.setText("")
        refresh { dismissProgressSpinner() }
    }


    private fun showAddressInfo(addressInfo: DepositAddressInfo?) {
        //show deposit address

        if (addressInfo != null) {
            val bitmap = QRCode.from(addressInfo.address).withSize(1000, 1000).bitmap()
            qrCodeImageView?.setImageBitmap(bitmap)
            qrCodeImageView?.visibility = View.VISIBLE
            infoText?.text = "Deposit address: " + (addressInfo.address)

        } else {
            qrCodeImageView?.visibility = View.GONE
            infoText?.text = "Deposit address not available"

        }
    }



    private fun getAddressFromCamera() {
        activity?.let { activity ->
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                // Ask for camera permission
                ActivityCompat.requestPermissions(activity,
                        arrayOf(Manifest.permission.CAMERA),
                        666)
            } else {
                val intent = Intent(activity, ScanActivity::class.java)
                startActivityForResult(intent, 2)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        val extras = data.extras
        if (extras != null) {
            val barcode = extras.getString("BarCode")
            if (barcode == "") {
                toast(R.string.send_address_not_found_warning)
            } else {
                //TODO: parse more advanced qr codes
                destinationAddressEditText?.setText(barcode)
            }
        }
    }
}
