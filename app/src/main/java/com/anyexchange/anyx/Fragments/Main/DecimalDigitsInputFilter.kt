package com.anyexchange.anyx.Fragments.Main

import android.text.Spanned
import android.text.InputFilter
import java.util.regex.Pattern


/**
 * Created by anyexchange on 3/27/2018.
 */

class DecimalDigitsInputFilter(val digitsBeforeZero: Int, val digitsAfterZero: Int) : InputFilter {


    override fun filter(source: CharSequence,
                        start: Int,
                        end: Int,
                        dest: Spanned,
                        dstart: Int,
                        dend: Int): CharSequence? {


        var dotPos = -1
        val len = dest.length
        for (i in 0 until len) {
            val c = dest[i]
            if (c == '.' || c == ',') {
                dotPos = i
                break
            }
        }
        if (dotPos >= 0) {

            // protects against many dots
            if (source == "." || source == ",") {
                return ""
            }
            // if the text is entered before the dot
            if (dend <= dotPos) {
                return if (dotPos >= digitsBeforeZero) "" else null
            } else if (len - dotPos > digitsAfterZero) {
                return ""
            }
        } else if (source != "." && len >= digitsBeforeZero){
            return ""
        }
        return null
    }
}