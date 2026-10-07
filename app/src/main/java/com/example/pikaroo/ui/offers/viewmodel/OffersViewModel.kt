package com.example.pikaroo.ui.offers.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.pikaroo.ui.offers.model.OffersState

class OffersViewModel : ViewModel() {
    var state by mutableStateOf(OffersState())
        private set
}
