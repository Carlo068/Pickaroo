package com.example.pikaroo.ui.orders.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pikaroo.appContainer
import com.example.pikaroo.ui.orders.data.OrdersRepository
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.qr.parseOrderQr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class OrderLookupUiState(
    val input: String = "", val isLoading: Boolean = false,
    val error: String? = null, val notice: String? = null
)

class OrderLookupViewModel(private val repository: OrdersRepository) : ViewModel() {
    private val _state = MutableStateFlow(OrderLookupUiState())
    val state = _state.asStateFlow()

    fun updateInput(input: String) {
        if (!_state.value.isLoading) _state.value = OrderLookupUiState(input = input)
    }

    fun scanCancelled() {
        _state.value = _state.value.copy(notice = "No se leyó el QR. Puedes escribir o pegar el ID del pedido.")
    }

    fun scanned(raw: String, onFound: (OrderDto) -> Unit) {
        updateInput(raw)
        lookup(onFound)
    }

    fun lookup(onFound: (OrderDto) -> Unit) {
        if (_state.value.isLoading) return
        val id = parseOrderQr(_state.value.input)
        if (id == null) {
            _state.value = _state.value.copy(error = "Orden no encontrada. Revisa el ID o el QR.", notice = null)
            return
        }
        _state.value = OrderLookupUiState(input = id, isLoading = true)
        viewModelScope.launch {
            val order = try {
                repository.get(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                _state.value = _state.value.copy(isLoading = false, error = if (e.code() in listOf(403, 404))
                    "Orden no encontrada" else "No se pudo consultar el pedido. Intenta de nuevo.")
                return@launch
            } catch (_: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "No se pudo consultar el pedido. Intenta de nuevo.")
                return@launch
            }
            _state.value = _state.value.copy(isLoading = false)
            onFound(order)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { OrderLookupViewModel(appContainer().ordersRepository) }
        }
    }
}
