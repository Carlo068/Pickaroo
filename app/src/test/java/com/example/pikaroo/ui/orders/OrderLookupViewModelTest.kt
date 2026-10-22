package com.example.pikaroo.ui.orders

import com.example.pikaroo.MainDispatcherRule
import com.example.pikaroo.common.mock.MockErrors
import com.example.pikaroo.ui.orders.data.OrdersRepository
import com.example.pikaroo.ui.orders.model.*
import com.example.pikaroo.ui.orders.viewmodel.OrderLookupViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

private class LookupRepository : OrdersRepository {
    var reads = 0
    var writes = 0
    var failure: Exception? = null
    override suspend fun get(id: String): OrderDto { reads++; failure?.let { throw it }; return receiptOrder(id) }
    override suspend fun create(request: CreateOrderRequest): OrderDto { writes++; error("Unexpected create") }
    override suspend fun listMine(): List<OrderDto> = emptyList()
    override suspend fun events(id: String): List<OrderEventDto> = emptyList()
    override suspend fun driverLocation(id: String): DriverLocationDto? = null
}

class OrderLookupViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun `invalid scan never queries or writes and cancellation leaves manual input usable`() = runTest {
        val repository = LookupRepository()
        val model = OrderLookupViewModel(repository)
        model.scanned("https://example.com/order_1001") { fail("Invalid QR") }
        assertNotNull(model.state.value.error)
        assertEquals(0, repository.reads)
        model.scanCancelled()
        assertNotNull(model.state.value.notice)
        model.updateInput(" order_1001 ")
        var found: OrderDto? = null
        model.lookup { found = it }
        assertEquals("order_1001", found!!.id)
        assertEquals(1, repository.reads)
        assertEquals(0, repository.writes)
    }

    @Test fun `unknown and foreign orders show not found without opening anything`() = runTest {
        val repository = LookupRepository()
        val model = OrderLookupViewModel(repository)
        for (error in listOf(MockErrors.notFound(), MockErrors.forbidden())) {
            repository.failure = error
            model.scanned("order_1001") { fail("Must not open a missing or foreign order") }
            assertEquals("Orden no encontrada", model.state.value.error)
            assertFalse(model.state.value.isLoading)
        }
        assertEquals(0, repository.writes)
    }

    @Test fun `lookup failure can be retried and lookup never changes the saved status`() = runTest {
        val repository = LookupRepository()
        val model = OrderLookupViewModel(repository)
        repository.failure = IllegalStateException("Offline")
        model.scanned("order_1001") { fail("Offline") }
        assertNotNull(model.state.value.error)
        repository.failure = null
        var found: OrderDto? = null
        model.lookup { found = it }
        assertEquals(OrderStatus.PLACED, found!!.status)
        assertNull(model.state.value.error)
        assertEquals(0, repository.writes)
    }
}
