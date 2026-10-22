package com.example.pikaroo.ui.order

import com.example.pikaroo.MainDispatcherRule
import com.example.pikaroo.ui.cart.FakeCartStorage
import com.example.pikaroo.ui.cart.model.*
import com.example.pikaroo.ui.cart.viewmodel.CartViewModel
import com.example.pikaroo.ui.order.model.*
import com.example.pikaroo.ui.order.viewmodel.PlaceOrderViewModel
import com.example.pikaroo.ui.orders.data.OrdersRepository
import com.example.pikaroo.ui.orders.model.*
import com.example.pikaroo.ui.products.model.Product
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

private class CreationRepository : OrdersRepository {
    val requests = mutableListOf<CreateOrderRequest>()
    var result = CompletableDeferred<OrderDto>()
    override suspend fun create(request: CreateOrderRequest): OrderDto { requests += request; return result.await() }
    override suspend fun driverLocation(id: String): DriverLocationDto? = null
    override suspend fun get(id: String): OrderDto = error("Unexpected get")
    override suspend fun listMine(): List<OrderDto> = error("Unexpected list")
    override suspend fun events(id: String): List<OrderEventDto> = error("Unexpected events")
}

class PlaceOrderViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val item = CartItem("1", "Pan", 3.5, "", 2)
    private val cart = CartUiState(listOf(item))
    private val delivery = OrderState(DeliveryMethod.PICKUP)
    private fun order() = OrderDto("order_new", "shop_1", "user_1", Fulfilment.PICKUP, OrderStatus.PLACED,
        emptyList(), 7.0, 0.0, 7.0, PickupDto("2026-10-20T20:00:00Z", "ABC123"), null, "2026-10-20T19:00:00Z")

    @Test fun `double tap creates once and cart changes only after successful save`() = runTest {
        val repository = CreationRepository()
        val vm = PlaceOrderViewModel(repository)
        val basket = CartViewModel(FakeCartStorage(cart.items))
        val saved: (OrderDto) -> Unit = { basket.removeOrderedItems(cart.items) }
        vm.place(cart, delivery, saved)
        vm.place(cart, delivery, saved)
        assertEquals(1, repository.requests.size)
        assertEquals(2, basket.uiState.value.itemCount)
        assertTrue(vm.state.value.isSaving)
        repository.result.complete(order())
        runCurrent()
        assertEquals(0, basket.uiState.value.itemCount)
        assertFalse(vm.state.value.isSaving)
    }

    @Test fun `failure preserves cart and retry reuses the same request id`() = runTest {
        val repository = CreationRepository()
        val vm = PlaceOrderViewModel(repository)
        var successes = 0
        vm.place(cart, delivery) { successes++ }
        repository.result.completeExceptionally(IllegalStateException("Offline"))
        runCurrent()
        assertNotNull(vm.state.value.error)
        assertEquals(0, successes)
        repository.result = CompletableDeferred()
        vm.place(cart, delivery) { successes++ }
        assertEquals(repository.requests[0].requestId, repository.requests[1].requestId)
        repository.result.complete(order())
        runCurrent()
        assertEquals(1, successes)
    }

    @Test fun `empty basket and missing address never create an order`() = runTest {
        val repository = CreationRepository()
        val vm = PlaceOrderViewModel(repository)
        vm.place(CartUiState(), delivery) { fail("Should not save") }
        vm.place(cart, OrderState(DeliveryMethod.DELIVERY, " ")) { fail("Should not save") }
        assertTrue(repository.requests.isEmpty())
    }

    @Test fun `items added while an order saves are preserved`() {
        val basket = CartViewModel(FakeCartStorage(cart.items))
        basket.add(Product("1", "Pan", "", 3.5, "", "Panadería"))
        basket.add(Product("2", "Leche", "", 2.0, "", "Lácteos"))
        basket.removeOrderedItems(cart.items)
        assertEquals(listOf("1", "2"), basket.uiState.value.items.map { it.productId })
        assertEquals(listOf(1, 1), basket.uiState.value.items.map { it.quantity })
    }
}
