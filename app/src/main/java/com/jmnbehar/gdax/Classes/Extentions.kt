package com.jmnbehar.gdax.Classes

import android.content.Context
import android.widget.Toast

/**
 * Created by josephbehar on 12/28/17.
 */


fun Context.toast(message: CharSequence) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun Context.toastLong(message: CharSequence) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()