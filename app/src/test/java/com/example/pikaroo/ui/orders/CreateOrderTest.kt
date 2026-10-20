package com.example.pikaroo.ui.orders

import com.example.pikaroo.common.mock.*
import com.example.pikaroo.common.preferences.StringStore
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.auth.network.MockAuthService
import com.example.pikaroo.ui.auth.network.MockAuthenticator
import com.example.pikaroo.ui.orders.model.*
import com.example.pikaroo.ui.orders.network.MockOrdersService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException

class CreateOrderTest {
    private val storage = InMemoryStringStore()
    private val seed = FileSeedSource()
    private val api = MockPickarooApi(MockStore(seed, storage)) { 0L }
    private val now = 1_800_000_000L
    private val auth = MockAuthService(api, nowSeconds = { now })
    private val orders = MockOrdersService(api, MockAuthenticator { now }, nowMillis = { now * 1000 })
    private suspend fun bearer(user: String = "customer1") = "Bearer " + auth.login(LoginRequest(user, "demo1234")).access
    private fun request(method: Fulfilment = Fulfilment.PICKUP) = CreateOrderRequest(
        "request-1", "shop_1", method, listOf(OrderLineDto("1", "Manzanas", 2, 3.49, 0, 6.98)),
        if (method == Fulfilment.DELIVERY) "  Calle Ejemplo 120  " else null
    )

    @Test fun `pickup persists snapshot and event and survives reopening the store`() = runTest {
        val token = bearer()
        val order = orders.create(token, request())
        assertEquals(OrderStatus.PLACED, order.status)
        assertEquals("user_1", order.customerId)
        assertEquals(6.98, order.total, 0.001)
        assertEquals(0.0, order.deliveryFee, 0.001)
        assertNotNull(order.pickup)
        assertNull(order.delivery)
        val reopened = MockOrdersService(MockPickarooApi(MockStore(seed, storage)) { 0L }, MockAuthenticator { now })
        assertEquals(order, reopened.get(token, order.id))
        assertEquals(listOf(OrderStatus.PLACED), reopened.events(token, order.id).map { it.status })
        assertTrue(reopened.listMine(token).any { it.id == order.id })
    }

    @Test fun `delivery has a trimmed address and fee and reusing a request never duplicates`() = runTest {
        val token = bearer()
        val first = orders.create(token, request(Fulfilment.DELIVERY))
        val second = orders.create(token, request(Fulfilment.DELIVERY))
        assertEquals(first.id, second.id)
        assertEquals(1, orders.listMine(token).count { it.id == first.id })
        assertEquals("Calle Ejemplo 120", first.delivery!!.address)
        assertNull(first.delivery.lat)
        assertEquals(9.97, first.total, 0.001)
        assertNull(first.pickup)
        try { orders.get(bearer("owner1"), first.id); fail("Foreign order must be denied") }
        catch (e: HttpException) { assertEquals(403, e.code()) }
    }

    @Test fun `invalid input and staff are rejected without saving an order`() = runTest {
        val token = bearer()
        val count = orders.listMine(token).size
        for (bad in listOf(request().copy(items = emptyList()), request(Fulfilment.DELIVERY).copy(address = " "),
            request().copy(items = listOf(request().items.first().copy(quantity = 0))))) {
            try { orders.create(token, bad); fail("Expected validation") }
            catch (e: HttpException) { assertEquals(400, e.code()) }
        }
        try { orders.create(bearer("staff1"), request()); fail("Staff cannot create a customer order") }
        catch (e: HttpException) { assertEquals(403, e.code()) }
        assertEquals(count, orders.listMine(token).size)
    }

    @Test fun `failed persistence does not leave a phantom successful order in the cache`() = runTest {
        var failWrite = true
        val failingStore = object : StringStore by storage {
            override fun put(key: String, value: String) {
                if (key == "mock_store_orders" && failWrite) throw IllegalStateException("Disk failure")
                storage.put(key, value)
            }
        }
        val service = MockOrdersService(MockPickarooApi(MockStore(seed, failingStore)) { 0L }, MockAuthenticator { now })
        val token = bearer()
        val before = service.listMine(token).size
        try { service.create(token, request()); fail("Expected disk failure") } catch (_: IllegalStateException) { }
        assertEquals(before, service.listMine(token).size)
        failWrite = false
        val created = service.create(token, request())
        assertEquals(created.id, service.create(token, request()).id)
        assertEquals(before + 1, service.listMine(token).size)
    }
}
