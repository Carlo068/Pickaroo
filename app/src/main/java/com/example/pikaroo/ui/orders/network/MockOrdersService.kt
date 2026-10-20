package com.example.pikaroo.ui.orders.network

import com.example.pikaroo.common.mock.MockCollections.DRIVER_LOCATIONS
import com.example.pikaroo.common.mock.MockCollections.ORDERS
import com.example.pikaroo.common.mock.MockCollections.SHOPS
import com.example.pikaroo.common.mock.MockErrors
import com.example.pikaroo.common.mock.MockPickarooApi
import com.example.pikaroo.common.mock.MockStore
import com.example.pikaroo.ui.auth.model.Permission
import com.example.pikaroo.ui.auth.network.AuthenticatedUser
import com.example.pikaroo.ui.auth.network.MockAuthenticator
import com.example.pikaroo.ui.orders.model.DriverLocationDto
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.model.OrderEventDto
import com.google.gson.Gson
import com.google.gson.JsonObject

/** Pedidos de un cliente sobre orders.seed.json: solo ve los suyos, como haría el servidor. */
class MockOrdersService(
    private val api: MockPickarooApi,
    private val authenticator: MockAuthenticator
) : OrdersApi {

    private val gson = Gson()

    override suspend fun listMine(bearer: String): List<OrderDto> = api.call {
        val caller = customer(bearer)
        all(ORDERS)
            .filter { it.get("customer_id").asString == caller.id }
            .sortedByDescending { it.get("created_at").asString }
            .map { gson.fromJson(withShop(it), OrderDto::class.java) }
    }

    override suspend fun get(bearer: String, id: String): OrderDto = api.call {
        gson.fromJson(withShop(ownOrder(bearer, id)), OrderDto::class.java)
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

    /** El servidor une los datos básicos de la tienda al pedido. */
    private fun MockStore.withShop(order: JsonObject): JsonObject {
        val shop = find(SHOPS, order.get("shop_id").asString) ?: return order
        return order.deepCopy().apply {
            add("shop", JsonObject().apply {
                listOf("id", "name", "address", "lat", "lng").forEach { add(it, shop.get(it)) }
            })
        }
    }
}
