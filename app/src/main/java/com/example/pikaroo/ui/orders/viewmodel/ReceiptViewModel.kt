package com.example.pikaroo.ui.orders.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pikaroo.appContainer
import com.example.pikaroo.ui.orders.data.OrdersRepository
import com.example.pikaroo.ui.orders.model.OrderDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class ReceiptUiState(val order: OrderDto? = null, val isLoading: Boolean = true, val error: String? = null)

class ReceiptViewModel(private val orderId: String?, private val repository: OrdersRepository) : ViewModel() {
    private val _state = MutableStateFlow(ReceiptUiState())
    val state = _state.asStateFlow()

    private var jobActive = false

    init { load() }

    fun load() {
        if (_state.value.isLoading && jobActive) return
        jobActive = true
        _state.value = ReceiptUiState()
        viewModelScope.launch {
            try {
                val order = if (orderId != null) repository.get(orderId)
                    else repository.listMine().maxByOrNull { it.createdAt }
                _state.value = ReceiptUiState(order, false, if (order == null) "Todavía no tienes pedidos" else null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                _state.value = ReceiptUiState(isLoading = false, error = when (e.code()) {
                    403 -> "No tienes acceso a este pedido"
                    404 -> "No se encontró el pedido"
                    else -> "No se pudo cargar el comprobante"
                })
            } catch (_: Exception) {
                _state.value = ReceiptUiState(isLoading = false, error = "No se pudo cargar el comprobante. Intenta de nuevo.")
            } finally { jobActive = false }
        }
    }

    companion object {
        fun factory(orderId: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer { ReceiptViewModel(orderId, appContainer().ordersRepository) }
        }
    }
}
