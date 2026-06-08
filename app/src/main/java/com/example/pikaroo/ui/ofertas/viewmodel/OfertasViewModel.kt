package com.example.pikaroo.ui.ofertas.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.pikaroo.ui.ofertas.model.OfertasState

class OfertasViewModel : ViewModel() {
    var state by mutableStateOf(OfertasState())
        private set
}
