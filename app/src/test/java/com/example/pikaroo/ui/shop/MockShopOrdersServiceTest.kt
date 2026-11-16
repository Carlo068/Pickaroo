package com.example.pikaroo.ui.shop

import com.example.pikaroo.common.mock.*
import com.example.pikaroo.common.mock.MockCollections.ORDERS
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.auth.network.*
import com.example.pikaroo.ui.orders.model.*
import com.example.pikaroo.ui.orders.network.MockOrdersService
import com.example.pikaroo.ui.shop.model.*
import com.example.pikaroo.ui.shop.network.MockShopOrdersService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException

class MockShopOrdersServiceTest {
    private var now = 1_000_000L
    private val storage = InMemoryStringStore()
    private val store = MockStore(FileSeedSource(), storage)
    private val api = MockPickarooApi(store, latencyMillis = { 0 })
    private val auth = MockAuthService(api, nowSeconds = { now })
    private val authenticator = MockAuthenticator { now }
    private val shop = MockShopOrdersService(api, authenticator) { now }
    private suspend fun token(user: String) = "Bearer " + auth.login(LoginRequest(user, "demo1234")).access
    private suspend fun error(status: Int, code: String? = null, action: suspend () -> Unit) {
        try { action(); fail("Expected HTTP $status") } catch (e: HttpException) {
            assertEquals(status, e.code()); if (code != null) assertEquals(code, e.toApiError()?.code)
        }
    }
    private fun setOrder(id: String, status: OrderStatus, fulfilment: Fulfilment) {
        val doc = store.find(ORDERS, id)!!
        doc.addProperty("status", status.name)
        doc.addProperty("fulfilment", if (fulfilment == Fulfilment.PICKUP) "pickup" else "delivery")
        store.put(ORDERS, doc)
    }

    @Test fun `staff and owners see only their branch`() = runTest {
        for ((user, id) in listOf("staff1" to "shop_1", "owner1" to "shop_1", "owner2" to "shop_2")) {
            val list = shop.orders(token(user))
            assertTrue(list.isNotEmpty()); assertTrue(list.all { it.shopId == id })
            assertTrue(list.all { it.shop?.id == id })
            assertEquals(list.map { it.createdAt }.sorted(), list.map { it.createdAt })
        }
    }

    @Test fun `every invalid transition is rejected without changing history`() = runTest {
        val bearer = token("staff1")
        for (mode in Fulfilment.entries) for (from in OrderStatus.entries) for (to in OrderStatus.entries) {
            setOrder("order_1001", from, mode)
            val before = store.find(ORDERS, "order_1001")!!
            val allowed = setOf(
                OrderStatus.PLACED to OrderStatus.ACCEPTED,
                OrderStatus.ACCEPTED to OrderStatus.PREPARING,
                OrderStatus.PREPARING to OrderStatus.READY
            ) + if (mode == Fulfilment.PICKUP) setOf(OrderStatus.READY to OrderStatus.PICKED_UP) else emptySet()
            if ((from to to) in allowed) {
                val result = shop.update(bearer, "order_1001", ShopStatusRequest(from, to, before.getAsJsonObject("pickup")?.get("code")?.asString))
                assertEquals(to, result.status)
                assertEquals(before.getAsJsonArray("events").size() + 1, store.find(ORDERS, "order_1001")!!.getAsJsonArray("events").size())
            } else {
                error(409, "INVALID_STATUS") { shop.update(bearer, "order_1001", ShopStatusRequest(from, to, before.getAsJsonObject("pickup")?.get("code")?.asString)) }
                assertEquals(before, store.find(ORDERS, "order_1001"))
            }
        }
    }

    @Test fun `wrong missing and blank pickup codes preserve status and events`() = runTest {
        setOrder("order_1001", OrderStatus.READY, Fulfilment.PICKUP)
        val bearer = token("staff1")
        val before = store.find(ORDERS, "order_1001")!!
        for (code in listOf(null, "", "  ", "WRONG", "order_1001")) {
            error(400, "INVALID_PICKUP_CODE") {
                shop.update(bearer, "order_1001", ShopStatusRequest(OrderStatus.READY, OrderStatus.PICKED_UP, code))
            }
            assertEquals(before, store.find(ORDERS, "order_1001"))
        }
    }

    @Test fun `valid pickup code accepts lowercase and spaces and cannot be reused`() = runTest {
        setOrder("order_1001", OrderStatus.READY, Fulfilment.PICKUP)
        val bearer = token("staff1")
        val before = store.find(ORDERS, "order_1001")!!
        val code = before.getAsJsonObject("pickup").get("code").asString
        val request = ShopStatusRequest(OrderStatus.READY, OrderStatus.PICKED_UP, "  " + code.lowercase(java.util.Locale.ROOT) + "  ")
        assertEquals(OrderStatus.PICKED_UP, shop.update(bearer, "order_1001", request).status)
        assertEquals(before.getAsJsonArray("events").size() + 1, store.find(ORDERS, "order_1001")!!.getAsJsonArray("events").size())
        error(409, "INVALID_STATUS") { shop.update(bearer, "order_1001", request) }
    }

    @Test fun `stale or repeated actions do not advance twice`() = runTest {
        setOrder("order_1001", OrderStatus.PLACED, Fulfilment.PICKUP)
        val bearer = token("staff1")
        val request = ShopStatusRequest(OrderStatus.PLACED, OrderStatus.ACCEPTED)
        shop.update(bearer, "order_1001", request)
        error(409) { shop.update(bearer, "order_1001", request) }
    }

    @Test fun `changed status persists and customer sees the same event`() = runTest {
        setOrder("order_1001", OrderStatus.PLACED, Fulfilment.PICKUP)
        shop.update(token("staff1"), "order_1001", ShopStatusRequest(OrderStatus.PLACED, OrderStatus.ACCEPTED))
        val reopenedApi = MockPickarooApi(MockStore(FileSeedSource(), storage), latencyMillis = { 0 })
        val customer = MockOrdersService(reopenedApi, authenticator)
        assertEquals(OrderStatus.ACCEPTED, customer.get(token("customer1"), "order_1001").status)
        val event = customer.events(token("customer1"), "order_1001").last()
        assertEquals(OrderStatus.ACCEPTED, event.status); assertEquals("user_2", event.by)
    }

    @Test fun `customer driver and another shop cannot update orders`() = runTest {
        for (user in listOf("customer1", "driver1")) {
            val bearer = token(user)
            error(403) { shop.orders(bearer) }
            error(403) { shop.update(bearer, "order_1001", ShopStatusRequest(OrderStatus.PLACED, OrderStatus.ACCEPTED)) }
        }
        error(403, "NOT_YOUR_SHOP") { shop.update(token("owner2"), "order_1001", ShopStatusRequest(OrderStatus.PLACED, OrderStatus.ACCEPTED)) }
        error(404, "ORDER_NOT_FOUND") { shop.update(token("staff1"), "missing", ShopStatusRequest(OrderStatus.PLACED, OrderStatus.ACCEPTED)) }
    }

    @Test fun `missing invalid and expired tokens are rejected on both endpoints`() = runTest {
        val bearer = token("staff1"); now += 15 * 60
        for (bad in listOf("", "Bearer garbage", bearer)) {
            error(401) { shop.orders(bad) }
            error(401) { shop.update(bad, "order_1001", ShopStatusRequest(OrderStatus.PLACED, OrderStatus.ACCEPTED)) }
        }
    }
}
