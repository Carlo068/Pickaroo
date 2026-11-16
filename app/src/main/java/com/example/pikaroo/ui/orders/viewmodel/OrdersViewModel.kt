package com.example.pikaroo.ui.orders.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pikaroo.appContainer
import com.example.pikaroo.ui.auth.data.SessionExpiredException
import com.example.pikaroo.ui.orders.data.OrdersRepository
import com.example.pikaroo.ui.orders.model.OrderDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import com.example.pikaroo.R
import com.example.pikaroo.common.text.UiText

data class OrdersUiState(
    val orders: List<OrderDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: UiText? = null
)

class OrdersViewModel(private val repository: OrdersRepository) : ViewModel() {

    private val _state = MutableStateFlow(OrdersUiState())
    val state: StateFlow<OrdersUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                _state.value = OrdersUiState(orders = repository.listMine(), isLoading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (_: SessionExpiredException) {
                _state.value = _state.value.copy(isLoading = false)
            } catch (e: HttpException) {
                val message = UiText.Res(if (e.code() == 403) R.string.orders_error_forbidden else R.string.orders_error_load)
                _state.value = _state.value.copy(isLoading = false, error = message)
            } catch (_: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = UiText.Res(R.string.error_offline_retry))
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { OrdersViewModel(appContainer().ordersRepository) }
        }
    }
}
