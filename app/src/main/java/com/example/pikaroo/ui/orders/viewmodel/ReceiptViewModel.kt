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
import com.example.pikaroo.R
import com.example.pikaroo.common.text.UiText

data class ReceiptUiState(val order: OrderDto? = null, val isLoading: Boolean = true, val error: UiText? = null)

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
                _state.value = ReceiptUiState(order, false, if (order == null) UiText.Res(R.string.orders_empty) else null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                _state.value = ReceiptUiState(isLoading = false, error = when (e.code()) {
                    403 -> UiText.Res(R.string.order_error_forbidden)
                    404 -> UiText.Res(R.string.order_error_not_found)
                    else -> UiText.Res(R.string.receipt_error_load)
                })
            } catch (_: Exception) {
                _state.value = ReceiptUiState(isLoading = false, error = UiText.Res(R.string.receipt_error_load_retry))
            } finally { jobActive = false }
        }
    }

    companion object {
        fun factory(orderId: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer { ReceiptViewModel(orderId, appContainer().ordersRepository) }
        }
    }
}
