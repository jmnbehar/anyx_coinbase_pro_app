package com.anyexchange.anyx.classes

import com.anyexchange.anyx.R
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import org.json.JSONObject
import java.util.*
import com.google.gson.reflect.TypeToken


/**
 * Created by anyexchange on 12/20/2017.
 */

class SimpleFiatProduct(var currency: Currency, var id: String, var quoteCurrency: Currency?, var tradingPairs: List<String>) {

    var price = 0.0

    private var hourCandles = listOf<Candle>()
    private var dayCandles = listOf<Candle>()
    private var weekCandles = listOf<Candle>()
    private var monthCandles = listOf<Candle>()
    private var yearCandles = listOf<Candle>()

    fun toProduct() : Product {
        val tradingPairs = tradingPairs.map { TradingPair(it) }
        val newProduct = Product(currency, id, quoteCurrency, tradingPairs)
        newProduct.setAllBasicCandles(hourCandles, dayCandles, weekCandles, monthCandles, yearCandles)
        return newProduct
    }
}
