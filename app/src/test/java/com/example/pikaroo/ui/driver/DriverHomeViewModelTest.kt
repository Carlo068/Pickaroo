package com.example.pikaroo.ui.driver

import com.example.pikaroo.MainDispatcherRule
import com.example.pikaroo.common.mock.MockErrors
import com.example.pikaroo.ui.auth.data.SessionExpiredException
import com.example.pikaroo.ui.driver.data.DriverRepository
import com.example.pikaroo.ui.driver.model.AvailableOrderDto
import com.example.pikaroo.ui.driver.viewmodel.DriverHomeViewModel
import com.example.pikaroo.ui.orders.model.DeliveryDto
import com.example.pikaroo.ui.orders.model.Fulfilment
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.model.OrderStatus
import com.example.pikaroo.ui.orders.model.ShopSummaryDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

private val shop = ShopSummaryDto("shop_2", "Panadería La Espiga", "Avenida Muestra 45", 19.4194, -99.1625)

private fun available(id: String) = AvailableOrderDto(
    id = id, shop = shop, delivery = DeliveryDto("Calle Falsa 9", 19.42, -99.16, null),
    deliveryFee = 1.5, itemsCount = 3, createdAt = "2026-10-20T15:10:00Z"
)

private fun carrying(id: String) = OrderDto(
    id = id, shopId = "shop_2", customerId = "user_1", fulfilment = Fulfilment.DELIVERY,
    status = OrderStatus.DRIVER_ASSIGNED, items = emptyList(), subtotal = 0.0, deliveryFee = 1.5, total = 1.5,
    pickup = null, delivery = DeliveryDto("Calle Falsa 9", 19.42, -99.16, "user_5"),
    createdAt = "2026-10-20T15:10:00Z", shop = shop
)

private class FakeDriverRepository : DriverRepository {
    var availableOrders: List<AvailableOrderDto> = emptyList()
    var currentOrder: OrderDto? = null
    var failure: Exception? = null
    var acceptFailure: Exception? = null
    var acceptDelayMillis = 0L
    var actionFailure: Exception? = null
    var actionDelayMillis = 0L
    var pickups = 0
    var deliveries = 0
    var availableCalls = 0
    var currentCalls = 0
    val accepted = mutableListOf<String>()

    override suspend fun available(): List<AvailableOrderDto> {
        availableCalls++
        failure?.let { throw it }
        return availableOrders
    }

    override suspend fun current(): OrderDto? {
        currentCalls++
        failure?.let { throw it }
        return currentOrder
    }

    override suspend fun pickup(orderId: String): OrderDto {
        if (actionDelayMillis > 0) delay(actionDelayMillis)
        actionFailure?.let { throw it }
        pickups++
        return currentOrder!!.copy(status = OrderStatus.OUT_FOR_DELIVERY).also { currentOrder = it }
    }

    override suspend fun deliver(orderId: String): OrderDto {
        if (actionDelayMillis > 0) delay(actionDelayMillis)
        actionFailure?.let { throw it }
        deliveries++
        val done = currentOrder!!.copy(status = OrderStatus.DELIVERED)
        currentOrder = null
        return done
    }

    override suspend fun reportLocation(orderId: String, lat: Double, lng: Double) = Unit

    override suspend fun accept(orderId: String): OrderDto {
        if (acceptDelayMillis > 0) delay(acceptDelayMillis)
        acceptFailure?.let { throw it }
        accepted += orderId
        availableOrders = availableOrders.filterNot { it.id == orderId }
        return carrying(orderId).also { currentOrder = it }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DriverHomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeDriverRepository()
    private val viewModel = DriverHomeViewModel(repository)

    @Test
    fun `starts online and loading`() {
        assertTrue(viewModel.state.value.isOnline)
        assertTrue(viewModel.state.value.isLoading)
    }

    @Test
    fun `polling shows the available orders`() = runTest {
        repository.availableOrders = listOf(available("order_1"), available("order_2"))

        val job = launch { viewModel.poll() }
        runCurrent()
        job.cancel()

        assertEquals(listOf("order_1", "order_2"), viewModel.state.value.available.map { it.id })
        assertTrue(!viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `polling repeats every 5 seconds and stops when cancelled`() = runTest {
        val job = launch { viewModel.poll() }
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        val whileVisible = repository.currentCalls
        job.cancel()
        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(3, whileVisible)
        assertEquals(whileVisible, repository.currentCalls)
    }

    @Test
    fun `a delivery in progress replaces the list`() = runTest {
        repository.currentOrder = carrying("order_9")
        repository.availableOrders = listOf(available("order_1"))

        val job = launch { viewModel.poll() }
        runCurrent()
        job.cancel()

        assertEquals("order_9", viewModel.state.value.current!!.id)
        assertTrue(viewModel.state.value.available.isEmpty())
        assertEquals(0, repository.availableCalls)
    }

    @Test
    fun `going offline hides the list and stops asking for orders`() = runTest {
        repository.availableOrders = listOf(available("order_1"))
        val job = launch { viewModel.poll() }
        runCurrent()

        viewModel.setOnline(false)
        val callsBefore = repository.availableCalls
        advanceTimeBy(10_000)
        runCurrent()
        job.cancel()

        assertTrue(viewModel.state.value.available.isEmpty())
        assertEquals(callsBefore, repository.availableCalls)
    }

    @Test
    fun `going online again loads the orders right away`() = runTest {
        repository.availableOrders = listOf(available("order_1"))
        viewModel.setOnline(false)
        runCurrent()

        viewModel.setOnline(true)
        runCurrent()

        assertEquals(listOf("order_1"), viewModel.state.value.available.map { it.id })
    }

    @Test
    fun `an offline driver still sees the delivery they are carrying`() = runTest {
        repository.currentOrder = carrying("order_9")
        viewModel.setOnline(false)

        val job = launch { viewModel.poll() }
        runCurrent()
        job.cancel()

        assertEquals("order_9", viewModel.state.value.current!!.id)
    }

    @Test
    fun `accepting an order makes it the current delivery`() = runTest {
        repository.availableOrders = listOf(available("order_1"))
        val job = launch { viewModel.poll() }
        runCurrent()

        viewModel.accept("order_1")
        runCurrent()
        job.cancel()

        assertEquals(listOf("order_1"), repository.accepted)
        assertEquals("order_1", viewModel.state.value.current!!.id)
        assertTrue(viewModel.state.value.available.isEmpty())
        assertNull(viewModel.state.value.acceptingId)
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `an order another driver took first shows a message and leaves the list`() = runTest {
        repository.availableOrders = listOf(available("order_1"), available("order_2"))
        val job = launch { viewModel.poll() }
        runCurrent()
        repository.availableOrders = listOf(available("order_2"))
        repository.acceptFailure = MockErrors.conflict("ORDER_NOT_AVAILABLE")

        viewModel.accept("order_1")
        runCurrent()
        job.cancel()

        assertEquals("Otro repartidor ya tomó este pedido", viewModel.state.value.message)
        assertEquals(listOf("order_2"), viewModel.state.value.available.map { it.id })
        assertNull(viewModel.state.value.acceptingId)
        assertNull(viewModel.state.value.current)
    }

    @Test
    fun `other conflicts and failures have their own messages`() = runTest {
        suspend fun messageFor(failure: Exception): String? {
            repository.acceptFailure = failure
            viewModel.accept("order_1")
            runCurrent()
            return viewModel.state.value.message
        }

        assertEquals("Ya tienes una entrega en curso", messageFor(MockErrors.conflict("ALREADY_DELIVERING")))
        assertEquals("Este pedido ya no existe", messageFor(MockErrors.notFound("ORDER_NOT_FOUND")))
        assertEquals("No tienes permiso para aceptar pedidos", messageFor(MockErrors.forbidden()))
        assertEquals("No se pudo completar la acción", messageFor(MockErrors.http(500, "SERVER_ERROR", "boom")))
        assertEquals("Sin conexión. Intenta de nuevo.", messageFor(IOException("offline")))
    }

    @Test
    fun `the message can be dismissed`() = runTest {
        repository.acceptFailure = MockErrors.conflict("ORDER_NOT_AVAILABLE")
        viewModel.accept("order_1")
        runCurrent()

        viewModel.dismissMessage()

        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `a second tap while accepting is ignored`() = runTest {
        repository.availableOrders = listOf(available("order_1"), available("order_2"))
        repository.acceptDelayMillis = 1_000

        viewModel.accept("order_1")
        viewModel.accept("order_2")
        runCurrent()
        assertEquals("order_1", viewModel.state.value.acceptingId)
        assertTrue(repository.accepted.isEmpty())

        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(listOf("order_1"), repository.accepted)
    }

    @Test
    fun `a dropped connection keeps polling and shows an error`() = runTest {
        repository.failure = IOException("offline")
        val job = launch { viewModel.poll() }
        runCurrent()

        assertEquals("Sin conexión. Reintentando…", viewModel.state.value.error)
        repository.failure = null
        advanceTimeBy(5_000)
        runCurrent()
        job.cancel()

        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `a driver without permission stops polling`() = runTest {
        repository.failure = MockErrors.forbidden()

        viewModel.poll()

        assertEquals("No tienes permiso para ver entregas", viewModel.state.value.error)
        assertEquals(1, repository.currentCalls)
    }

    @Test
    fun `an expired session stops polling quietly`() = runTest {
        repository.failure = SessionExpiredException()

        viewModel.poll()

        assertNull(viewModel.state.value.error)
        assertEquals(1, repository.currentCalls)
        assertEquals(0L, currentTime)
    }

    @Test
    fun `picking up moves the delivery to the next step`() = runTest {
        repository.currentOrder = carrying("order_9")
        val job = launch { viewModel.poll() }
        runCurrent()

        viewModel.pickUp()
        runCurrent()
        job.cancel()

        assertEquals(1, repository.pickups)
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, viewModel.state.value.current!!.status)
        assertNull(viewModel.state.value.message)
        assertTrue(!viewModel.state.value.acting)
    }

    @Test
    fun `delivering ends the delivery, celebrates and brings the list back`() = runTest {
        repository.currentOrder = carrying("order_9").copy(status = OrderStatus.OUT_FOR_DELIVERY)
        repository.availableOrders = listOf(available("order_next"))
        val job = launch { viewModel.poll() }
        runCurrent()

        viewModel.deliver()
        runCurrent()
        job.cancel()

        assertEquals(1, repository.deliveries)
        assertNull(viewModel.state.value.current)
        assertEquals("¡Entrega completada!", viewModel.state.value.message)
        assertEquals(listOf("order_next"), viewModel.state.value.available.map { it.id })
    }

    @Test
    fun `the action buttons do nothing without a delivery in progress`() = runTest {
        viewModel.pickUp()
        viewModel.deliver()
        runCurrent()

        assertEquals(0, repository.pickups)
        assertEquals(0, repository.deliveries)
        assertTrue(!viewModel.state.value.acting)
    }

    @Test
    fun `a second tap while the action is running is ignored`() = runTest {
        repository.currentOrder = carrying("order_9")
        repository.actionDelayMillis = 1_000
        val job = launch { viewModel.poll() }
        runCurrent()

        viewModel.pickUp()
        viewModel.pickUp()
        runCurrent()
        assertTrue(viewModel.state.value.acting)
        advanceTimeBy(1_000)
        runCurrent()
        job.cancel()

        assertEquals(1, repository.pickups)
        assertTrue(!viewModel.state.value.acting)
    }

    @Test
    fun `a step that is no longer valid shows a message and refreshes`() = runTest {
        repository.currentOrder = carrying("order_9")
        val job = launch { viewModel.poll() }
        runCurrent()
        repository.actionFailure = MockErrors.conflict("INVALID_STATUS")

        viewModel.pickUp()
        runCurrent()
        job.cancel()

        assertEquals("Este pedido ya no está en ese paso", viewModel.state.value.message)
        assertTrue(!viewModel.state.value.acting)
        assertEquals("order_9", viewModel.state.value.current!!.id)
    }

    @Test
    fun `other failures of a delivery action have their own messages`() = runTest {
        repository.currentOrder = carrying("order_9")
        val job = launch { viewModel.poll() }
        runCurrent()

        suspend fun messageFor(failure: Exception): String? {
            repository.actionFailure = failure
            viewModel.deliver()
            runCurrent()
            return viewModel.state.value.message
        }

        assertEquals("Esta entrega pertenece a otro repartidor", messageFor(MockErrors.forbidden("NOT_YOUR_DELIVERY")))
        assertEquals("Sin conexión. Intenta de nuevo.", messageFor(IOException("offline")))
        job.cancel()
    }
}
