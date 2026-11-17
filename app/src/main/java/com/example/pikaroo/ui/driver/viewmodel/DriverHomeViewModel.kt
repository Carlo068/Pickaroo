package com.example.pikaroo.ui.driver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pikaroo.appContainer
import com.example.pikaroo.common.mock.toApiError
import com.example.pikaroo.ui.auth.data.SessionExpiredException
import com.example.pikaroo.ui.driver.data.DriverRepository
import com.example.pikaroo.ui.driver.model.AvailableOrderDto
import com.example.pikaroo.ui.orders.model.OrderDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import com.example.pikaroo.R
import com.example.pikaroo.common.text.UiText

data class DriverHomeState(
    val isOnline: Boolean = true,
    val current: OrderDto? = null,
    val available: List<AvailableOrderDto> = emptyList(),
    val isLoading: Boolean = true,
    val acceptingId: String? = null,
    val acting: Boolean = false,
    val message: UiText? = null,
    val error: UiText? = null
)

class DriverHomeViewModel(
    private val repository: DriverRepository,
    private val pollMillis: Long = 5_000L
) : ViewModel() {

    private val _state = MutableStateFlow(DriverHomeState())
    val state: StateFlow<DriverHomeState> = _state.asStateFlow()

    /** Actualiza entrega y pedidos disponibles mientras la pantalla está visible. */
    suspend fun poll() {
        while (refresh()) delay(pollMillis)
    }

    fun setOnline(online: Boolean) {
        _state.update { it.copy(isOnline = online, available = if (online) it.available else emptyList()) }
        if (online) viewModelScope.launch { refresh() }
    }

    fun accept(orderId: String) {
        if (_state.value.acceptingId != null) return
        _state.update { it.copy(acceptingId = orderId, message = null) }
        viewModelScope.launch {
            val message = try {
                repository.accept(orderId)
                null
            } catch (e: CancellationException) {
                throw e
            } catch (_: SessionExpiredException) {
                null
            } catch (e: HttpException) {
                messageFor(e)
            } catch (_: Exception) {
                UiText.Res(R.string.error_offline_retry)
            }
            _state.update { it.copy(acceptingId = null, message = message) }
            refresh()
        }
    }

    fun pickUp() = advance(successMessage = null) { repository.pickup(it) }

    fun deliver() = advance(successMessage = UiText.Res(R.string.driver_delivery_completed)) { repository.deliver(it) }

    private fun advance(successMessage: UiText?, action: suspend (String) -> Unit) {
        val order = _state.value.current ?: return
        if (_state.value.acting) return
        _state.update { it.copy(acting = true, message = null) }
        viewModelScope.launch {
            val message = try {
                action(order.id)
                successMessage
            } catch (e: CancellationException) {
                throw e
            } catch (_: SessionExpiredException) {
                null
            } catch (e: HttpException) {
                messageFor(e)
            } catch (_: Exception) {
                UiText.Res(R.string.error_offline_retry)
            }
            _state.update { it.copy(acting = false, message = message) }
            refresh()
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    /** True si hay que seguir consultando. */
    private suspend fun refresh(): Boolean = try {
        val current = repository.current()
        val available = if (current == null && _state.value.isOnline) repository.available() else emptyList()
        _state.update { it.copy(current = current, available = available, isLoading = false, error = null) }
        true
    } catch (e: CancellationException) {
        throw e
    } catch (_: SessionExpiredException) {
        false
    } catch (e: HttpException) {
        val forbidden = e.code() == 403
        fail(UiText.Res(if (forbidden) R.string.driver_error_forbidden_list else R.string.driver_error_load))
        !forbidden
    } catch (_: Exception) {
        fail(UiText.Res(R.string.error_offline_retrying))
        true
    }

    private fun fail(message: UiText) {
        _state.update { it.copy(isLoading = false, error = message) }
    }

    private fun messageFor(e: HttpException): UiText = when (e.toApiError()?.code) {
        "ORDER_NOT_AVAILABLE" -> UiText.Res(R.string.driver_error_taken)
        "ALREADY_DELIVERING" -> UiText.Res(R.string.driver_error_already_delivering)
        "ORDER_NOT_FOUND" -> UiText.Res(R.string.driver_error_order_gone)
        "INVALID_STATUS" -> UiText.Res(R.string.driver_error_invalid_status)
        "NOT_YOUR_DELIVERY" -> UiText.Res(R.string.driver_error_not_yours)
        else -> UiText.Res(if (e.code() == 403) R.string.driver_error_forbidden_accept else R.string.driver_error_action)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { DriverHomeViewModel(appContainer().driverRepository) }
        }
    }
}
