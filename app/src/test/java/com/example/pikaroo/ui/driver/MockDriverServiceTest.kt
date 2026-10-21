package com.example.pikaroo.ui.driver

import com.example.pikaroo.common.mock.FileSeedSource
import com.example.pikaroo.common.mock.InMemoryStringStore
import com.example.pikaroo.common.mock.MockCollections.ORDERS
import com.example.pikaroo.common.mock.MockPickarooApi
import com.example.pikaroo.common.mock.MockStore
import com.example.pikaroo.common.mock.toApiError
import com.example.pikaroo.common.time.formatIsoUtc
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.auth.network.MockAuthService
import com.example.pikaroo.ui.auth.network.MockAuthenticator
import com.example.pikaroo.ui.driver.network.MockDriverService
import com.example.pikaroo.ui.orders.model.OrderStatus
import com.example.pikaroo.ui.orders.network.MockOrdersService
import com.google.gson.JsonObject
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException

class MockDriverServiceTest {

    private val now = 1_790_000_000L
    private val mockApi = MockPickarooApi(MockStore(FileSeedSource(), InMemoryStringStore()), latencyMillis = { 0L })
    private val authenticator = MockAuthenticator { now }
    private val auth = MockAuthService(mockApi, nowSeconds = { now })
    private val driver = MockDriverService(mockApi, authenticator, nowSeconds = { now })
    private val orders = MockOrdersService(mockApi, authenticator)

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

    /** Edita un pedido del almacén como lo haría otra parte del sistema (tienda, otro repartidor). */
    private suspend fun change(orderId: String, edit: JsonObject.() -> Unit) {
        mockApi.call {
            val order = find(ORDERS, orderId)!!
            order.edit()
            put(ORDERS, order)
        }
    }

    private suspend fun finishCurrentDelivery() = change("order_1009") { addProperty("status", "DELIVERED") }

    @Test
    fun `available lists ready deliveries nobody has taken`() = runTest {
        val list = driver.available(bearer("driver1"))

        assertEquals(listOf("order_1010"), list.map { it.id })
        val order = list.single()
        assertEquals("Panadería La Espiga", order.shop.name)
        assertEquals("Calle Falsa 9, Col. Roma Norte", order.delivery.address)
        assertEquals(1.5, order.deliveryFee, 0.0001)
        assertEquals(3, order.itemsCount)
        assertEquals("1010", order.number)
    }

    @Test
    fun `available leaves out pickup orders and deliveries already assigned`() = runTest {
        val ids = driver.available(bearer("driver1")).map { it.id }

        assertTrue("order_1003" !in ids)
        assertTrue("order_1009" !in ids)
    }

    @Test
    fun `available shows the oldest order first`() = runTest {
        change("order_1004") {
            addProperty("fulfilment", "delivery")
            addProperty("status", "READY")
            add("delivery", JsonObject().apply {
                addProperty("address", "Calle Vieja 1")
                addProperty("lat", 19.42)
                addProperty("lng", -99.14)
                add("driver_id", com.google.gson.JsonNull.INSTANCE)
            })
        }

        val ids = driver.available(bearer("driver1")).map { it.id }

        assertEquals(listOf("order_1004", "order_1010"), ids)
    }

    @Test
    fun `current is the delivery the driver is carrying`() = runTest {
        val current = driver.current(bearer("driver1"))

        assertEquals("order_1009", current.id)
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, current.status)
        assertEquals("Pickaroo Centro", current.shop!!.name)
    }

    @Test
    fun `current is not found when the driver carries nothing`() = runTest {
        finishCurrentDelivery()

        expectError(404, "NO_ACTIVE_DELIVERY") { driver.current(bearer("driver1")) }
    }

    @Test
    fun `accepting assigns the order to the driver and records the event`() = runTest {
        finishCurrentDelivery()
        val bearer = bearer("driver1")

        val accepted = driver.accept(bearer, "order_1010")

        assertEquals(OrderStatus.DRIVER_ASSIGNED, accepted.status)
        assertEquals("user_5", accepted.delivery!!.driverId)
        assertEquals("Panadería La Espiga", accepted.shop!!.name)

        val events = orders.events(bearer("customer1"), "order_1010")
        assertEquals(OrderStatus.DRIVER_ASSIGNED, events.last().status)
        assertEquals("user_5", events.last().by)
        assertEquals(formatIsoUtc(now), events.last().at)
    }

    @Test
    fun `an accepted order leaves the list and becomes the current delivery`() = runTest {
        finishCurrentDelivery()
        val bearer = bearer("driver1")

        driver.accept(bearer, "order_1010")

        assertTrue(driver.available(bearer).isEmpty())
        assertEquals("order_1010", driver.current(bearer).id)
    }

    @Test
    fun `the customer sees the new status after a driver accepts`() = runTest {
        finishCurrentDelivery()
        driver.accept(bearer("driver1"), "order_1010")

        val order = orders.get(bearer("customer1"), "order_1010")

        assertEquals(OrderStatus.DRIVER_ASSIGNED, order.status)
    }

    @Test
    fun `a driver cannot take a second delivery while carrying one`() = runTest {
        expectError(409, "ALREADY_DELIVERING") { driver.accept(bearer("driver1"), "order_1010") }
    }

    @Test
    fun `an order another driver took first is a conflict`() = runTest {
        finishCurrentDelivery()
        change("order_1010") {
            addProperty("status", "DRIVER_ASSIGNED")
            getAsJsonObject("delivery").addProperty("driver_id", "user_99")
        }

        expectError(409, "ORDER_NOT_AVAILABLE") { driver.accept(bearer("driver1"), "order_1010") }
    }

    @Test
    fun `accepting twice is a conflict the second time`() = runTest {
        finishCurrentDelivery()
        val bearer = bearer("driver1")
        driver.accept(bearer, "order_1010")

        expectError(409, "ORDER_NOT_AVAILABLE") { driver.accept(bearer, "order_1010") }
    }

    @Test
    fun `orders that are not ready deliveries cannot be accepted`() = runTest {
        finishCurrentDelivery()
        val bearer = bearer("driver1")

        expectError(409, "ORDER_NOT_AVAILABLE") { driver.accept(bearer, "order_1004") }
        expectError(409, "ORDER_NOT_AVAILABLE") { driver.accept(bearer, "order_1008") }
        expectError(404, "ORDER_NOT_FOUND") { driver.accept(bearer, "order_9999") }
    }

    @Test
    fun `customers and shop staff cannot use the driver endpoints`() = runTest {
        for (username in listOf("customer1", "staff1", "owner1")) {
            val bearer = bearer(username)
            expectError(403, "FORBIDDEN") { driver.available(bearer) }
            expectError(403, "FORBIDDEN") { driver.current(bearer) }
            expectError(403, "FORBIDDEN") { driver.accept(bearer, "order_1010") }
        }
    }

    @Test
    fun `driver requests need a valid token`() = runTest {
        expectError(401, "UNAUTHORIZED") { driver.available("no token") }
        expectError(401, "INVALID_TOKEN") { driver.accept("Bearer garbage", "order_1010") }
    }

    private suspend fun startDelivery(): String {
        finishCurrentDelivery()
        val bearer = bearer("driver1")
        driver.accept(bearer, "order_1010")
        return bearer
    }

    @Test
    fun `picking up sends the order out for delivery and records the event`() = runTest {
        val bearer = startDelivery()

        val order = driver.pickup(bearer, "order_1010")

        assertEquals(OrderStatus.OUT_FOR_DELIVERY, order.status)
        val last = orders.events(bearer("customer1"), "order_1010").last()
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, last.status)
        assertEquals("user_5", last.by)
        assertEquals(formatIsoUtc(now), last.at)
    }

    @Test
    fun `delivering completes the order and frees the driver`() = runTest {
        val bearer = startDelivery()
        driver.pickup(bearer, "order_1010")

        val order = driver.deliver(bearer, "order_1010")

        assertEquals(OrderStatus.DELIVERED, order.status)
        expectError(404, "NO_ACTIVE_DELIVERY") { driver.current(bearer) }
    }

    @Test
    fun `the customer sees every step of the delivery in order`() = runTest {
        val bearer = startDelivery()
        driver.pickup(bearer, "order_1010")
        driver.deliver(bearer, "order_1010")

        val events = orders.events(bearer("customer1"), "order_1010")

        assertEquals(
            listOf(
                OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY,
                OrderStatus.DRIVER_ASSIGNED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED
            ),
            events.map { it.status }
        )
        assertEquals(OrderStatus.DELIVERED, orders.get(bearer("customer1"), "order_1010").status)
    }

    @Test
    fun `steps cannot be skipped or repeated`() = runTest {
        val bearer = startDelivery()

        expectError(409, "INVALID_STATUS") { driver.deliver(bearer, "order_1010") }
        driver.pickup(bearer, "order_1010")
        expectError(409, "INVALID_STATUS") { driver.pickup(bearer, "order_1010") }
        driver.deliver(bearer, "order_1010")
        expectError(409, "INVALID_STATUS") { driver.deliver(bearer, "order_1010") }
    }

    @Test
    fun `the delivery already on the way can be delivered but not picked up again`() = runTest {
        val bearer = bearer("driver1")

        expectError(409, "INVALID_STATUS") { driver.pickup(bearer, "order_1009") }
        assertEquals(OrderStatus.DELIVERED, driver.deliver(bearer, "order_1009").status)
    }

    @Test
    fun `only the assigned driver can move an order forward`() = runTest {
        change("order_1009") { getAsJsonObject("delivery").addProperty("driver_id", "user_99") }
        val bearer = bearer("driver1")

        expectError(403, "NOT_YOUR_DELIVERY") { driver.deliver(bearer, "order_1009") }
        expectError(403, "NOT_YOUR_DELIVERY") { driver.pickup(bearer, "order_1010") }
    }

    @Test
    fun `customers and shop staff cannot move deliveries`() = runTest {
        for (username in listOf("customer1", "staff1", "owner1")) {
            val bearer = bearer(username)
            expectError(403, "FORBIDDEN") { driver.pickup(bearer, "order_1009") }
            expectError(403, "FORBIDDEN") { driver.deliver(bearer, "order_1009") }
        }
    }

    @Test
    fun `unknown orders and missing tokens are rejected`() = runTest {
        expectError(404, "ORDER_NOT_FOUND") { driver.pickup(bearer("driver1"), "order_9999") }
        expectError(401, "UNAUTHORIZED") { driver.deliver("no token", "order_1009") }
    }
}
