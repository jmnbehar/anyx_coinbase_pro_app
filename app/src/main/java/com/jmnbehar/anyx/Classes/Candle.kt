package com.jmnbehar.anyx.Classes

data class Candle(
        val time: Double,
        val low: Double,
        val high: Double,
        val open: Double,
        val close: Double,
        val volume: Double) {

     companion object {
         fun granularityForTimespan(timespan: Long) : Long {
             return when (timespan) {
                 TimeInSeconds.halfHour -> TimeInSeconds.oneMinute
                 TimeInSeconds.oneHour -> TimeInSeconds.oneMinute
                 TimeInSeconds.sixHours -> TimeInSeconds.fiveMinutes
                 TimeInSeconds.oneDay -> TimeInSeconds.fiveMinutes
                 TimeInSeconds.oneWeek -> TimeInSeconds.oneHour
                 TimeInSeconds.twoWeeks -> TimeInSeconds.oneHour
                 TimeInSeconds.oneMonth -> TimeInSeconds.sixHours
                 TimeInSeconds.oneYear -> TimeInSeconds.oneDay
                 else -> TimeInSeconds.oneDay
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
