package com.example.pikaroo.ui.order.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.pikaroo.ui.order.model.OrderState

class OrderViewModel : ViewModel() {
    var state by mutableStateOf(OrderState())
        private set
}
