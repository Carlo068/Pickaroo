package com.example.pikaroo.ui.orders

import com.example.pikaroo.MainDispatcherRule
import com.example.pikaroo.common.mock.MockErrors
import com.example.pikaroo.ui.auth.data.SessionExpiredException
import com.example.pikaroo.ui.orders.data.OrdersRepository
import com.example.pikaroo.ui.orders.model.DriverLocationDto
import com.example.pikaroo.ui.orders.model.Fulfilment
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.model.OrderEventDto
import com.example.pikaroo.ui.orders.model.OrderStatus
import com.example.pikaroo.ui.orders.model.OrderStatus.ACCEPTED
import com.example.pikaroo.ui.orders.model.OrderStatus.DELIVERED
import com.example.pikaroo.ui.orders.model.OrderStatus.OUT_FOR_DELIVERY
import com.example.pikaroo.ui.orders.model.OrderStatus.PICKED_UP
import com.example.pikaroo.ui.orders.model.OrderStatus.PLACED
import com.example.pikaroo.ui.orders.model.OrderStatus.PREPARING
import com.example.pikaroo.ui.orders.model.OrderStatus.READY
import com.example.pikaroo.ui.orders.model.StepState
import com.example.pikaroo.ui.orders.viewmodel.OrdersViewModel
import com.example.pikaroo.ui.orders.viewmodel.PollIntervals
import com.example.pikaroo.ui.orders.viewmodel.TrackingViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

private fun orderWith(status: OrderStatus, fulfilment: Fulfilment = Fulfilment.PICKUP, id: String = "order_1") = OrderDto(
    id = id, shopId = "shop_1", customerId = "user_1", fulfilment = fulfilment, status = status,
    items = emptyList(), subtotal = 0.0, deliveryFee = 0.0, total = 0.0, pickup = null, delivery = null,
    createdAt = "2026-10-20T14:00:00Z"
)

/** Cada get() consume la siguiente respuesta (un pedido o un fallo); la última se repite. */
private class ScriptedOrdersRepository(
    private val script: List<Any>,
    private val locations: List<Any?> = emptyList()
) : OrdersRepository {
    override suspend fun create(request: com.example.pikaroo.ui.orders.model.CreateOrderRequest): OrderDto = error("Unexpected create")
    var getCalls = 0
        private set
    var locationCalls = 0
        private set

    private fun next(): Any = script[minOf(getCalls, script.lastIndex)]

    override suspend fun get(id: String): OrderDto {
        val response = next()
        getCalls++
        if (response is Exception) throw response
        return response as OrderDto
    }

    override suspend fun events(id: String): List<OrderEventDto> =
        listOf(OrderEventDto(PLACED, "2026-10-20T14:00:00Z", "user_1"))

    override suspend fun listMine(): List<OrderDto> = emptyList()

    override suspend fun driverLocation(id: String): DriverLocationDto? {
        val response = locations.getOrNull(minOf(locationCalls, locations.lastIndex))
        locationCalls++
        if (response is Exception) throw response
        return response as DriverLocationDto?
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TrackingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(vararg script: Any, intervals: PollIntervals = PollIntervals()): Pair<TrackingViewModel, ScriptedOrdersRepository> {
        val repository = ScriptedOrdersRepository(script.toList())
        return TrackingViewModel("order_1", repository, intervals) to repository
    }

    private fun viewModelWithLocations(
        script: List<Any>,
        locations: List<Any?>
    ): Pair<TrackingViewModel, ScriptedOrdersRepository> {
        val repository = ScriptedOrdersRepository(script, locations)
        return TrackingViewModel("order_1", repository) to repository
    }

    private fun at(lat: Double, lng: Double) = DriverLocationDto(lat, lng, "2026-10-20T15:05:00Z")

    @Test
    fun `polling fills the state with the order and its timeline`() = runTest {
        val (viewModel, _) = viewModel(orderWith(READY))

        val job = launch { viewModel.poll() }
        runCurrent()
        job.cancel()

        val state = viewModel.state.value
        assertEquals(READY, state.order!!.status)
        assertEquals(1, state.events.size)
        assertEquals(StepState.CURRENT, state.timeline.first { it.status == READY }.state)
        assertTrue(!state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `polling checks every 5 seconds and stops at a final status`() = runTest {
        val (viewModel, repository) = viewModel(orderWith(PLACED), orderWith(PREPARING), orderWith(PICKED_UP))

        viewModel.poll()

        assertEquals(3, repository.getCalls)
        assertEquals(10_000L, currentTime)
    }

    @Test
    fun `polling is faster while the order is out for delivery`() = runTest {
        val delivery = Fulfilment.DELIVERY
        val (viewModel, repository) = viewModel(
            orderWith(READY, delivery), orderWith(OUT_FOR_DELIVERY, delivery),
            orderWith(OUT_FOR_DELIVERY, delivery), orderWith(DELIVERED, delivery)
        )

        viewModel.poll()

        assertEquals(4, repository.getCalls)
        assertEquals(5_000L + 2_000L + 2_000L, currentTime)
    }

    @Test
    fun `an order that is already final is fetched once and never polled again`() = runTest {
        val (viewModel, repository) = viewModel(orderWith(DELIVERED, Fulfilment.DELIVERY))

        viewModel.poll()

        assertEquals(1, repository.getCalls)
        assertEquals(0L, currentTime)
    }

    @Test
    fun `leaving the screen stops the polling`() = runTest {
        val (viewModel, repository) = viewModel(orderWith(PREPARING))

        val job = launch { viewModel.poll() }
        advanceTimeBy(12_000)
        runCurrent()
        val callsWhileVisible = repository.getCalls
        job.cancel()
        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(3, callsWhileVisible)
        assertEquals(callsWhileVisible, repository.getCalls)
    }

    @Test
    fun `a dropped connection keeps the last data and keeps polling`() = runTest {
        val (viewModel, repository) = viewModel(orderWith(ACCEPTED), IOException("offline"), orderWith(PICKED_UP))

        val job = launch { viewModel.poll() }
        advanceTimeBy(1)
        assertEquals(ACCEPTED, viewModel.state.value.order!!.status)

        advanceTimeBy(5_000)
        assertEquals("Sin conexión. Reintentando…", viewModel.state.value.error)
        assertEquals(ACCEPTED, viewModel.state.value.order!!.status)

        advanceTimeBy(5_000)
        job.join()
        assertNull(viewModel.state.value.error)
        assertEquals(PICKED_UP, viewModel.state.value.order!!.status)
        assertEquals(3, repository.getCalls)
    }

    @Test
    fun `a server error is shown and polling continues`() = runTest {
        val (viewModel, repository) = viewModel(MockErrors.http(500, "SERVER_ERROR", "boom"), orderWith(PICKED_UP))

        viewModel.poll()

        assertEquals(2, repository.getCalls)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `an order that is not yours or not found stops the polling with a message`() = runTest {
        val (forbidden, forbiddenRepository) = viewModel(MockErrors.forbidden())
        val (missing, missingRepository) = viewModel(MockErrors.notFound())

        forbidden.poll()
        missing.poll()

        assertEquals("No tienes acceso a este pedido", forbidden.state.value.error)
        assertEquals("No se encontró el pedido", missing.state.value.error)
        assertEquals(1, forbiddenRepository.getCalls)
        assertEquals(1, missingRepository.getCalls)
        assertNull(forbidden.state.value.order)
    }

    @Test
    fun `an expired session stops the polling without an error message`() = runTest {
        val (viewModel, repository) = viewModel(SessionExpiredException())

        viewModel.poll()

        assertEquals(1, repository.getCalls)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `intervals can be changed`() = runTest {
        val (viewModel, _) = viewModel(orderWith(PLACED), orderWith(PICKED_UP), intervals = PollIntervals(defaultMillis = 100))

        viewModel.poll()

        assertEquals(100L, currentTime)
    }

    @Test
    fun `the orders list loads the customer's orders`() = runTest {
        val repository = object : OrdersRepository {
    override suspend fun create(request: com.example.pikaroo.ui.orders.model.CreateOrderRequest): OrderDto = error("Unexpected create")
            override suspend fun listMine() = listOf(orderWith(READY, id = "order_7"), orderWith(PLACED, id = "order_8"))
            override suspend fun get(id: String) = orderWith(READY)
            override suspend fun events(id: String) = emptyList<OrderEventDto>()
            override suspend fun driverLocation(id: String): DriverLocationDto? = null
        }

        val viewModel = OrdersViewModel(repository)

        assertEquals(listOf("order_7", "order_8"), viewModel.state.value.orders.map { it.id })
        assertTrue(!viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `the orders list shows an error and can be retried`() = runTest {
        var fail = true
        val repository = object : OrdersRepository {
    override suspend fun create(request: com.example.pikaroo.ui.orders.model.CreateOrderRequest): OrderDto = error("Unexpected create")
            override suspend fun listMine(): List<OrderDto> {
                if (fail) throw MockErrors.http(500, "SERVER_ERROR", "boom")
                return listOf(orderWith(READY))
            }
            override suspend fun get(id: String) = orderWith(READY)
            override suspend fun events(id: String) = emptyList<OrderEventDto>()
            override suspend fun driverLocation(id: String): DriverLocationDto? = null
        }

        val viewModel = OrdersViewModel(repository)
        assertEquals("No se pudieron cargar tus pedidos", viewModel.state.value.error)

        fail = false
        viewModel.load()

        assertNull(viewModel.state.value.error)
        assertEquals(1, viewModel.state.value.orders.size)
        assertNotNull(viewModel.state.value.orders.firstOrNull())
    }

    @Test
    fun `the driver location is followed while the order is out for delivery`() = runTest {
        val delivery = Fulfilment.DELIVERY
        val (viewModel, repository) = viewModelWithLocations(
            script = listOf(
                orderWith(OUT_FOR_DELIVERY, delivery), orderWith(OUT_FOR_DELIVERY, delivery),
                orderWith(OUT_FOR_DELIVERY, delivery), orderWith(DELIVERED, delivery)
            ),
            locations = listOf(at(19.43, -99.13), at(19.42, -99.14), at(19.41, -99.15))
        )
        val seen = mutableListOf<Double?>()

        val job = launch { viewModel.poll() }
        runCurrent()
        seen += viewModel.state.value.driverLocation?.lat
        advanceTimeBy(2_000)
        runCurrent()
        seen += viewModel.state.value.driverLocation?.lat
        advanceTimeBy(2_000)
        runCurrent()
        seen += viewModel.state.value.driverLocation?.lat
        job.join()

        assertEquals(listOf<Double?>(19.43, 19.42, 19.41), seen)
        assertEquals(3, repository.locationCalls)
        assertNull(viewModel.state.value.driverLocation)
    }

    @Test
    fun `the driver location is not requested when no driver is on the way`() = runTest {
        val (pickup, pickupRepository) = viewModelWithLocations(listOf(orderWith(PICKED_UP)), listOf(at(1.0, 1.0)))
        val (waiting, waitingRepository) = viewModelWithLocations(
            listOf(orderWith(READY, Fulfilment.DELIVERY), orderWith(DELIVERED, Fulfilment.DELIVERY)), listOf(at(1.0, 1.0))
        )

        pickup.poll()
        waiting.poll()

        assertEquals(0, pickupRepository.locationCalls)
        assertEquals(0, waitingRepository.locationCalls)
    }

    @Test
    fun `a driver who has not reported yet leaves the map without a pin`() = runTest {
        val delivery = Fulfilment.DELIVERY
        val (viewModel, _) = viewModelWithLocations(
            script = listOf(orderWith(OUT_FOR_DELIVERY, delivery), orderWith(DELIVERED, delivery)),
            locations = listOf(null)
        )

        viewModel.poll()

        assertNull(viewModel.state.value.driverLocation)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `a failing location request keeps the last pin and the order keeps updating`() = runTest {
        val delivery = Fulfilment.DELIVERY
        val (viewModel, repository) = viewModelWithLocations(
            script = listOf(orderWith(OUT_FOR_DELIVERY, delivery), orderWith(OUT_FOR_DELIVERY, delivery), orderWith(DELIVERED, delivery)),
            locations = listOf(at(19.43, -99.13), MockErrors.http(500, "SERVER_ERROR", "boom"))
        )

        val job = launch { viewModel.poll() }
        runCurrent()
        advanceTimeBy(2_000)
        runCurrent()

        assertEquals(19.43, viewModel.state.value.driverLocation!!.lat, 0.0001)
        assertNull(viewModel.state.value.error)
        job.join()
        assertEquals(3, repository.getCalls)
    }
}
