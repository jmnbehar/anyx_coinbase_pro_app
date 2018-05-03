package com.anyexchange.anyx.classes

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
data class Candle(
        val time: Double,
        val low: Double,
        val high: Double,
        val open: Double,
        val close: Double,
        val volume: Double) : Parcelable  {

     companion object {
         fun granularityForTimespan(timespan: Timespan) : Long {
             return when (timespan) {
                 Timespan.HOUR -> TimeInSeconds.oneMinute
                 Timespan.DAY -> TimeInSeconds.fiveMinutes
                 Timespan.WEEK -> TimeInSeconds.oneHour
                 Timespan.MONTH -> TimeInSeconds.sixHours
                 Timespan.YEAR -> TimeInSeconds.oneDay
                 Timespan.ALL -> TimeInSeconds.oneDay
//                 TimeInSeconds.halfHour -> TimeInSeconds.oneMinute
//                 TimeInSeconds.sixHours -> TimeInSeconds.fiveMinutes
//                 TimeInSeconds.twoWeeks -> TimeInSeconds.oneHour
             }
         }
     }
}

fun MutableList<Candle>.addingCandles(newList: List<Candle>) : MutableList<Candle> {
    val previouslyEmpty = this.isEmpty()
    this.addAll(0, newList)
    //TODO: assess whether or not distinctby is a big waste of time
    return if (previouslyEmpty) {
        this
    } else {
        val distinctList = this.distinctBy { it.time }
        distinctList.toMutableList()
    }
}

fun MutableList<Candle>.sorted() : MutableList<Candle> {
    return this.sortedWith(compareBy({ it.time }, { it.close })).toMutableList()
}
