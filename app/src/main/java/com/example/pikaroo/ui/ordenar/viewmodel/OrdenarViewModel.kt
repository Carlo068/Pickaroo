package com.example.pikaroo.ui.ordenar.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.pikaroo.ui.ordenar.model.OrdenarState

class OrdenarViewModel : ViewModel() {
    var state by mutableStateOf(OrdenarState())
        private set
}
