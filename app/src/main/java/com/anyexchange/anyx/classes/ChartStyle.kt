package com.anyexchange.anyx.classes

enum class ChartStyle {
    Line,
    Candle;

    override fun toString(): String {
        return when (this) {
            Line -> "Line"
            Candle -> "Candle"
        }
    }
    companion object {
        fun forString(string: String) : ChartStyle{
            return when (string) {
                Line.toString() -> Line
                Candle.toString() -> Candle
                else -> Line
            }
        }
    }
}