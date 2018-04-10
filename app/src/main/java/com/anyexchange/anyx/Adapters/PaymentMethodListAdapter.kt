package com.anyexchange.anyx.Adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.anyexchange.anyx.Classes.ApiPaymentMethod
import com.anyexchange.anyx.Classes.inflate
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.list_row_payment_method.view.*

/**
 * Created by anyexchange on 3/14/2018.
 */
class PaymentMethodListAdapter(context: Context, var resource: Int, var list: List<ApiPaymentMethod>) :
        ArrayAdapter<ApiPaymentMethod>(context, resource, list) {


    internal class ViewHolder {
        var paymentMethodNameText: TextView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val viewHolder: ViewHolder?
        val outputView: View

        if (convertView == null) {
            viewHolder = ViewHolder()

            val vi = parent.inflate(R.layout.list_row_payment_method)

            viewHolder.paymentMethodNameText = vi.txt_payment_method_name

            vi.tag = viewHolder
            outputView = vi
        } else {
            viewHolder = convertView.tag as ViewHolder
            outputView = convertView
        }

        val paymentMethod = list[position]

        viewHolder.paymentMethodNameText?.text = paymentMethod.name

        return outputView
    }
}