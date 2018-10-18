package com.anyexchange.anyx.adapters.spinnerAdapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.anyexchange.anyx.classes.inflate
import com.anyexchange.anyx.R
import com.anyexchange.anyx.classes.TradingPair
import kotlinx.android.synthetic.main.list_row_trading_pair.view.*
import org.jetbrains.anko.backgroundColor

/**
 * Created by anyexchange on 3/14/2018.
 */
class TradingPairSpinnerAdapter(context: Context, private var tradingPairList: List<TradingPair>) :
        ArrayAdapter<TradingPair>(context, layoutId, textResId, tradingPairList) {

    companion object {
        const val layoutId = R.layout.list_row_trading_pair
        const val textResId = R.id.txt_trading_pair_id
    }

    internal class ViewHolder {
        var view: View? = null
        var tradingPairText: TextView? = null
        var quoteCurrencyIcon: ImageView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        return getViewGeneric(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getViewGeneric(position, convertView, parent)
    }

    private fun getViewGeneric(position: Int, convertView: View?, parent: ViewGroup): View {
        val viewHolder: ViewHolder?
        val outputView: View
        if (convertView == null) {
            viewHolder = ViewHolder()

            val vi = parent.inflate(layoutId)

            viewHolder.view = vi
            viewHolder.tradingPairText = vi.txt_trading_pair_id
            viewHolder.quoteCurrencyIcon = vi.img_trading_pair_quote_icon

            vi.tag = viewHolder
            outputView = vi
        } else {
            viewHolder = convertView.tag as ViewHolder
            outputView = convertView
        }

        val tradingPair = tradingPairList[position]
        viewHolder.tradingPairText?.text = tradingPair.toString()
        viewHolder.tradingPairText?.textColor = Color.WHITE
        viewHolder.quoteCurrencyIcon?.visibility = View.GONE
        viewHolder.view?.backgroundColor = context.resources.getColor(R.color.dark_accent, null)

//        viewHolder.quoteCurrencyIcon?.setImageResource(tradingPair.quoteCurrency.iconId)

        return outputView
    }
}