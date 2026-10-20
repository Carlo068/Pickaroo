package com.example.pikaroo.ui.driver.model

import com.example.pikaroo.ui.orders.model.DeliveryDto
import com.example.pikaroo.ui.orders.model.ShopSummaryDto
import com.google.gson.annotations.SerializedName

/** Pedido listo para entregar que cualquier repartidor puede tomar. */
data class AvailableOrderDto(
    val id: String,
    val shop: ShopSummaryDto,
    val delivery: DeliveryDto,
    @SerializedName("delivery_fee") val deliveryFee: Double,
    @SerializedName("items_count") val itemsCount: Int,
    @SerializedName("created_at") val createdAt: String
) {
    val number: String get() = id.removePrefix("order_")
}
