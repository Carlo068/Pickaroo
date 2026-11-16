package com.example.pikaroo.ui.shop.model

import com.example.pikaroo.ui.orders.model.Fulfilment
import com.example.pikaroo.ui.orders.model.OrderStatus

/** La tienda prepara ambos tipos; únicamente cierra la recogida del cliente. */
fun nextShopStatus(status: OrderStatus, fulfilment: Fulfilment): OrderStatus? = when (status) {
    OrderStatus.PLACED -> OrderStatus.ACCEPTED
    OrderStatus.ACCEPTED -> OrderStatus.PREPARING
    OrderStatus.PREPARING -> OrderStatus.READY
    OrderStatus.READY -> if (fulfilment == Fulfilment.PICKUP) OrderStatus.PICKED_UP else null
    else -> null
}

data class ShopStatusRequest(
    val expected: OrderStatus,
    val status: OrderStatus,
    @com.google.gson.annotations.SerializedName("pickup_code") val pickupCode: String? = null
)
