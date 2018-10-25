package com.anyexchange.anyx.activities


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

import com.google.zxing.Result

import me.dm7.barcodescanner.zxing.ZXingScannerView

/**
 * Created by joydeep on 28/10/16.
 */
class ScanActivity : Activity(), ZXingScannerView.ResultHandler {

    private var mScannerView: ZXingScannerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.redeem_it);

        mScannerView = ZXingScannerView(this)   // Programmatically initialize the scanner view
        setContentView(mScannerView)

        mScannerView?.setResultHandler(this) // Register ourselves as a handler for scan results.
        mScannerView?.startCamera()         // Start camera


    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    override fun onPause() {
        super.onPause()

        try {
            mScannerView?.stopCamera() // Stop camera on pause
        } catch (e: Exception) {
            Log.e("Error", e.message)
        }

        val resultintent = Intent()
        resultintent.putExtra("BarCode", "")
        setResult(2, resultintent)
        finish()
    }

    override fun onBackPressed() {

        try {
            mScannerView?.stopCamera() // Stop camera on pause
        } catch (e: Exception) {
            Log.e("Error", e.message)
        }

        val resultintent = Intent()
        resultintent.putExtra("BarCode", "")
        setResult(2, resultintent)
        finish()

    }

    override fun handleResult(rawResult: Result) {
        // Do something with the result here
        Log.e("handler", rawResult.text) // Prints scan results
        Log.e("handler", rawResult.barcodeFormat.toString()) // Prints the scan format (qrcode)

        try {
            mScannerView?.stopCamera()

            val resultIntent = Intent()
            resultIntent.putExtra("BarCode", rawResult.text)
            setResult(2, resultIntent)
            finish()
            // Stop camera on pause
        } catch (e: Exception) {
            Log.e("Error", e.message)
        }

    }
}