package com.example.pikaroo.ui.user.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.pikaroo.ui.user.model.UserState

class UserViewModel : ViewModel() {
    var state by mutableStateOf(UserState())
        private set
}
