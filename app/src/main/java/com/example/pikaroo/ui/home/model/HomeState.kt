package com.example.pikaroo.ui.home.model

import com.example.pikaroo.R
import com.example.pikaroo.common.text.UiText

data class HomeState(
    val title: UiText = UiText.Res(R.string.home_title),
    val isLoading: Boolean = false
)
