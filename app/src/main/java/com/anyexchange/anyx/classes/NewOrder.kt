package com.anyexchange.anyx.classes

class NewOrder(val tradingPair: TradingPair, val price: Double?, val amount: Double,
               val funds: Double?, val type: TradeType, val side: TradeSide,
               val timeInForce: TimeInForce?, val cancelAfter: TimeInForce.CancelAfter?, val iceBergQty: String?) {

}