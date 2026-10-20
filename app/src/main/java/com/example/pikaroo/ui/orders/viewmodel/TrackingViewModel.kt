package com.example.pikaroo.ui.orders.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pikaroo.appContainer
import com.example.pikaroo.ui.auth.data.SessionExpiredException
import com.example.pikaroo.ui.orders.data.OrdersRepository
import com.example.pikaroo.ui.orders.model.DriverLocationDto
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.model.OrderEventDto
import com.example.pikaroo.ui.orders.model.OrderStatus
import com.example.pikaroo.ui.orders.model.TimelineStep
import com.example.pikaroo.ui.orders.model.buildTimeline
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException

data class TrackingUiState(
    val order: OrderDto? = null,
    val events: List<OrderEventDto> = emptyList(),
    val timeline: List<TimelineStep> = emptyList(),
    val driverLocation: DriverLocationDto? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class PollIntervals(
    val defaultMillis: Long = 5_000L,
    val outForDeliveryMillis: Long = 2_000L
) {
    fun forStatus(status: OrderStatus?) =
        if (status == OrderStatus.OUT_FOR_DELIVERY) outForDeliveryMillis else defaultMillis
}

class TrackingViewModel(
    private val orderId: String,
    private val repository: OrdersRepository,
    private val intervals: PollIntervals = PollIntervals()
) : ViewModel() {

    private val _state = MutableStateFlow(TrackingUiState())
    val state: StateFlow<TrackingUiState> = _state.asStateFlow()

    /** Actualiza el pedido hasta que llega a un estado final; se cancela al salir de la pantalla. */
    suspend fun poll() {
        while (refresh()) {
            delay(intervals.forStatus(_state.value.order?.status))
        }
    }

    /** True si hay que seguir consultando. */
    private suspend fun refresh(): Boolean = try {
        val order = repository.get(orderId)
        val events = repository.events(orderId)
        _state.value = TrackingUiState(
            order = order,
            events = events,
            timeline = buildTimeline(order, events),
            driverLocation = fetchDriverLocation(order),
            isLoading = false
        )
        !order.status.isFinal
    } catch (e: CancellationException) {
        throw e
    } catch (_: SessionExpiredException) {
        false
    } catch (e: HttpException) {
        fail(
            when (e.code()) {
                403 -> "No tienes acceso a este pedido"
                404 -> "No se encontró el pedido"
                else -> "No se pudo actualizar el pedido"
            }
        )
        e.code() !in listOf(403, 404)
    } catch (_: Exception) {
        fail("Sin conexión. Reintentando…")
        true
    }

    /** La ubicación es un extra: si falla se conserva la anterior y el pedido sigue actualizándose. */
    private suspend fun fetchDriverLocation(order: OrderDto): DriverLocationDto? {
        if (!order.tracksDriver) return null
        return try {
            repository.driverLocation(order.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SessionExpiredException) {
            throw e
        } catch (_: Exception) {
            _state.value.driverLocation
        }
    }

    private fun fail(message: String) {
        _state.value = _state.value.copy(isLoading = false, error = message)
    }

    companion object {
        fun factory(orderId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer { TrackingViewModel(orderId, appContainer().ordersRepository) }
        }
    }
}
