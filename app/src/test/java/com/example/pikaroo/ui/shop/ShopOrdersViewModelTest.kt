package com.example.pikaroo.ui.shop

import com.example.pikaroo.MainDispatcherRule
import com.example.pikaroo.common.mock.MockErrors
import com.example.pikaroo.ui.orders.model.*
import com.example.pikaroo.ui.shop.data.ShopOrdersRepository
import com.example.pikaroo.ui.shop.model.ShopStatusRequest
import com.example.pikaroo.ui.shop.viewmodel.ShopOrdersViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShopOrdersViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val order = OrderDto("test", "shop_1", "user_1", Fulfilment.PICKUP, OrderStatus.PLACED,
        emptyList(), 1.0, 0.0, 1.0, null, null, "2026-11-12T00:00:00Z")
    private inner class Repository : ShopOrdersRepository {
        var list = listOf(order)
        var reads = 0
        var failure: Exception? = null
        var updates = 0
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun orders(): List<OrderDto> { reads++; failure?.let { throw it }; return list }
        override suspend fun update(id: String, request: ShopStatusRequest): OrderDto {
            updates++; gate?.await(); failure?.let { throw it }
            return order.copy(status = request.status).also { list = listOf(it) }
        }
    }
    @Test fun `poll refreshes and stops on cancellation`() = runTest {
        val r = Repository(); val vm = ShopOrdersViewModel(r, 1000)
        val job = launch { vm.poll() }; runCurrent()
        assertEquals(listOf(order), vm.state.value.orders)
        r.list = emptyList(); advanceTimeBy(1000); runCurrent()
        assertTrue(vm.state.value.orders.isEmpty()); assertEquals(2, r.reads)
        job.cancelAndJoin(); advanceTimeBy(3000); assertEquals(2, r.reads)
    }
    @Test fun `double tap saves once and updates the visible row`() = runTest {
        val r = Repository(); val vm = ShopOrdersViewModel(r)
        vm.retry(); runCurrent(); r.gate = CompletableDeferred()
        vm.advance(order); vm.advance(order); runCurrent()
        assertEquals(1, r.updates); assertEquals(order.id, vm.state.value.updatingId)
        r.gate!!.complete(Unit); runCurrent()
        assertEquals(OrderStatus.ACCEPTED, vm.state.value.orders.single().status)
        assertNull(vm.state.value.updatingId)
    }
    @Test fun `failure keeps data and retry recovers`() = runTest {
        val r = Repository(); val vm = ShopOrdersViewModel(r)
        vm.retry(); runCurrent(); r.failure = IllegalStateException("Offline")
        vm.advance(order); runCurrent()
        assertEquals(listOf(order), vm.state.value.orders); assertNotNull(vm.state.value.error)
        r.failure = null; vm.retry(); runCurrent(); assertNull(vm.state.value.error)
    }
    @Test fun `forbidden refresh ends polling`() = runTest {
        val r = Repository(); r.failure = MockErrors.forbidden()
        val vm = ShopOrdersViewModel(r, 1000)
        val job = launch { vm.poll() }; runCurrent(); advanceTimeBy(5000)
        assertTrue(job.isCompleted); assertEquals(1, r.reads)
    }
}
