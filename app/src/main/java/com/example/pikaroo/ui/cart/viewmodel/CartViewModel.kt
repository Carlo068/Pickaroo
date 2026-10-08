package com.example.pikaroo.ui.cart.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pikaroo.ui.cart.data.CartPreferences
import com.example.pikaroo.ui.cart.data.CartStorage
import com.example.pikaroo.ui.cart.model.CartItem
import com.example.pikaroo.ui.cart.model.CartUiState
import com.example.pikaroo.ui.products.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Dueño del carrito. Recibe un [CartStorage] para poder probarse sin Android
 * (en las pruebas se usa uno en memoria); en la app se usa [CartPreferences].
 */
class CartViewModel(private val storage: CartStorage) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState(storage.load()))
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    fun add(product: Product) {
        val items = _uiState.value.items
        val existing = items.find { it.productId == product.id }
        val updated = if (existing == null) {
            items + CartItem.from(product)
        } else {
            items.map { if (it.productId == product.id) it.copy(quantity = it.quantity + 1) else it }
        }
        commit(updated)
    }

    fun increment(productId: String) {
        commit(_uiState.value.items.map {
            if (it.productId == productId) it.copy(quantity = it.quantity + 1) else it
        })
    }

    fun decrement(productId: String) {
        commit(_uiState.value.items
            .map { if (it.productId == productId) it.copy(quantity = it.quantity - 1) else it }
            .filter { it.quantity > 0 })
    }

    fun remove(productId: String) {
        commit(_uiState.value.items.filterNot { it.productId == productId })
    }

    fun clear() {
        commit(emptyList())
    }

    private fun commit(items: List<CartItem>) {
        _uiState.value = CartUiState(items)
        storage.save(items)
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer { CartViewModel(CartPreferences(context.applicationContext)) }
        }
    }
}
