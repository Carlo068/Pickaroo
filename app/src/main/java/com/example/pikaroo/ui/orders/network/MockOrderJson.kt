package com.example.pikaroo.ui.orders.network

import com.example.pikaroo.common.mock.MockCollections.SHOPS
import com.example.pikaroo.common.mock.MockStore
import com.google.gson.JsonObject

/** El servidor une los datos básicos de la tienda al pedido. */
internal fun MockStore.orderWithShop(order: JsonObject): JsonObject {
    val shop = find(SHOPS, order.get("shop_id").asString) ?: return order
    return order.deepCopy().apply {
        add("shop", JsonObject().apply {
            listOf("id", "name", "address", "lat", "lng").forEach { add(it, shop.get(it)) }
        })
    }
}
