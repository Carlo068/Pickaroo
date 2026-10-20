package com.example.pikaroo.ui.orders.network

import com.example.pikaroo.common.mock.MockCollections.DRIVER_LOCATIONS
import com.example.pikaroo.common.mock.MockCollections.ORDERS
import com.example.pikaroo.common.mock.MockErrors
import com.example.pikaroo.common.mock.MockPickarooApi
import com.example.pikaroo.common.mock.MockStore
import com.example.pikaroo.ui.auth.model.Permission
import com.example.pikaroo.ui.auth.network.AuthenticatedUser
import com.example.pikaroo.ui.auth.network.MockAuthenticator
import com.example.pikaroo.ui.orders.model.*
import com.example.pikaroo.common.mock.MockCollections.SHOPS
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.model.OrderEventDto
import com.google.gson.Gson
import com.google.gson.JsonObject

/** Pedidos de un cliente sobre orders.seed.json: solo ve los suyos, como haría el servidor. */
class MockOrdersService(
    private val api: MockPickarooApi,
    private val authenticator: MockAuthenticator,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : OrdersApi {

    private val gson = Gson()

    override suspend fun create(bearer: String, request: CreateOrderRequest): OrderDto = api.call {
        val caller = authenticator.authenticate(this, bearer)
        if (!caller.can(Permission.ORDER_CREATE)) throw MockErrors.forbidden()
        synchronized(this) {
            // Mismo request_id + mismo usuario devuelve la orden ya guardada.
            all(ORDERS).firstOrNull {
                it.get("customer_id").asString == caller.id &&
                    it.get("request_id")?.asString == request.requestId
            }?.let { return@synchronized gson.fromJson(it, OrderDto::class.java) }
            if (request.requestId.isBlank() || request.items.isEmpty() || request.items.any {
                    it.productId.isBlank() || it.name.isBlank() || it.quantity <= 0 ||
                        !it.unitPrice.isFinite() || it.unitPrice < 0 ||
                        !(it.unitPrice * it.quantity).isFinite()
                }) throw MockErrors.badRequest("INVALID_ORDER")
            val shop = find(SHOPS, request.shopId) ?: throw MockErrors.notFound("SHOP_NOT_FOUND")
            if (request.fulfilment == Fulfilment.DELIVERY && request.address.isNullOrBlank()) {
                throw MockErrors.badRequest("ADDRESS_REQUIRED")
            }
            if (request.fulfilment == Fulfilment.DELIVERY && !shop.getAsJsonObject("delivery").get("enabled").asBoolean) {
                throw MockErrors.badRequest("DELIVERY_UNAVAILABLE")
            }
            fun money(amount: Double) = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toDouble()
            fun iso(millis: Long) = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date(millis))
            val lines = request.items.map { it.copy(discountPercentage = 0, lineTotal = money(it.unitPrice * it.quantity)) }
            val subtotal = money(lines.sumOf { it.lineTotal })
            val deliveryFee = if (request.fulfilment == Fulfilment.DELIVERY) shop.getAsJsonObject("delivery").get("fee").asDouble else 0.0
            val now = nowMillis()
            val order = OrderDto(
                id = "order_" + UUID.randomUUID(), shopId = request.shopId, customerId = caller.id,
                fulfilment = request.fulfilment, status = OrderStatus.PLACED, items = lines,
                subtotal = subtotal, deliveryFee = deliveryFee, total = money(subtotal + deliveryFee),
                pickup = if (request.fulfilment == Fulfilment.PICKUP) PickupDto(iso(now + 3_600_000), UUID.randomUUID().toString().take(6).uppercase(Locale.ROOT)) else null,
                delivery = if (request.fulfilment == Fulfilment.DELIVERY) DeliveryDto(address = request.address!!.trim(), driverId = null) else null,
                createdAt = iso(now), paymentStatus = "PENDING",
                shop = gson.fromJson(shop, ShopSummaryDto::class.java)
            )
            val document = gson.toJsonTree(order).asJsonObject.apply {
                addProperty("request_id", request.requestId)
                add("events", gson.toJsonTree(listOf(OrderEventDto(OrderStatus.PLACED, order.createdAt, caller.id))))
            }
            put(ORDERS, document)
            order
        }
    }

    override suspend fun listMine(bearer: String): List<OrderDto> = api.call {
        val caller = customer(bearer)
        all(ORDERS)
            .filter { it.get("customer_id").asString == caller.id }
            .sortedByDescending { it.get("created_at").asString }
            .map { gson.fromJson(orderWithShop(it), OrderDto::class.java) }
    }

    override suspend fun get(bearer: String, id: String): OrderDto = api.call {
        gson.fromJson(orderWithShop(ownOrder(bearer, id)), OrderDto::class.java)
    }

    override suspend fun events(bearer: String, id: String): List<OrderEventDto> = api.call {
        ownOrder(bearer, id).getAsJsonArray("events").map { gson.fromJson(it, OrderEventDto::class.java) }
    }

    override suspend fun driverLocation(bearer: String, id: String): DriverLocationDto = api.call {
        val order = ownOrder(bearer, id)
        val driverId = order.get("delivery")?.takeIf { it.isJsonObject }?.asJsonObject
            ?.get("driver_id")?.takeIf { !it.isJsonNull }?.asString
        val live = order.get("status").asString in setOf("DRIVER_ASSIGNED", "OUT_FOR_DELIVERY")
        val location = driverId?.takeIf { live }?.let { find(DRIVER_LOCATIONS, it) }
        if (location == null || location.get("order_id")?.asString != id) {
            throw MockErrors.notFound("NO_DRIVER_LOCATION", "The driver has not reported a location for this order")
        }
        gson.fromJson(location, DriverLocationDto::class.java)
    }

    private fun MockStore.customer(bearer: String): AuthenticatedUser {
        val caller = authenticator.authenticate(this, bearer)
        if (!caller.can(Permission.ORDER_READ_OWN)) throw MockErrors.forbidden()
        return caller
    }

    private fun MockStore.ownOrder(bearer: String, id: String): JsonObject {
        val caller = customer(bearer)
        val order = find(ORDERS, id) ?: throw MockErrors.notFound("ORDER_NOT_FOUND", "Order not found")
        if (order.get("customer_id").asString != caller.id) {
            throw MockErrors.forbidden("NOT_YOUR_ORDER", "This order belongs to another customer")
        }
        return order
    }
}
