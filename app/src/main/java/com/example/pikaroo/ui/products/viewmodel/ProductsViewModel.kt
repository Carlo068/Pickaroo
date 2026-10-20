package com.example.pikaroo.ui.products.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pikaroo.appContainer
import com.example.pikaroo.ui.products.data.ProductsRepository
import com.example.pikaroo.ui.products.model.ProductsState
import kotlinx.coroutines.launch

class ProductsViewModel(private val repository: ProductsRepository) : ViewModel() {
    var state by mutableStateOf(ProductsState())
        private set

    init {
        Log.d("ProductsViewModel", "Iniciando fetchProducts con Retrofit...")
        fetchProducts()
    }

    private fun fetchProducts() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            try {
                val products = repository.getProducts()
                
                Log.d("ProductsViewModel", "Productos recibidos: ${products.size}")
                
                val categories = listOf("Todos") + products.map { it.category }.distinct().sorted()
                
                state = state.copy(
                    products = products,
                    filteredProducts = products,
                    categories = categories,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                Log.e("ProductsViewModel", "Error cargando productos", e)
                state = state.copy(
                    isLoading = false,
                    error = e.message ?: "Error desconocido"
                )
            }
        }
    }

    fun selectCategory(category: String) {
        val filtered = if (category == "Todos") {
            state.products
        } else {
            state.products.filter { it.category == category }
        }
        state = state.copy(
            selectedCategory = category,
            filteredProducts = filtered
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ProductsViewModel(appContainer().productsRepository) }
        }
    }
}
