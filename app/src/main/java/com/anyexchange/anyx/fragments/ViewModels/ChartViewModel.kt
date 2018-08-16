package com.anyexchange.anyx.fragments.ViewModels

import android.arch.lifecycle.ViewModel
import com.anyexchange.anyx.classes.ChartStyle
import com.anyexchange.anyx.classes.Timespan
import com.anyexchange.anyx.classes.TradingPair

class ChartViewModel : ViewModel() {
    var timeSpan = Timespan.DAY
    var chartStyle = ChartStyle.Line
    var tradingPair: TradingPair? = null

}