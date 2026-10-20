package com.example.pikaroo.ui.orders

import com.example.pikaroo.MainDispatcherRule
import com.example.pikaroo.common.mock.MockErrors
import com.example.pikaroo.ui.orders.data.OrdersRepository
import com.example.pikaroo.ui.orders.model.*
import com.example.pikaroo.ui.orders.viewmodel.ReceiptViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

internal fun receiptOrder(id: String = "order_1001", date: String = "2026-10-20T18:00:00Z") = OrderDto(
    id, "shop_1", "user_1", Fulfilment.PICKUP, OrderStatus.PLACED,
    listOf(OrderLineDto("1", "Pan", 2, 3.5, 0, 7.0)), 7.0, 0.0, 7.0,
    PickupDto(date, "ABC123"), null, date, paymentStatus = "PENDING"
)

private class ReceiptRepository : OrdersRepository {
    var saved = listOf(receiptOrder())
    var fail = false
    var created = 0
    override suspend fun create(request: CreateOrderRequest): OrderDto { created++; error("Unexpected write") }
    override suspend fun listMine(): List<OrderDto> = saved
    override suspend fun get(id: String): OrderDto {
        if (fail) throw MockErrors.notFound()
        return saved.first { it.id == id }
    }
    override suspend fun events(id: String): List<OrderEventDto> = emptyList()
    override suspend fun driverLocation(id: String): DriverLocationDto? = null
}

class ReceiptViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun `reopening a receipt reads the saved order without creating another`() = runTest {
        val repository = ReceiptRepository()
        repeat(2) {
            val model = ReceiptViewModel("order_1001", repository)
            assertEquals("order_1001", model.state.value.order!!.id)
            assertFalse(model.state.value.isLoading)
        }
        assertEquals(0, repository.created)
    }

    @Test fun `latest uses creation time rather than repository list order`() = runTest {
        val repository = ReceiptRepository()
        repository.saved = listOf(receiptOrder("order_1"), receiptOrder("order_2", "2026-10-20T19:00:00Z"))
        assertEquals("order_2", ReceiptViewModel(null, repository).state.value.order!!.id)
    }

    @Test fun `missing order can be retried and an empty history has a useful message`() = runTest {
        val repository = ReceiptRepository()
        repository.fail = true
        val model = ReceiptViewModel("order_1001", repository)
        assertEquals("No se encontró el pedido", model.state.value.error)
        repository.fail = false
        model.load()
        assertNotNull(model.state.value.order)
        repository.saved = emptyList()
        assertEquals("Todavía no tienes pedidos", ReceiptViewModel(null, repository).state.value.error)
    }
}
