package com.jmnbehar.gdax.Classes

import android.content.Context
import android.support.v4.app.Fragment
import android.widget.Toast
import java.text.DecimalFormat
import java.text.NumberFormat

/**
 * Created by josephbehar on 12/28/17.
 */


fun Context.toast(message: CharSequence) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
fun Context.toastLong(message: CharSequence) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()


fun Fragment.toast(message: CharSequence, context: Context) =
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
fun Fragment.toastLong(message: CharSequence, context: Context) =
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()

fun DecimalFormat.getBitcoinFormat(value: Double) = "%.8f".format(value)

fun String.toDoubleOrZero() = this.toDoubleOrNull() ?: 0.0