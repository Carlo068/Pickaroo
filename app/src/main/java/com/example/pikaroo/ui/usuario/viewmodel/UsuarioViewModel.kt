package com.example.pikaroo.ui.usuario.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.pikaroo.ui.usuario.model.UsuarioState

class UsuarioViewModel : ViewModel() {
    var state by mutableStateOf(UsuarioState())
        private set
}
