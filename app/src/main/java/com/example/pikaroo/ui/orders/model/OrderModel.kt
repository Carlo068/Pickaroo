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
    val lat: Double? = null,
    val lng: Double? = null,
    @SerializedName("driver_id") val driverId: String?
)

data class ShopSummaryDto(
    val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double
)

data class DriverLocationDto(
    val lat: Double,
    val lng: Double,
    @SerializedName("updated_at") val updatedAt: String
)

data class MapPoint(val lat: Double, val lng: Double)

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
    @SerializedName("created_at") val createdAt: String,
    val shop: ShopSummaryDto? = null,
    @SerializedName("payment_status") val paymentStatus: String? = null
) {
    /** Número que ve el cliente: order_1004 -> 1004. */
    val number: String get() = id.removePrefix("order_")

    /** Entrega con repartidor en camino: el cliente puede ver su ubicación. */
    val tracksDriver: Boolean
        get() = fulfilment == Fulfilment.DELIVERY &&
            (status == OrderStatus.DRIVER_ASSIGNED || status == OrderStatus.OUT_FOR_DELIVERY)
}

data class OrderEventDto(
    val status: OrderStatus,
    val at: String,
    val by: String
)

/** Snapshot del carrito; requestId se conserva al reintentar para no duplicar la orden. */
data class CreateOrderRequest(
    @SerializedName("request_id") val requestId: String,
    @SerializedName("shop_id") val shopId: String,
    val fulfilment: Fulfilment,
    val items: List<OrderLineDto>,
    val address: String? = null
)
