package com.example.pikaroo.ui.home.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.pikaroo.ui.home.model.HomeState

class HomeViewModel : ViewModel() {
    var state by mutableStateOf(HomeState())
        private set
}
