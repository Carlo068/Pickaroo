package com.example.pikaroo.ui.driver.network

import com.example.pikaroo.common.mock.MockCollections.DRIVER_LOCATIONS
import com.example.pikaroo.common.mock.MockCollections.ORDERS
import com.example.pikaroo.common.mock.MockErrors
import com.example.pikaroo.common.mock.MockPickarooApi
import com.example.pikaroo.common.mock.MockStore
import com.example.pikaroo.common.time.formatIsoUtc
import com.example.pikaroo.ui.auth.model.Permission
import com.example.pikaroo.ui.auth.network.AuthenticatedUser
import com.example.pikaroo.ui.auth.network.MockAuthenticator
import com.example.pikaroo.ui.driver.model.AvailableOrderDto
import com.example.pikaroo.ui.driver.model.DriverLocationRequest
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.network.orderWithShop
import com.google.gson.Gson
import com.google.gson.JsonObject

/** Entregas sobre orders.seed.json; los cambios se guardan, así cliente y repartidor ven lo mismo. */
class MockDriverService(
    private val api: MockPickarooApi,
    private val authenticator: MockAuthenticator,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 }
) : DriverApi {

    private val gson = Gson()

    override suspend fun available(bearer: String): List<AvailableOrderDto> = api.call {
        driver(bearer, Permission.DELIVERY_READ_AVAILABLE)
        all(ORDERS)
            .filter { it.isAvailableDelivery() }
            .sortedBy { it.get("created_at").asString }
            .map { toAvailable(it) }
    }

    override suspend fun current(bearer: String): OrderDto = api.call {
        val driver = driver(bearer, Permission.DELIVERY_READ_AVAILABLE)
        val order = activeOrderOf(driver.id)
            ?: throw MockErrors.notFound("NO_ACTIVE_DELIVERY", "You have no delivery in progress")
        gson.fromJson(orderWithShop(order), OrderDto::class.java)
    }

    override suspend fun accept(bearer: String, id: String): OrderDto = api.call {
        val driver = driver(bearer, Permission.DELIVERY_ACCEPT)
        val order = find(ORDERS, id) ?: throw MockErrors.notFound("ORDER_NOT_FOUND", "Order not found")
        if (!order.isAvailableDelivery()) {
            throw MockErrors.conflict("ORDER_NOT_AVAILABLE", "Another driver already took this order")
        }
        if (activeOrderOf(driver.id) != null) {
            throw MockErrors.conflict("ALREADY_DELIVERING", "Finish your current delivery first")
        }

        order.addProperty("status", "DRIVER_ASSIGNED")
        order.getAsJsonObject("delivery").addProperty("driver_id", driver.id)
        order.getAsJsonArray("events").add(JsonObject().apply {
            addProperty("status", "DRIVER_ASSIGNED")
            addProperty("at", formatIsoUtc(nowSeconds()))
            addProperty("by", driver.id)
        })
        put(ORDERS, order)
        gson.fromJson(orderWithShop(order), OrderDto::class.java)
    }

    override suspend fun pickup(bearer: String, id: String): OrderDto =
        advance(bearer, id, from = "DRIVER_ASSIGNED", to = "OUT_FOR_DELIVERY")

    override suspend fun deliver(bearer: String, id: String): OrderDto =
        advance(bearer, id, from = "OUT_FOR_DELIVERY", to = "DELIVERED")

    override suspend fun reportLocation(bearer: String, body: DriverLocationRequest) {
        api.call {
            val driver = driver(bearer, Permission.LOCATION_WRITE)
            if (body.lat !in -90.0..90.0 || body.lng !in -180.0..180.0) {
                throw MockErrors.badRequest("INVALID_LOCATION", "Latitude or longitude is out of range")
            }
            val order = find(ORDERS, body.orderId) ?: throw MockErrors.notFound("ORDER_NOT_FOUND", "Order not found")
            if (order.driverId() != driver.id) {
                throw MockErrors.forbidden("NOT_YOUR_DELIVERY", "This delivery belongs to another driver")
            }
            if (order.get("status").asString !in setOf("DRIVER_ASSIGNED", "OUT_FOR_DELIVERY")) {
                throw MockErrors.conflict("INVALID_STATUS", "The delivery is not in progress")
            }
            put(DRIVER_LOCATIONS, JsonObject().apply {
                addProperty("id", driver.id)
                addProperty("order_id", body.orderId)
                addProperty("lat", body.lat)
                addProperty("lng", body.lng)
                addProperty("updated_at", formatIsoUtc(nowSeconds()))
            })
        }
    }

    /** Solo el repartidor asignado puede avanzar su entrega, y solo desde el estado anterior. */
    private suspend fun advance(bearer: String, id: String, from: String, to: String): OrderDto = api.call {
        val driver = driver(bearer, Permission.DELIVERY_UPDATE)
        val order = find(ORDERS, id) ?: throw MockErrors.notFound("ORDER_NOT_FOUND", "Order not found")
        if (order.driverId() != driver.id) {
            throw MockErrors.forbidden("NOT_YOUR_DELIVERY", "This delivery belongs to another driver")
        }
        if (order.get("status").asString != from) {
            throw MockErrors.conflict("INVALID_STATUS", "The order is not in the right state for this action")
        }

        order.addProperty("status", to)
        order.getAsJsonArray("events").add(JsonObject().apply {
            addProperty("status", to)
            addProperty("at", formatIsoUtc(nowSeconds()))
            addProperty("by", driver.id)
        })
        put(ORDERS, order)
        gson.fromJson(orderWithShop(order), OrderDto::class.java)
    }

    private fun MockStore.driver(bearer: String, permission: String): AuthenticatedUser {
        val caller = authenticator.authenticate(this, bearer)
        if (!caller.can(permission)) throw MockErrors.forbidden()
        return caller
    }

    private fun JsonObject.deliveryBlock(): JsonObject? =
        get("delivery")?.takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonObject.driverId(): String? =
        deliveryBlock()?.get("driver_id")?.takeIf { !it.isJsonNull }?.asString

    private fun JsonObject.isAvailableDelivery(): Boolean =
        get("fulfilment").asString == "delivery" && get("status").asString == "READY" && driverId() == null

    private fun MockStore.activeOrderOf(driverId: String): JsonObject? = all(ORDERS).firstOrNull {
        it.driverId() == driverId && it.get("status").asString in setOf("DRIVER_ASSIGNED", "OUT_FOR_DELIVERY")
    }

    private fun MockStore.toAvailable(order: JsonObject): AvailableOrderDto {
        val dto = gson.fromJson(orderWithShop(order), OrderDto::class.java)
        return AvailableOrderDto(
            id = dto.id,
            shop = dto.shop!!,
            delivery = dto.delivery!!,
            deliveryFee = dto.deliveryFee,
            itemsCount = dto.items.sumOf { it.quantity },
            createdAt = dto.createdAt
        )
    }
}
