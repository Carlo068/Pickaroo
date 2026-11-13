package com.example.pikaroo.ui.user.model

import com.example.pikaroo.ui.auth.model.MeResponse

data class UserState(
    val title: String = "Usuario",
    val isLoading: Boolean = false,
    val profile: MeResponse? = null,
    val error: String? = null
)
