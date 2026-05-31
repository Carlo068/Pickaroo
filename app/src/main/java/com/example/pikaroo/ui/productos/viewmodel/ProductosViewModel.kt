package com.example.pikaroo.ui.productos.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.pikaroo.ui.productos.model.ProductosState

class ProductosViewModel : ViewModel() {
    var state by mutableStateOf(ProductosState())
        private set
}
