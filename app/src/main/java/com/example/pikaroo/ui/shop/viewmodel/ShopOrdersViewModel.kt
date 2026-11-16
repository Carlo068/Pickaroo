package com.example.pikaroo.ui.shop.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pikaroo.common.mock.toApiError
import com.example.pikaroo.appContainer
import com.example.pikaroo.ui.auth.data.SessionExpiredException
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.shop.data.ShopOrdersRepository
import com.example.pikaroo.ui.shop.model.ShopStatusRequest
import com.example.pikaroo.ui.shop.model.nextShopStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

data class ShopOrdersState(
    val orders: List<OrderDto> = emptyList(),
    val loading: Boolean = true,
    val updatingId: String? = null,
    val error: String? = null,
    val pickupError: String? = null
)

class ShopOrdersViewModel(private val repository: ShopOrdersRepository, private val pollMillis: Long = 5_000) : ViewModel() {
    private val mutableState = MutableStateFlow(ShopOrdersState())
    val state = mutableState.asStateFlow()
    private val mutex = Mutex()

    suspend fun poll() {
        while (refresh()) delay(pollMillis)
    }

    fun retry() { viewModelScope.launch { refresh() } }

    fun advance(order: OrderDto, pickupCode: String? = null) {
        if (state.value.updatingId != null) return
        val next = nextShopStatus(order.status, order.fulfilment) ?: return
        mutableState.update { it.copy(updatingId = order.id, error = null, pickupError = null) }
        viewModelScope.launch {
            mutex.withLock {
                try {
                    val updated = repository.update(order.id, ShopStatusRequest(order.status, next, pickupCode))
                    mutableState.update { state -> state.copy(orders = state.orders.map { if (it.id == updated.id) updated else it }) }
                } catch (e: CancellationException) { throw e
                } catch (_: SessionExpiredException) {
                } catch (e: Exception) {
                    if (e is HttpException && e.toApiError()?.code == "INVALID_PICKUP_CODE") {
                        mutableState.update { it.copy(pickupError = "Código incorrecto. Revisa el código del cliente.") }
                    } else {
                        mutableState.update { it.copy(error = if (e is HttpException && e.code() == 409)
                            "El pedido cambió. Actualiza la lista antes de continuar." else "No se pudo actualizar el pedido. Intenta de nuevo.") }
                    }
                } finally {
                    mutableState.update { it.copy(updatingId = null) }
                }
            }
        }
    }

    private suspend fun refresh(): Boolean = mutex.withLock {
        try {
            val orders = repository.orders()
            mutableState.update { it.copy(orders = orders, loading = false, error = null) }
            true
        } catch (e: CancellationException) { throw e
        } catch (_: SessionExpiredException) { false
        } catch (e: Exception) {
            mutableState.update { it.copy(loading = false, error = if (e is HttpException && e.code() == 403)
                "No tienes permiso para ver los pedidos de esta tienda." else "No se pudieron cargar los pedidos. Reintenta.") }
            !(e is HttpException && e.code() == 403)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ShopOrdersViewModel(appContainer().shopOrdersRepository) }
        }
    }
}
