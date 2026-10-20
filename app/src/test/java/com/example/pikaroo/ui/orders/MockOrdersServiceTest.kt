package com.example.pikaroo.ui.orders

import com.example.pikaroo.common.mock.FileSeedSource
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
}
