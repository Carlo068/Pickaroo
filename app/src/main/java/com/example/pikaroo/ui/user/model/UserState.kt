package com.example.pikaroo.ui.user.model

import com.example.pikaroo.R
import com.example.pikaroo.common.text.UiText
import com.example.pikaroo.ui.auth.model.MeResponse

data class UserState(
    val title: UiText = UiText.Res(R.string.user_title),
    val isLoading: Boolean = false,
    val profile: MeResponse? = null,
    val error: UiText? = null
)
