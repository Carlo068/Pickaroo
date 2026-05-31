package com.example.pikaroo.ui.inicio.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.pikaroo.ui.inicio.model.InicioState

class InicioViewModel : ViewModel() {
    var state by mutableStateOf(InicioState())
        private set
}
