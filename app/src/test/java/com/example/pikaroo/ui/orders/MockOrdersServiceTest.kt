package com.example.pikaroo.ui.orders

import com.example.pikaroo.common.mock.FileSeedSource
import com.example.pikaroo.common.mock.MockCollections.DRIVER_LOCATIONS
import com.example.pikaroo.common.mock.InMemoryStringStore
import com.example.pikaroo.common.mock.MockPickarooApi
import com.example.pikaroo.common.mock.MockStore
import com.example.pikaroo.common.mock.toApiError
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.auth.network.MockAuthService
import com.example.pikaroo.ui.auth.network.MockAuthenticator
import com.example.pikaroo.ui.orders.model.Fulfilment
import com.example.pikaroo.ui.orders.model.OrderStatus
import com.example.pikaroo.ui.orders.network.MockOrdersService
import com.google.gson.JsonObject
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException

class MockOrdersServiceTest {

    private var now = 1_000_000L
    private val mockApi = MockPickarooApi(MockStore(FileSeedSource(), InMemoryStringStore()), latencyMillis = { 0L })
    private val auth = MockAuthService(mockApi, nowSeconds = { now })
    private val orders = MockOrdersService(mockApi, MockAuthenticator { now })

    private suspend fun bearer(username: String) = "Bearer " + auth.login(LoginRequest(username, "demo1234")).access

    private suspend fun expectError(status: Int, code: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: HttpException) {
            assertEquals(status, e.code())
            assertEquals(code, e.toApiError()!!.code)
            return
        }
        throw AssertionError("Expected HTTP $status $code")
    }

    @Test
    fun `a customer lists only their own orders, newest first`() = runTest {
        val list = orders.listMine(bearer("customer1"))

        assertEquals(
            listOf("order_1010", "order_1006", "order_1004", "order_1003", "order_1008", "order_1001").sortedByDescending {
                list.first { o -> o.id == it }.createdAt
            },
            list.map { it.id }
        )
        assertTrue(list.all { it.customerId == "user_1" })
        assertEquals(6, list.size)
    }

    @Test
    fun `an owner who also shops sees their own customer orders`() = runTest {
        val list = orders.listMine(bearer("owner1"))

        assertEquals(setOf("order_1002", "order_1005", "order_1007", "order_1009"), list.map { it.id }.toSet())
    }

    @Test
    fun `orders are mapped with items, pickup code and delivery details`() = runTest {
        val bearer = bearer("customer1")

        val pickup = orders.get(bearer, "order_1004")
        val delivery = orders.get(bearer, "order_1008")

        assertEquals(Fulfilment.PICKUP, pickup.fulfilment)
        assertEquals(OrderStatus.PREPARING, pickup.status)
        assertEquals("H6Z2LC", pickup.pickup!!.code)
        assertNull(pickup.delivery)
        assertEquals(3, pickup.items.size)
        assertEquals("1004", pickup.number)

        assertEquals(Fulfilment.DELIVERY, delivery.fulfilment)
        assertEquals("user_5", delivery.delivery!!.driverId)
        assertEquals(2.99, delivery.deliveryFee, 0.0001)
        assertNull(delivery.pickup)
    }

    @Test
    fun `events come back in the order they happened`() = runTest {
        val events = orders.events(bearer("customer1"), "order_1008")

        assertEquals(
            listOf(
                OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY,
                OrderStatus.DRIVER_ASSIGNED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED
            ),
            events.map { it.status }
        )
        assertEquals("user_1", events.first().by)
        assertEquals("user_5", events.last().by)
    }

    @Test
    fun `a customer cannot read someone else's order`() = runTest {
        val bearer = bearer("customer1")

        expectError(403, "NOT_YOUR_ORDER") { orders.get(bearer, "order_1002") }
        expectError(403, "NOT_YOUR_ORDER") { orders.events(bearer, "order_1002") }
    }

    @Test
    fun `an unknown order is not found`() = runTest {
        expectError(404, "ORDER_NOT_FOUND") { orders.get(bearer("customer1"), "order_9999") }
    }

    @Test
    fun `staff and drivers cannot use the customer endpoints`() = runTest {
        for (username in listOf("staff1", "driver1")) {
            val bearer = bearer(username)
            expectError(403, "FORBIDDEN") { orders.listMine(bearer) }
            expectError(403, "FORBIDDEN") { orders.get(bearer, "order_1001") }
            expectError(403, "FORBIDDEN") { orders.events(bearer, "order_1001") }
        }
    }

    @Test
    fun `requests need a valid access token`() = runTest {
        val bearer = bearer("customer1")

        expectError(401, "UNAUTHORIZED") { orders.listMine("no token") }
        expectError(401, "INVALID_TOKEN") { orders.listMine("Bearer garbage") }

        now += 15 * 60
        expectError(401, "TOKEN_EXPIRED") { orders.listMine(bearer) }
    }

    private fun location(order: String, lat: Double, lng: Double, at: String) = JsonObject().apply {
        addProperty("id", "user_5")
        addProperty("order_id", order)
        addProperty("lat", lat)
        addProperty("lng", lng)
        addProperty("updated_at", at)
    }

    @Test
    fun `orders include the basic details of their shop`() = runTest {
        val order = orders.get(bearer("customer1"), "order_1004")

        assertEquals("shop_1", order.shop!!.id)
        assertEquals("Pickaroo Centro", order.shop!!.name)
        assertEquals(19.4326, order.shop!!.lat, 0.0001)
        assertEquals(-99.1332, order.shop!!.lng, 0.0001)
    }

    @Test
    fun `the driver location of an order on the way is returned`() = runTest {
        val location = orders.driverLocation(bearer("owner1"), "order_1009")

        assertEquals(19.423, location.lat, 0.0001)
        assertEquals(-99.142, location.lng, 0.0001)
        assertEquals("2026-10-20T15:05:00Z", location.updatedAt)
    }

    @Test
    fun `the driver location follows the driver as they report new positions`() = runTest {
        mockApi.call { put(DRIVER_LOCATIONS, location("order_1009", 19.4201, -99.1433, "2026-10-20T15:07:00Z")) }

        val location = orders.driverLocation(bearer("owner1"), "order_1009")

        assertEquals(19.4201, location.lat, 0.0001)
        assertEquals("2026-10-20T15:07:00Z", location.updatedAt)
    }

    @Test
    fun `there is no driver location unless a driver is on the way`() = runTest {
        val bearer = bearer("customer1")

        expectError(404, "NO_DRIVER_LOCATION") { orders.driverLocation(bearer, "order_1004") }
        expectError(404, "NO_DRIVER_LOCATION") { orders.driverLocation(bearer, "order_1010") }
        expectError(404, "NO_DRIVER_LOCATION") { orders.driverLocation(bearer, "order_1008") }
    }

    @Test
    fun `a location the driver reported for another order is not used`() = runTest {
        mockApi.call { put(DRIVER_LOCATIONS, location("order_9999", 1.0, 1.0, "2026-10-20T15:07:00Z")) }

        expectError(404, "NO_DRIVER_LOCATION") { orders.driverLocation(bearer("owner1"), "order_1009") }
    }

    @Test
    fun `only the customer of the order can see the driver location`() = runTest {
        expectError(403, "NOT_YOUR_ORDER") { orders.driverLocation(bearer("customer1"), "order_1009") }
        expectError(403, "FORBIDDEN") { orders.driverLocation(bearer("driver1"), "order_1009") }
        expectError(401, "UNAUTHORIZED") { orders.driverLocation("no token", "order_1009") }
    }
}
