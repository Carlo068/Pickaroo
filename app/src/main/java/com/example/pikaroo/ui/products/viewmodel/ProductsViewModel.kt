package com.example.pikaroo.ui.products.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pikaroo.ui.products.model.ProductsState
import com.example.pikaroo.ui.products.network.RetrofitClient
import kotlinx.coroutines.launch

class ProductsViewModel : ViewModel() {
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
                // Usando RetrofitClient para obtener los productos
                val response = RetrofitClient.gistService.getProducts()
                val products = response.products
                
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
}
