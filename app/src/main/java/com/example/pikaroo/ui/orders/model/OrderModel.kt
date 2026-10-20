package com.example.pikaroo.ui.orders.model

import com.google.gson.annotations.SerializedName

enum class OrderStatus {
    PLACED, ACCEPTED, PREPARING, READY, PICKED_UP, DRIVER_ASSIGNED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED;

    val isFinal: Boolean get() = this == PICKED_UP || this == DELIVERED || this == CANCELLED
}

enum class Fulfilment {
    @SerializedName("pickup") PICKUP,
    @SerializedName("delivery") DELIVERY
}

data class OrderLineDto(
    @SerializedName("product_id") val productId: String,
    val name: String,
    val quantity: Int,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("discount_percentage") val discountPercentage: Int,
    @SerializedName("line_total") val lineTotal: Double
)

data class PickupDto(
    @SerializedName("slot_start") val slotStart: String,
    val code: String
)

data class DeliveryDto(
    val address: String,
    val lat: Double,
    val lng: Double,
    @SerializedName("driver_id") val driverId: String?
)

data class OrderDto(
    val id: String,
    @SerializedName("shop_id") val shopId: String,
    @SerializedName("customer_id") val customerId: String,
    val fulfilment: Fulfilment,
    val status: OrderStatus,
    val items: List<OrderLineDto>,
    val subtotal: Double,
    @SerializedName("delivery_fee") val deliveryFee: Double,
    val total: Double,
    val pickup: PickupDto?,
    val delivery: DeliveryDto?,
    @SerializedName("created_at") val createdAt: String
) {
    /** Número que ve el cliente: order_1004 -> 1004. */
    val number: String get() = id.removePrefix("order_")
}

data class OrderEventDto(
    val status: OrderStatus,
    val at: String,
    val by: String
)
