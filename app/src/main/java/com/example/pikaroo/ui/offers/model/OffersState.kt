package com.example.pikaroo.ui.offers.model

import com.example.pikaroo.R
import com.example.pikaroo.common.text.UiText

data class OffersState(
    val title: UiText = UiText.Res(R.string.offers_title),
    val isLoading: Boolean = false
)
