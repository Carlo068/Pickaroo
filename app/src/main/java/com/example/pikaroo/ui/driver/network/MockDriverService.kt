package com.example.pikaroo.ui.driver.network

import com.example.pikaroo.common.mock.MockCollections.ORDERS
import com.example.pikaroo.common.mock.MockErrors
import com.example.pikaroo.common.mock.MockPickarooApi
import com.example.pikaroo.common.mock.MockStore
import com.example.pikaroo.common.time.formatIsoUtc
import com.example.pikaroo.ui.auth.model.Permission
import com.example.pikaroo.ui.auth.network.AuthenticatedUser
import com.example.pikaroo.ui.auth.network.MockAuthenticator
import com.example.pikaroo.ui.driver.model.AvailableOrderDto
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
