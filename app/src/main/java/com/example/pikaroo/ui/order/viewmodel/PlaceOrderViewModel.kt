package com.example.pikaroo.ui.order.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pikaroo.appContainer
import com.example.pikaroo.ui.cart.model.CartUiState
import com.example.pikaroo.ui.order.model.DeliveryMethod
import com.example.pikaroo.ui.order.model.OrderState
import com.example.pikaroo.ui.orders.data.OrdersRepository
import com.example.pikaroo.ui.orders.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class PlaceOrderUiState(val isSaving: Boolean = false, val error: String? = null)

class PlaceOrderViewModel(private val orders: OrdersRepository) : ViewModel() {
    private val _state = MutableStateFlow(PlaceOrderUiState())
    val state = _state.asStateFlow()
    private var pending: CreateOrderRequest? = null

    fun place(cart: CartUiState, delivery: OrderState, onSaved: (OrderDto) -> Unit) {
        if (_state.value.isSaving || !delivery.canReview(cart)) return
        val draft = CreateOrderRequest(
            requestId = "", shopId = "shop_1", // Sucursal de ejemplo hasta PIC-28.
            fulfilment = if (delivery.method == DeliveryMethod.PICKUP) Fulfilment.PICKUP else Fulfilment.DELIVERY,
            items = cart.items.map { OrderLineDto(it.productId, it.name, it.quantity, it.price, 0, it.lineTotal) },
            address = if (delivery.method == DeliveryMethod.DELIVERY) delivery.address.trim() else null
        )
        val request = pending?.takeIf { it.copy(requestId = "") == draft }
            ?: draft.copy(requestId = UUID.randomUUID().toString())
        pending = request
        _state.value = PlaceOrderUiState(isSaving = true)
        viewModelScope.launch {
            val saved = try {
                orders.create(request)
            } catch (e: CancellationException) {
                _state.value = PlaceOrderUiState()
                throw e
            } catch (_: Exception) {
                _state.value = PlaceOrderUiState(error = "No se pudo guardar el pedido. Tu carrito sigue disponible. Intenta de nuevo.")
                return@launch
            }
            // El callback de éxito queda fuera del catch: no confundir navegación con fallo de guardado.
            onSaved(saved)
            pending = null
            _state.value = PlaceOrderUiState()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { PlaceOrderViewModel(appContainer().ordersRepository) }
        }
    }
}
