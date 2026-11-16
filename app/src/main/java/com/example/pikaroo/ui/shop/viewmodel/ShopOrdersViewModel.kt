package com.example.pikaroo.ui.shop.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pikaroo.R
import com.example.pikaroo.appContainer
import com.example.pikaroo.common.text.UiText
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
    val error: UiText? = null
)

class ShopOrdersViewModel(private val repository: ShopOrdersRepository, private val pollMillis: Long = 5_000) : ViewModel() {
    private val mutableState = MutableStateFlow(ShopOrdersState())
    val state = mutableState.asStateFlow()
    private val mutex = Mutex()

    suspend fun poll() {
        while (refresh()) delay(pollMillis)
    }

    fun retry() { viewModelScope.launch { refresh() } }

    fun advance(order: OrderDto) {
        if (state.value.updatingId != null) return
        val next = nextShopStatus(order.status, order.fulfilment) ?: return
        mutableState.update { it.copy(updatingId = order.id, error = null) }
        viewModelScope.launch {
            mutex.withLock {
                try {
                    val updated = repository.update(order.id, ShopStatusRequest(order.status, next))
                    mutableState.update { state -> state.copy(orders = state.orders.map { if (it.id == updated.id) updated else it }) }
                } catch (e: CancellationException) { throw e
                } catch (_: SessionExpiredException) {
                } catch (e: Exception) {
                    mutableState.update { it.copy(error = UiText.Res(if (e is HttpException && e.code() == 409)
                        R.string.shop_error_changed else R.string.shop_error_update)) }
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
            mutableState.update { it.copy(loading = false, error = UiText.Res(if (e is HttpException && e.code() == 403)
                R.string.shop_error_forbidden else R.string.shop_error_load)) }
            !(e is HttpException && e.code() == 403)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ShopOrdersViewModel(appContainer().shopOrdersRepository) }
        }
    }
}
