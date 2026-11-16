package com.example.pikaroo.ui.shop.network

import com.example.pikaroo.common.mock.MockCollections.ORDERS
import com.example.pikaroo.common.mock.MockErrors
import com.example.pikaroo.common.mock.MockPickarooApi
import com.example.pikaroo.common.mock.MockStore
import com.example.pikaroo.common.time.formatIsoUtc
import com.example.pikaroo.ui.auth.model.Permission
import com.example.pikaroo.ui.auth.network.MockAuthenticator
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.model.OrderEventDto
import com.example.pikaroo.ui.orders.network.orderWithShop
import com.example.pikaroo.ui.shop.model.ShopStatusRequest
import com.example.pikaroo.ui.shop.model.nextShopStatus
import com.google.gson.Gson

class MockShopOrdersService(
    private val api: MockPickarooApi,
    private val authenticator: MockAuthenticator,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 }
) : ShopOrdersApi {
    private val gson = Gson()

    override suspend fun orders(bearer: String): List<OrderDto> = api.call {
        val (_, shops) = shopCaller(bearer, Permission.SHOP_ORDERS_READ)
        all(ORDERS).filter { it.get("shop_id").asString in shops }
            .sortedBy { it.get("created_at").asString }
            .map { gson.fromJson(orderWithShop(it), OrderDto::class.java) }
    }

    override suspend fun update(bearer: String, id: String, request: ShopStatusRequest): OrderDto = api.call {
        val (callerId, shops) = shopCaller(bearer, Permission.ORDER_STATUS_UPDATE)
        synchronized(this) {
            val document = find(ORDERS, id) ?: throw MockErrors.notFound("ORDER_NOT_FOUND")
            if (document.get("shop_id").asString !in shops) throw MockErrors.forbidden("NOT_YOUR_SHOP")
            val order = gson.fromJson(document, OrderDto::class.java)
            if (order.status != request.expected || nextShopStatus(order.status, order.fulfilment) != request.status) {
                throw MockErrors.conflict("INVALID_STATUS", "El pedido cambió o la transición no está permitida")
            }
            document.addProperty("status", request.status.name)
            document.getAsJsonArray("events").add(gson.toJsonTree(OrderEventDto(request.status, formatIsoUtc(nowSeconds()), callerId)))
            put(ORDERS, document)
            gson.fromJson(orderWithShop(document), OrderDto::class.java)
        }
    }

    private fun MockStore.shopCaller(bearer: String, permission: String): Pair<String, Set<String>> {
        val caller = authenticator.authenticate(this, bearer)
        if (!caller.can(permission)) throw MockErrors.forbidden()
        val shops = caller.roles.filter { it.role in setOf("staff", "owner") }.mapNotNull { it.shopId }.toSet()
        if (shops.isEmpty()) throw MockErrors.forbidden()
        return caller.id to shops
    }
}
